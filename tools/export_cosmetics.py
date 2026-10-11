"""Export the user's cached Badlion assets and decode animated cape textures.

No account credentials or remote services are accessed. Paths are taken only from
ZIP entries under the cosmetic prefixes and validated before writing.
"""
import argparse
import hashlib
import io
import json
import re
import zipfile
from pathlib import Path, PurePosixPath
from PIL import Image

PROJECT = Path(__file__).resolve().parents[1]
PREFIXES = ("assets/minecraft/blc/textures/cosmetics/", "blc/cosmetics/")

def export(cache, output, cape_ids):
    output.mkdir(parents=True, exist_ok=True)
    inventory = {}
    for archive in sorted(cache.glob("data*.jar")):
        with zipfile.ZipFile(archive) as source:
            for item in source.infolist():
                name = item.filename
                path = PurePosixPath(name)
                if not name.startswith(PREFIXES) or item.is_dir():
                    continue
                if path.is_absolute() or ".." in path.parts or "\\" in name or ":" in name:
                    raise ValueError(f"Unsafe archive entry: {name}")
                target = output / "raw" / path
                target.parent.mkdir(parents=True, exist_ok=True)
                data = source.read(item)
                target.write_bytes(data)
                inventory[name] = {"archive": archive.name, "bytes": len(data),
                                   "sha256": hashlib.sha256(data).hexdigest()}
    catalog = json.loads((output / "raw/blc/cosmetics/cosmetics.json").read_text())
    entries = {x["cosmeticId"]: x for x in catalog["registeredCosmetics"]["CAPE"]}
    packs = []
    for cape_id in cape_ids:
        candidates = [n for n in inventory if "/cosmetics/cloak/" in n
                      and PurePosixPath(n).stem == f"cloak_{cape_id}"
                      and PurePosixPath(n).suffix.lower() in (".avif", ".png")]
        if not candidates:
            print(f"Cape {cape_id}: texture missing from cache")
            continue
        path = output / "raw" / candidates[-1]
        with Image.open(path) as image:
            frames = getattr(image, "n_frames", 1)
            pack = output / "capes" / str(cape_id)
            pack.mkdir(parents=True, exist_ok=True)
            filenames = []
            for frame in range(frames):
                image.seek(frame)
                filename = f"frame-{frame:03d}.png"
                image.convert("RGBA").save(pack / filename)
                filenames.append(filename)
            metadata = entries[cape_id]
            config = {"id": cape_id, "name": metadata["name"], "frames": filenames,
                      "frameDelayMs": metadata.get("animationData", {}).get("frameDelay", 100),
                      "source": candidates[-1], "sourceSize": list(image.size),
                      "atlas": "badlion-original", "catalog": metadata}
            (pack / "cape.json").write_text(json.dumps(config, indent=2) + "\n")
            packs.append(config)
    manifest = {"entries": inventory, "exportedCapes": packs,
                "scope": "Locally cached assets; cache presence does not prove ownership."}
    (output / "export-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"Exported {len(inventory)} local files and {len(packs)} decoded cape packs to {output}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", type=Path, default=Path.home() / "AppData/Roaming/.minecraft/assets/blclient/objects")
    parser.add_argument("--output", type=Path, default=PROJECT / "exports/local-assets")
    parser.add_argument("--cape", type=int, action="append")
    args = parser.parse_args()
    export(args.cache, args.output.resolve(), args.cape or [1438, 732])
