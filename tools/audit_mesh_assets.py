"""Check every prepared PNG header and model hash against the unchanged exported sources."""
import collections
import hashlib
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
assets = ROOT / "exports/mesh-assets"
asset_root = assets.resolve()
manifest = json.loads((assets / "catalog.json").read_text())
frames = 0
for index, pack in enumerate(manifest["packs"]):
    if not (assets / pack["model"]).is_file():
        raise ValueError("Missing mesh " + pack["model"])
    folder_relative = Path(pack["frames"][0]).parent
    folder = (assets / folder_relative).resolve()
    if not folder.is_relative_to(asset_root):
        raise ValueError("Pack escaped asset root")
    for frame in pack["frames"]:
        relative = Path(frame)
        if relative.parent != folder_relative or relative.is_absolute() or '..' in relative.parts:
            raise ValueError("Frame escaped asset root")
        path = folder / relative.name
        with path.open("rb") as stream:
            header = stream.read(24)
        if header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
            raise ValueError("Invalid PNG header")
        if list(struct.unpack(">II", header[16:24])) != pack["sourceSize"]:
            raise ValueError("Original image resolution changed: " + frame)
        frames += 1
    if index % 500 == 0:
        print(f"Checked {index + 1}/{len(manifest['packs'])} packs ({frames} original-resolution frames)", flush=True)
for relative, record in manifest["models"].items():
    actual = hashlib.sha256((assets / relative).read_bytes()).hexdigest()
    original = hashlib.sha256((ROOT / "exports/local-assets/raw" / record["source"]).read_bytes()).hexdigest()
    if actual != record["sha256"] or original != actual:
        raise ValueError("Geometry/animation bytes changed: " + relative)
report = {"packs": len(manifest["packs"]), "categories": dict(collections.Counter(p["type"] for p in manifest["packs"])),
          "originalResolutionFrames": frames, "unchangedModelAndAnimationFiles": len(manifest["models"]), "skippedCatalogRecords": len(manifest["skipped"]),
          "checks": "passed; PNG dimensions and model bytes checked, not a full PNG decode audit"}
(ROOT / "exports/mesh-asset-validation.json").write_text(json.dumps(report, indent=2))
print(json.dumps(report))
