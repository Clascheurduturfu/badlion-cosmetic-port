"""Package source separately from the user's locally exported cosmetic assets."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

PROJECT = Path(__file__).resolve().parents[1]
VERSION = "0.2.2"


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def package(destination):
    destination.mkdir(parents=True, exist_ok=True)
    instance = PROJECT / "instances/fabric-1.21.1"
    jar = instance / "mods" / f"badlion-cosmetic-port-{VERSION}.jar"
    validation = json.loads((PROJECT / "exports/port-validation.json").read_text(encoding="utf-8-sig"))
    if validation["version"] != VERSION or validation["modSha256"] != sha256(jar):
        raise ValueError("The installed release JAR has not passed production validation")
    source = destination / f"cosmetic-port-{VERSION}-source.zip"
    with zipfile.ZipFile(source, "w", zipfile.ZIP_DEFLATED) as archive:
        roots = [PROJECT / "mod/src", PROJECT / "mod/gradle", PROJECT / "tools"]
        for root in roots:
            for path in sorted(root.rglob("*")):
                if path.is_file() and "__pycache__" not in path.parts:
                    archive.write(path, path.relative_to(PROJECT))
        for relative in ["mod/build.gradle", "mod/settings.gradle", "mod/gradle.properties",
                         "mod/gradlew.bat", "mod/build.ps1", "mod/smoke-test.ps1",
                         "mod/play-dev.ps1", "mod/README.md", "prepare-instance.ps1",
                         "exports/REVERSE-ENGINEERING.md", "exports/reverse-engineering.json",
                         "exports/asset-validation.json", "exports/port-validation.json"]:
            archive.write(PROJECT / relative, relative)

    bundle = destination / f"cosmetic-port-{VERSION}-personal-instance.zip"
    with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED, compresslevel=1) as archive:
        for path in sorted((instance / "mods").glob("*.jar")):
            archive.write(path, path.relative_to(instance))
        capes = instance / "config/badlion-cosmetic-port/capes"
        for manifest in sorted(capes.glob("*/cape.json")):
            pack = json.loads(manifest.read_text())
            archive.write(manifest, manifest.relative_to(instance))
            for name in pack["portableFrames"]:
                frame = manifest.parent / name
                if not frame.resolve().is_relative_to(manifest.parent.resolve()):
                    raise ValueError("Invalid cape frame path")
                archive.write(frame, frame.relative_to(instance))
        archive.writestr("config/badlion-cosmetic-port/selection.json", '{"capeId":1438}\n')
        archive.writestr("INSTALL.txt", (
            f"Cosmetic Port {VERSION} - personal local asset export\n\n"
            "Extract this ZIP into a separate Minecraft game directory.\n"
            "Use Minecraft 1.21.1, Fabric Loader 0.16.14 or newer, and Java 21.\n"
            "Fabric API is included under mods. Point your launcher installation\n"
            "at this game directory. Press K in a world to select a cape; F5\n"
            "shows your character. Skin Customization > Cape must be enabled.\n"
            "1,405 cached cape packs; no ownership filter. Local player only.\n"
            "Other cosmetic types and custom cloth/particles are not rendered.\n"
            "In-world appearance is not yet visually calibrated.\n"
        ))
        archive.write(PROJECT / "exports/local-assets/cape-conversion-report.json", "reports/cape-conversion-report.json")
        archive.write(PROJECT / "exports/port-validation.json", "reports/port-validation.json")

    result = {"version": VERSION, "files": [{"name": path.name, "bytes": path.stat().st_size,
               "sha256": sha256(path)} for path in (source, bundle)], "validation": validation}
    (destination / f"cosmetic-port-{VERSION}-manifest.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--destination", type=Path, default=PROJECT / "releases")
    package(parser.parse_args().destination)
