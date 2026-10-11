"""Create the scoped portable project handoff, excluding account and host files."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOP = "badlion-cosmetic-port"


def files():
    seen = set()

    def add(path, relative=None):
        relative = relative or path.relative_to(ROOT).as_posix()
        if relative in seen:
            return []
        seen.add(relative)
        return [(path, relative)]

    for directory in ("src", "vendor", "shared/src", "mod/src", "mod/gradle", "forge-1.8.9/src",
                      "forge-1.8.9/gradle", "tools", "tests", "exports/mesh-assets"):
        for path in sorted((ROOT / directory).rglob("*")):
            if path.is_file() and not any(part in {".git", "__pycache__", "node_modules"} for part in path.parts):
                yield from add(path)
    for folder in ("mod", "forge-1.8.9"):
        for path in sorted((ROOT / folder).glob("*")):
            if path.is_file():
                yield from add(path)
    for relative in ("HANDOFF.md", "README.md", ".gitignore", "CMakeLists.txt", "build.ps1", "manage.ps1",
                     "patch_forwarders.py", "prepare-instance.ps1", "prepare-mesh-instance.ps1"):
        yield from add(ROOT / relative)
    for path in sorted((ROOT / "exports").glob("*")):
        if path.is_file() and path.suffix.lower() in {".json", ".md"}:
            yield from add(path)
    for relative in ("exports/forge-0.4.0-production.log", "exports/mesh-production-0.3.6.log",
                     "exports/rea-native/NATIVE-LOADER-FINDINGS.md",
                     "exports/rea-native/Agent_OnLoad.ghidra.c", "exports/rea-native/Callback_ClassFileLoadHook.ghidra.c"):
        yield from add(ROOT / relative)
    for path in sorted((ROOT / "work/decompiled-renderer").rglob("*.java")):
        yield from add(path, "reference/decompiled-renderer/" + path.relative_to(ROOT / "work/decompiled-renderer").as_posix())
    for relative in ("dist/version.dll", "dist/cosmetics.json", "dist/build-report.json", "dist/stock-response-schema.json"):
        yield from add(ROOT / relative)
    for name in ("badlion-cosmetic-port-0.1.3-confirmed.zip", "cosmetic-port-0.3.6-runtime-capture.zip"):
        yield from add(ROOT / "releases" / name, "reference/" + name)
    for path in (ROOT / "mod/build/libs/badlion-cosmetic-port-0.3.6.jar",
                 ROOT / "forge-1.8.9/build/libs/badlion-cosmetic-port-forge-0.4.0.jar"):
        yield from add(path, "release/" + path.name)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    paths = list(files())
    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(".partial")
    manifest = {"handoffVersion": "2026-10-11.1", "files": [], "excludes": [
        "launcher/accounts/tokens", "private game chat", "full JVM fatal reports", "saves",
        "Windows runtimes", "disposable Gradle/build caches", "conversation history"]}
    total = sum(path.stat().st_size for path, _ in paths)
    print(f"Packing {len(paths)} files, {total / 2**30:.2f} GiB uncompressed", flush=True)
    complete = 0
    with zipfile.ZipFile(temporary, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=1, allowZip64=True) as archive:
        for index, (path, relative) in enumerate(paths):
            digest = hashlib.sha256()
            with path.open("rb") as source, archive.open(TOP + "/" + relative, "w", force_zip64=True) as destination:
                for chunk in iter(lambda: source.read(1024 * 1024), b""):
                    digest.update(chunk)
                    destination.write(chunk)
            size = path.stat().st_size
            manifest["files"].append({"path": relative, "bytes": size, "sha256": digest.hexdigest()})
            complete += size
            if index % 2000 == 0:
                print(f"{index + 1}/{len(paths)} files, {complete / max(total, 1):.0%} bytes", flush=True)
        archive.writestr(TOP + "/HANDOFF-MANIFEST.json", json.dumps(manifest, indent=2))
    print("Checking full archive CRCs", flush=True)
    with zipfile.ZipFile(temporary) as archive:
        failure = archive.testzip()
        if failure:
            raise RuntimeError("CRC mismatch: " + failure)
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise RuntimeError("Duplicate archive paths")
    temporary.replace(output)
    with output.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    output.with_suffix(".zip.sha256").write_text(digest + "  " + output.name + "\n", encoding="ascii")
    output.with_suffix(".manifest.json").write_text(json.dumps({
        "filename": output.name, "bytes": output.stat().st_size, "sha256": digest,
        "files": len(paths), "uncompressedBytes": total, "crcCheck": "passed"}, indent=2))
    print(json.dumps({"zip": str(output), "bytes": output.stat().st_size, "sha256": digest}), flush=True)


if __name__ == "__main__":
    main()
