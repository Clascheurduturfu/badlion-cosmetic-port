"""Prepare every compatible cached cape, with no ownership filter."""
import concurrent.futures
import argparse
import json
import re
import shutil
from pathlib import Path
from PIL import Image
from prepare_capes import convert

PROJECT = Path(__file__).resolve().parents[1]
ROOT = PROJECT / "exports/local-assets"
RAW = ROOT / "raw"
CATALOG = json.loads((RAW / "blc/cosmetics/cosmetics.json").read_text())
METADATA = {entry["cosmeticId"]: entry for entry in CATALOG["registeredCosmetics"]["CAPE"]}

def prepare(source):
    identifier = int(re.fullmatch(r"cloak_(\d+)", source.stem)[1])
    folder = ROOT / "capes" / str(identifier)
    folder.mkdir(parents=True, exist_ok=True)
    meta = METADATA.get(identifier, {"name": f"Cached cape {identifier}"})
    names = []
    try:
        with Image.open(source) as image:
            source_size = list(image.size)
            frames = getattr(image, "n_frames", 1)
            for frame in range(frames):
                image.seek(frame)
                converted = convert(image)
                name = f"portable-{frame:03d}.png"
                converted.save(folder / name)
                names.append(name)
        pack = {"id": identifier, "name": meta["name"], "portableFrames": names,
                "frameDelayMs": meta.get("animationData", {}).get("frameDelay", 100),
                "source": source.relative_to(RAW).as_posix(), "sourceSize": source_size,
                "portableRenderer": "vanilla cape; common atlas panel mapping; texture animation only",
                "catalog": meta}
        (folder / "cape.json").write_text(json.dumps(pack, indent=2) + "\n")
        return {"id": identifier, "frames": len(names), "status": "prepared"}
    except Exception as error:
        # A failed conversion must not leave a selectable partial pack.
        (folder / "cape.json").unlink(missing_ok=True)
        return {"id": identifier, "status": "unsupported", "reason": str(error)}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--retry-unsupported", action="store_true")
    args = parser.parse_args()
    files = {}
    for path in sorted((RAW / "assets/minecraft/blc/textures/cosmetics/cloak").rglob("*")):
        if path.suffix.lower() in (".avif", ".png") and re.fullmatch(r"cloak_\d+", path.stem):
            files[int(path.stem.split("_")[-1])] = path
    results = []
    if args.retry_unsupported:
        previous = json.loads((ROOT / "cape-conversion-report.json").read_text())
        results = [entry for entry in previous["results"] if entry["status"] == "prepared"]
        completed = {entry["id"] for entry in results}
        pending = [path for identifier, path in files.items() if identifier not in completed]
    else:
        pending = list(files.values())
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        for count, result in enumerate(pool.map(prepare, pending), 1):
            results.append(result)
            if count % 100 == 0:
                print(f"Processed {count}/{len(pending)} cached capes", flush=True)
    report = {"ownershipFilter": False, "sourceCount": len(files), "results": results,
              "prepared": sum(r["status"] == "prepared" for r in results)}
    (ROOT / "cape-conversion-report.json").write_text(json.dumps(report, indent=2) + "\n")
    print(f"Prepared {report['prepared']}/{len(files)} cape packs; report records unsupported atlas formats.", flush=True)
