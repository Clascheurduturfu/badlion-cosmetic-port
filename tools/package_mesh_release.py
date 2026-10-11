"""Preserve the validated mesh adapter and scoped class capture without account files."""
import hashlib
import json
import zipfile
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION = "0.3.6"


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def package():
    releases = ROOT / "releases"
    releases.mkdir(exist_ok=True)
    validation = json.loads((ROOT / "exports/mesh-port-validation.json").read_text(encoding="utf-8-sig"))
    jar = ROOT / f"mod/build/libs/badlion-cosmetic-port-{VERSION}.jar"
    installed = ROOT / f"instances/fabric-1.21.1-mesh/mods/{jar.name}"
    if validation["version"] != VERSION or validation["modSha256"] != sha256(jar) or sha256(installed) != sha256(jar):
        raise ValueError("Built and installed JAR must match the tested release")
    core = json.loads((ROOT / "exports/mesh-core-validation.json").read_text(encoding="utf-8-sig"))
    if (core["failures"] != 0 or not all(validation["poseVariantPet"].values())
            or not validation.get("proceduralCloak") or not validation.get("heldShieldReplacement")
            or not validation.get("shieldWithoutItemToggle")
            or not all(validation.get("petEffects", {}).values())):
        raise ValueError("Required mesh/production checks did not pass")
    source = releases / f"cosmetic-port-{VERSION}-source.zip"
    with zipfile.ZipFile(source, "w", zipfile.ZIP_DEFLATED) as archive:
        for relative in ("mod/src", "shared/src", "mod/gradle", "tools", "tests"):
            for path in sorted((ROOT / relative).rglob("*")):
                if path.is_file() and "__pycache__" not in path.parts:
                    archive.write(path, path.relative_to(ROOT))
        for relative in (
            "mod/build.gradle", "mod/settings.gradle", "mod/gradle.properties",
            "mod/gradlew.bat", "mod/build.ps1", "mod/README.md", "prepare-mesh-instance.ps1",
            "exports/renderer-recovery-report.md", "exports/renderer-decompilation-index.json",
            "exports/mesh-core-validation.json", "exports/mesh-asset-validation.json",
            "exports/mesh-port-validation.json", "exports/effect-shader-provenance.json",
            "exports/renderer-corrections-0.3.6.md", "exports/native-motion-parity.json",
            "exports/native-ribbon-parity.json", "exports/mesh-production-0.3.6.log",
            "exports/wing-joint-parity.json", "exports/mesh-assets/thumbnails/manifest.json",
            "exports/rea-native/NATIVE-LOADER-FINDINGS.md",
        ):
            archive.write(ROOT / relative, relative)
        archive.write(jar, "release/" + jar.name)

    capture_root = ROOT / "exports/runtime-classes"
    capture = json.loads((capture_root / "capture-manifest.json").read_text())
    saved_capture = releases / f"cosmetic-port-{VERSION}-runtime-capture.zip"
    if "--reuse-capture" in sys.argv:
        previous = json.loads((releases / f"cosmetic-port-{VERSION}-manifest.json").read_text())
        record = next(item for item in previous["files"] if item["name"] == saved_capture.name)
        if sha256(saved_capture) != record["sha256"]:
            raise ValueError("Saved capture archive checksum changed")
        with zipfile.ZipFile(saved_capture) as archive:
            if json.loads(archive.read("capture-manifest.json")) != capture:
                raise ValueError("Capture manifest changed; cannot reuse its archive")
        print("Reused verified, unchanged runtime capture archive", flush=True)
    else:
        preserve_capture(saved_capture, capture_root, capture)
    for path in (source, saved_capture):
        with zipfile.ZipFile(path) as archive:
            if archive.testzip() is not None:
                raise ValueError("Archive CRC check failed: " + path.name)
    manifest = {
        "version": VERSION, "modSha256": sha256(jar),
        "files": [{"name": path.name, "bytes": path.stat().st_size, "sha256": sha256(path)} for path in (source, saved_capture)],
        "captureClasses": len(capture["records"]),
        "excludes": ["account files", "launcher-profile backups", "decoded local texture collection"],
        "assetRequirement": "JAR requires project exports/mesh-assets catalog, textures and models",
        "validation": validation,
    }
    (releases / f"cosmetic-port-{VERSION}-manifest.json").write_text(json.dumps(manifest, indent=2))
    print(json.dumps({"version": VERSION, "archives": manifest["files"], "captureClasses": len(capture["records"])}), flush=True)


def preserve_capture(saved_capture, capture_root, capture):
    with zipfile.ZipFile(saved_capture, "w", zipfile.ZIP_DEFLATED, compresslevel=1) as archive:
        archive.write(capture_root / "capture-manifest.json", "capture-manifest.json")
        for index, record in enumerate(capture["records"]):
            relative = Path(record["path"])
            if relative.is_absolute() or ".." in relative.parts:
                raise ValueError("Capture path escaped its root")
            path = capture_root / relative
            if sha256(path) != record["sha256"]:
                raise ValueError("Captured class changed: " + record["name"])
            archive.write(path, relative.as_posix())
            if index % 1500 == 0:
                print(f"Preserved {index + 1}/{len(capture['records'])} captured classes", flush=True)
        for path in sorted((ROOT / "work/decompiled-renderer").rglob("*.java")):
            archive.write(path, Path("reconstructions") / path.relative_to(ROOT / "work/decompiled-renderer"))
        archive.write(ROOT / "exports/renderer-decompilation-index.json", "renderer-decompilation-index.json")


if __name__ == "__main__":
    package()
