"""Decode original particle sprites, retaining source resolution and file hashes."""
import hashlib
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "exports/local-assets/raw/assets/minecraft/blc/textures/cosmetics/particles"
destination = ROOT / "exports/mesh-assets/particles"
destination.mkdir(parents=True, exist_ok=True)
records = []
for path in sorted(source.glob("*.avif")):
    with Image.open(path) as image:
        image.seek(0)
        image.convert("RGBA").save(destination / (path.stem + ".png"))
        records.append({"name": path.stem, "size": image.size, "sourceSha256": hashlib.sha256(path.read_bytes()).hexdigest(), "frames": getattr(image, "n_frames", 1)})
(destination / "manifest.json").write_text(json.dumps(records, indent=2))
print(json.dumps({"particleSprites": len(records), "animatedSources": sum(record["frames"] > 1 for record in records)}))
