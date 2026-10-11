"""Prepare and run an isolated Forge 1.8.9 preview/audit on macOS.

Uses public, checksum-verified Minecraft/Forge downloads and an offline identity.
Does not read account files or edit the installed launcher's profiles.
"""
import argparse
import concurrent.futures
import hashlib
import json
import os
import platform
import re
import shutil
import subprocess
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "work/forge-mac-0.5.0"
INSTANCE = ROOT / "instances/forge-1.8.9-0.5.0-mac"
EXISTING = Path.home() / "Library/Application Support/minecraft"
FORGE = "1.8.9-11.15.1.2318-1.8.9"
VERSION = "0.5.0"
MC = "1.8.9"
TARGET = "forge-1.8.9"
JAR_NAME = "badlion-cosmetic-port-forge-0.5.0.jar"
REPORT_NAME = "forge-1.8.9-0.5.0-mac-validation.json"


def urlopen(url, timeout=45):
    request = urllib.request.Request(url, headers={"User-Agent": "BadlionCosmeticPort/0.5.0"})
    return urllib.request.urlopen(request, timeout=timeout)


def digest(path, algorithm="sha1"):
    h = hashlib.new(algorithm)
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def fetch(url, target, expected=None, algorithm="sha1", reuse=None):
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.is_file() and (not expected or digest(target, algorithm) == expected):
        return target
    if reuse and reuse.is_file() and expected and digest(reuse, algorithm) == expected:
        shutil.copy2(reuse, target)
        return target
    temporary = target.with_name(target.name + ".download")
    try:
        with urlopen(url) as incoming, temporary.open("wb") as out:
            shutil.copyfileobj(incoming, out)
        if expected and digest(temporary, algorithm) != expected:
            raise RuntimeError("Checksum mismatch: " + str(target))
        temporary.replace(target)
    finally:
        temporary.unlink(missing_ok=True)
    return target


def allowed(lib):
    result = not bool(lib.get("rules"))
    for rule in lib.get("rules", []):
        system = rule.get("os", {})
        if system.get("name", "osx") != "osx":
            continue
        if "arch" in system and not re.search(system["arch"], "x86_64"):
            continue
        if "version" in system and not re.search(system["version"], platform.mac_ver()[0]):
            continue
        result = rule["action"] == "allow"
    return result


def coordinate(name):
    group, artifact, version, *classifier = name.split(":")
    suffix = "-" + classifier[0] if classifier else ""
    return f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}{suffix}.jar"


def library(lib):
    artifact = lib.get("downloads", {}).get("artifact")
    if lib.get("downloads") and not artifact:
        return None
    relative = artifact["path"] if artifact else coordinate(lib["name"])
    url = artifact["url"] if artifact else lib.get("url", "https://libraries.minecraft.net/") + relative
    expected = artifact.get("sha1") if artifact else None
    if not expected:
        values = lib.get("checksums", [])
        if values:
            expected = values[0]
        else:
            with urlopen(url + ".sha1", timeout=30) as r:
                expected = r.read().decode().strip().split()[0]
    path = CACHE / "libraries" / relative
    try:
        return fetch(url, path, expected, reuse=EXISTING / "libraries" / relative)
    except urllib.error.HTTPError:
        return fetch("https://repo.maven.apache.org/maven2/" + relative, path, expected)


def java_home(explicit=None):
    if explicit:
        home = Path(explicit).expanduser().resolve()
    elif os.environ.get("JAVA_HOME"):
        home = Path(os.environ["JAVA_HOME"])
    else:
        candidates = sorted((ROOT / "work/toolchains").glob("*jdk8*macosx_x64/Contents/Home"))
        if not candidates:
            raise RuntimeError("Provide --java-home pointing to an x86_64 Java 8 JDK")
        home = candidates[-1]
    java = home / "bin/java"
    result = subprocess.run([str(java), "-XshowSettings:properties", "-version"], capture_output=True, text=True, check=True)
    if "java.version = 1.8." not in result.stderr or not re.search(r"os.arch = (x86_64|amd64)", result.stderr):
        raise RuntimeError("This stock LWJGL 2 runtime requires x86_64 Java 8 (Rosetta on Apple Silicon)")
    return java


def prepare(explicit_java, native_mode):
    java = java_home(explicit_java)
    CACHE.mkdir(parents=True, exist_ok=True)
    INSTANCE.mkdir(parents=True, exist_ok=True)
    with urlopen("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", timeout=30) as r:
        versions = json.load(r)
    record = next(v for v in versions["versions"] if v["id"] == MC)
    base_path = fetch(record["url"], CACHE / (MC + ".json"), record["sha1"])
    base = json.loads(base_path.read_text())
    installer_url = f"https://maven.minecraftforge.net/net/minecraftforge/forge/{FORGE}/forge-{FORGE}-installer.jar"
    # Forge publishes the installer's MD5; verify the embedded universal JAR with SHA1 too.
    with urlopen(installer_url + ".md5", timeout=30) as r:
        installer_md5 = r.read().decode().strip().split()[0]
    installer_path = fetch(installer_url, CACHE / "forge-installer.jar", installer_md5, "md5",
                           ROOT / "work/forge-runtime/forge-installer.jar")
    with zipfile.ZipFile(installer_path) as archive:
        profile = json.loads(archive.read("install_profile.json"))
        if MC == "1.8.9":
            forge = profile["versionInfo"]
            relative = coordinate(profile["install"]["path"])
            data = archive.read(profile["install"]["filePath"])
            expected = "beda619c465af293e63952dd573c137c17c0a4cd"
        else:
            forge = json.loads(archive.read("version.json"))
            record = next(lib for lib in forge["libraries"] if lib["name"].startswith("net.minecraftforge:forge:"))
            relative = record["downloads"]["artifact"]["path"]
            expected = record["downloads"]["artifact"]["sha1"]
            data = archive.read("maven/" + relative)
            record["downloads"]["artifact"]["url"] = "https://maven.minecraftforge.net/" + relative
        if hashlib.sha1(data).hexdigest() != expected:
            raise RuntimeError("Unexpected Forge universal JAR")
        forge_path = CACHE / "libraries" / relative
        forge_path.parent.mkdir(parents=True, exist_ok=True)
        forge_path.write_bytes(data)
    libraries = {}
    for lib in base["libraries"] + forge["libraries"]:
        if allowed(lib) and lib.get("clientreq") is not False:
            libraries[":".join(lib["name"].split(":")[:2])] = lib
    # The retired Forge Scala XML repack is unavailable; use the upstream module.
    xml = libraries.get("org.scala-lang:scala-xml_2.11")
    if xml:
        xml["name"] = "org.scala-lang.modules:scala-xml_2.11:1.0.2"
        xml["url"] = "https://repo.maven.apache.org/maven2/"
        xml.pop("checksums", None)
    # Legacy Forge directly references beta9 internals. Keep the manifest's jars
    # and use Mojang's checksum-verified logging configuration/filter.
    logging = base["logging"]["client"]
    logging_file = logging["file"]
    logging_path = fetch(logging_file["url"], CACHE / logging_file["id"], logging_file["sha1"])
    natives = CACHE / "natives"
    natives.mkdir(exist_ok=True)
    classpath = []
    for lib in libraries.values():
        print("Resolving " + lib["name"], flush=True)
        if lib["name"].startswith("net.minecraftforge:forge:"):
            path = forge_path
        else:
            path = library(lib)
        if path:
            classpath.append(str(path))
        classifier = lib.get("natives", {}).get("osx")
        if classifier:
            native = lib["downloads"]["classifiers"][classifier.replace("${arch}", "64")]
            native_jar = fetch(native["url"], CACHE / "libraries" / native["path"], native["sha1"],
                               reuse=EXISTING / "libraries" / native["path"])
            with zipfile.ZipFile(native_jar) as archive:
                for name in archive.namelist():
                    if name.endswith((".dylib", ".jnilib")):
                        (natives / Path(name).name).write_bytes(archive.read(name))
    if native_mode == "badlion":
        relative = "org/lwjgl/lwjgl/lwjgl-platform/2.9.2-nightly-20250117/lwjgl-platform-2.9.2-nightly-20250117-natives-osx.jar"
        native_jar = fetch("https://assets.badlion.net/libs/" + relative,
                           CACHE / "libraries" / relative, "d94ed8060c920cd35d9be520dda30e39e70e8652",
                           reuse=EXISTING / "libraries" / relative)
        with zipfile.ZipFile(native_jar) as archive:
            for name in ("liblwjgl.dylib", "openal.dylib"):
                (natives / name).write_bytes(archive.read(name))
    client = base["downloads"]["client"]
    classpath.append(str(fetch(client["url"], CACHE / (MC + ".jar"), client["sha1"],
                              reuse=EXISTING / "versions" / MC / (MC + ".jar"))))
    assets = CACHE / "assets"
    index = base["assetIndex"]
    index_path = fetch(index["url"], assets / "indexes" / (index["id"] + ".json"), index["sha1"],
                       reuse=EXISTING / "assets/indexes" / (index["id"] + ".json"))
    objects = json.loads(index_path.read_text())["objects"]
    hashes = sorted(set(value["hash"] for value in objects.values()))
    def asset(digest):
        relative = "objects/" + digest[:2] + "/" + digest
        fetch("https://resources.download.minecraft.net/" + digest[:2] + "/" + digest,
              assets / relative, digest, reuse=EXISTING / "assets" / relative)
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        list(pool.map(asset, hashes))
    subprocess.run([shutil.which("python3"), str(ROOT / "tools/install-portable.py"),
                    "--target", TARGET, "--game-dir", str(INSTANCE)], check=True)
    options = INSTANCE / "options.txt"
    if not options.exists():
        options.write_text("guiScale:2\nrenderDistance:6\nmaxFps:60\npauseOnLostFocus:false\nthirdPersonView:1\n")
    runtime = {"java": str(java), "classpath": classpath, "natives": str(natives),
               "assets": str(assets), "assetIndex": index["id"], "forgeId": forge["id"],
               "loggingArgument": logging["argument"].replace("${path}", logging_path.as_uri())}
    runtime["nativeVariant"] = native_mode
    (CACHE / "runtime.json").write_text(json.dumps(runtime, indent=2))
    print("Prepared isolated Mac instance: " + str(INSTANCE))


def launch(audit, explicit_java, timeout):
    runtime = json.loads((CACHE / "runtime.json").read_text())
    java = java_home(explicit_java) if explicit_java else Path(runtime["java"])
    args = [str(java), "-Xmx2G", runtime["loggingArgument"],
            "-Djava.library.path=" + runtime["natives"], "-cp", os.pathsep.join(runtime["classpath"]),
            "net.minecraft.launchwrapper.Launch", "--username", "PortAudit",
            "--uuid", "00000000000040008000000000000001", "--accessToken", "0", "--userType", "legacy",
            "--version", runtime["forgeId"], "--gameDir", str(INSTANCE), "--assetsDir", runtime["assets"],
            "--assetIndex", runtime["assetIndex"], "--userProperties", "{}",
            "--tweakClass", "net.minecraftforge.fml.common.launcher.FMLTweaker", "--width", "1280", "--height", "800"]
    env = os.environ.copy()
    env["BCP_FORGE_AUDIT"] = "1" if audit else "0"
    env["BCP_FORGE_PLAY"] = "0" if audit else "1"
    selection = INSTANCE / "config/badlion-cosmetic-port/mesh-selection.json"
    saved = selection.read_bytes() if selection.exists() else None
    console_path = CACHE / ("audit-console.log" if audit else "preview-console.log")
    with console_path.open("w") as log:
        child = subprocess.Popen(args, cwd=INSTANCE, env=env, stdout=log, stderr=log,
                                 stdin=subprocess.DEVNULL, start_new_session=not audit)
    print(f"{'Audit' if audit else 'Preview'} PID {child.pid}; console: {console_path}", flush=True)
    if not audit:
        return
    try:
        try:
            code = child.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            (ROOT / "exports" / REPORT_NAME).write_text(json.dumps({
                "version": VERSION, "productionAudit": "timed_out", "timeoutSeconds": timeout,
                "console": str(console_path), "hostArchitecture": platform.machine()}, indent=2))
            raise
        console = console_path.read_text(errors="replace")
        report = {"version": VERSION, "minecraft": MC, "forge": FORGE,
                  "hostArchitecture": platform.machine(), "javaArchitecture": "x86_64",
                  "nativeVariant": runtime.get("nativeVariant", "stock"),
                  "offlineIdentity": True, "exitCode": code,
                  "modSha256": digest(INSTANCE / "mods" / JAR_NAME, "sha256"),
                  "families": {name: {"draws": int(draws), "textureChanges": int(textures)}
                               for name, draws, textures in re.findall(r"BCP_FORGE_DRAW (\w+) draws=(\d+) textures=(\d+)", console)},
                  "effects": {key: int(value) for key, value in re.findall(r"(shaders|cloakSpawned|cloakDrawn|fire|sonic)=(\d+)", console)},
                  "frameCycle": re.findall(r"BCP_FORGE_FRAME_CYCLE (frame-\d+)", console),
                  "airPet": re.findall(r"BCP_FORGE_PET_AIR joints=(\d+) draws=(\d+)", console),
                  "multiplayerSynchronization": "not implemented"}
        passed = (code == 0 and "BCP_FORGE_AUDIT PASS" in console
                  and set(report["families"]) == {"CAPE", "HAT", "BANDANA", "SHIELD", "PET", "WINGS"}
                  and all(f["draws"] > 0 for f in report["families"].values())
                  and all(report["effects"].get(k, 0) > 0 for k in ("shaders", "cloakSpawned", "cloakDrawn", "fire", "sonic"))
                  and bool(report["frameCycle"]) and any(int(j) == 32 and int(d) > 0 for j, d in report["airPet"])
                  and not any(t in console for t in ("Mesh rendering failed", "Mesh catalog initialization failed",
                                                     "Legacy cosmetic shader failed", "Cannot equip", "Cannot load particle sprite")))
        weapon = re.search(r"BCP_WEAPON_DRAW swords=(\d+) bows=(\d+) gui=(\d+) pulls=(\d+)",console)
        views = re.search(r"BCP_WEAPON_VIEWS first=(\d+) third=(\d+)",console)
        report["weapons"] = dict(zip(("swords","bows","gui","pulls"),map(int,weapon.groups()))) if weapon else {}
        report["weaponViews"] = dict(zip(("firstPerson","thirdPerson"),map(int,views.groups()))) if views else {}
        report["weaponAssets"] = re.findall(r"BCP_WEAPON_ASSETS PASS packs=(\d+) pullComponents=(\d+) textureFrames=(\d+)",console)
        passed &= bool(weapon) and all(int(v)>0 for v in weapon.groups()) and bool(views) and all(int(v)>0 for v in views.groups()) and bool(report["weaponAssets"])
        report["productionAudit"] = "passed" if passed else "failed"
        destination = ROOT / "exports" / REPORT_NAME
        destination.write_text(json.dumps(report, indent=2))
        print(json.dumps(report, indent=2))
        if not passed:
            raise RuntimeError("Mac production audit failed; see " + str(console_path))
    finally:
        if child.poll() is None:
            child.terminate()
            try:
                child.wait(timeout=15)
            except subprocess.TimeoutExpired:
                child.kill()
                child.wait()
        if saved is None:
            selection.unlink(missing_ok=True)
        else:
            selection.write_bytes(saved)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare", action="store_true")
    action = parser.add_mutually_exclusive_group()
    action.add_argument("--audit", action="store_true")
    action.add_argument("--play", action="store_true")
    parser.add_argument("--java-home")
    parser.add_argument("--native-mode", choices=("stock", "badlion"), default="badlion")
    parser.add_argument("--timeout", type=int, default=240)
    parser.add_argument("--minecraft", choices=("1.8.9", "1.12.2"), default="1.8.9")
    args = parser.parse_args()
    if args.minecraft == "1.12.2":
        MC = "1.12.2"; TARGET = "forge-1.12.2"; VERSION = "0.5.0"
        FORGE = "1.12.2-14.23.5.2864"
        JAR_NAME = "badlion-cosmetic-port-forge-1.12.2-0.5.0.jar"
        REPORT_NAME = "forge-1.12.2-0.5.0-mac-validation.json"
        CACHE = ROOT / "work/forge-1.12.2-mac"
        INSTANCE = ROOT / "instances/forge-1.12.2-mac"
    if platform.system() != "Darwin":
        parser.error("Use this helper on macOS")
    if not (args.prepare or args.audit or args.play):
        parser.error("Choose --prepare, --audit or --play")
    if args.prepare:
        prepare(args.java_home, args.native_mode)
    if args.audit or args.play:
        launch(args.audit, args.java_home, args.timeout)
