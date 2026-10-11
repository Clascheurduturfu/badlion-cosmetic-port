"""Decompile selected captured renderer classes into collision-safe directories."""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = Path("C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin/java.exe")
DEFAULT_CLASSES = ["aCA", "aCE", "aCF", "aCH", "aCJ", "aCf", "aCv", "aCz",
                   "aGe", "aGd", "aGi", "aGl", "aGm", "aGn", "aGo", "aGp",
                   "aGq", "aGr", "aGs", "aGt", "aGu", "aGv", "aGw", "aGx", "aGy"]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("classes", nargs="*", default=DEFAULT_CLASSES)
    args = parser.parse_args()
    capture = ROOT / "exports/runtime-classes"
    manifest = json.loads((capture / "capture-manifest.json").read_text(encoding="utf-8"))
    records = {record["name"]: record for record in manifest["records"]}
    index_path = ROOT / "exports/renderer-decompilation-index.json"
    index = json.loads(index_path.read_text()) if index_path.exists() else {}
    for short_name in args.classes:
        name = short_name if "/" in short_name else "net/badlion/a/" + short_name
        if name not in records:
            print("not captured:", name, flush=True)
            continue
        record = records[name]
        source = capture / record["path"]
        if hashlib.sha256(source.read_bytes()).hexdigest() != record["sha256"]:
            raise ValueError("captured class changed: " + name)
        destination = ROOT / "work/decompiled-renderer" / record["sha256"][:12]
        destination.mkdir(parents=True, exist_ok=True)
        expected = destination / (name + ".java")
        if not expected.exists():
            result = subprocess.run([
                str(JAVA), "-Xmx256m", "-jar", str(ROOT / "work/tools/cfr-0.152.jar"),
                str(source), "--extraclasspath", str(ROOT / "work/renderer-audit/captured-1.21.1.jar"),
                "--outputdir", str(destination), "--caseinsensitivefs", "false", "--silent", "true"
            ], capture_output=True, text=True, timeout=60)
            (destination / "decompiler.stderr.log").write_text(result.stderr)
        if expected.exists():
            index[name] = {**record, "javaPath": str(expected.relative_to(ROOT)),
                           "decompiler": "CFR 0.152", "reconstruction": True}
            print("decompiled:", name, flush=True)
        else:
            print("unresolved:", name, flush=True)
        index_path.write_text(json.dumps(index, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
