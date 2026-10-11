"""Restore and decompile captured renderer classes using the handoff on any OS."""
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import zipfile
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("classes", nargs="*", help="Short names, for example aCH aMI, or full slash-separated names")
    parser.add_argument("--java", default=None)
    args = parser.parse_args()
    capture = ROOT / "exports/runtime-classes"
    classpath = ROOT / "work/renderer-audit/captured-1.21.1.jar"
    capture.mkdir(parents=True, exist_ok=True)
    classpath.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(ROOT / "reference/cosmetic-port-0.3.6-runtime-capture.zip") as archive:
        manifest = json.loads(archive.read("capture-manifest.json"))
        with zipfile.ZipFile(classpath, "w", zipfile.ZIP_DEFLATED) as combined:
            for record in manifest["records"]:
                relative = PurePosixPath(record["path"].replace("\\", "/"))
                name = PurePosixPath(record["name"])
                if relative.is_absolute() or ".." in relative.parts or name.is_absolute() or ".." in name.parts:
                    raise ValueError("Captured path escaped its root")
                data = archive.read(relative.as_posix())
                if hashlib.sha256(data).hexdigest() != record["sha256"]:
                    raise ValueError("Captured class checksum mismatch: " + record["name"])
                target = capture.joinpath(*relative.parts)
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
                combined.writestr(record["name"] + ".class", data)
        (capture / "capture-manifest.json").write_text(json.dumps(manifest, indent=2))
    print(f"Restored and verified {len(manifest['records'])} captured classes")
    if not args.classes:
        return
    home = os.environ.get("JAVA_HOME")
    java = args.java or (str(Path(home) / "bin" / ("java.exe" if os.name == "nt" else "java")) if home else shutil.which("java"))
    if not java:
        raise RuntimeError("Set JAVA_HOME or provide --java")
    records = {record["name"]: record for record in manifest["records"]}
    for short in args.classes:
        name = short if "/" in short else "net/badlion/a/" + short
        record = records[name]
        source = capture.joinpath(*PurePosixPath(record["path"].replace("\\", "/")).parts)
        destination = ROOT / "work/decompiled-renderer" / record["sha256"][:12]
        destination.mkdir(parents=True, exist_ok=True)
        subprocess.run([java, "-Xmx256m", "-jar", str(ROOT / "reference/cfr-0.152.jar"), str(source),
                        "--extraclasspath", str(classpath), "--outputdir", str(destination),
                        "--caseinsensitivefs", "false", "--silent", "true"], check=True, timeout=120)
        print("Decompiled " + name)


if __name__ == "__main__":
    main()
