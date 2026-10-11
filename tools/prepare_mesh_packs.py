"""Prepare original-resolution cosmetic textures and original OBJ/MD5 assets."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
TYPES = {"CAPE": "cloak", "HAT": "hat", "BANDANA": "bandana",
         "SHIELD": "shield", "PET": "pet", "WINGS": "wing"}


def prepare(output: Path) -> None:
    raw = ROOT / "exports/local-assets/raw"
    source_root = raw / "assets/minecraft/blc/textures/cosmetics"
    catalog = json.loads((raw / "blc/cosmetics/cosmetics.json").read_text())["registeredCosmetics"]
    models = json.loads((raw / "blc/cosmetics/models.json").read_text())["modelData"]
    output.mkdir(parents=True, exist_ok=True)
    provenance = {}
    for category, directory in TYPES.items():
        original = source_root / directory / "models"
        if not original.exists():
            continue
        for path in original.rglob("*"):
            if path.suffix.lower() not in (".obj", ".md5mesh", ".md5anim"):
                continue
            relative = Path("models") / directory / path.relative_to(original)
            target = output / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, target)
            provenance[relative.as_posix()] = {"source": str(path.relative_to(raw)),
                                             "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
    jobs = []
    skipped = []
    for category, directory in TYPES.items():
        textures: dict[str, list[Path]] = {}
        for path in (source_root / directory).rglob("*.avif"):
            if "models" not in path.parts:
                textures.setdefault(path.stem, []).append(path)
        for entry in catalog.get(category, []):
            identifier = entry["cosmeticId"]
            choices = textures.get(f"{directory}_{identifier}", [])
            if not choices:
                skipped.append({"type": category, "id": identifier, "reason": "cached texture missing"})
                continue
            model_name = entry.get("modelName", "").strip()
            if not model_name:
                model_name = "cloak_v2" if category == "CAPE" else directory
            metadata = models.get(category, {}).get(model_name)
            if metadata is None:
                skipped.append({"type": category, "id": identifier, "reason": "model metadata missing", "model": model_name})
                continue
            extension = ".md5mesh" if metadata.get("md5format") else ".obj"
            # Catalog keys can be aliases: chroma_bandana resolves to bandana.obj,
            # butterfly_wings resolves to butterfly_wings_1.obj.
            geometry_name = metadata.get("modelName", model_name)
            if metadata.get("animationData") and extension == ".obj":
                geometry_name += "_0"
            paths = list((output / "models" / directory).rglob(geometry_name + extension))
            if not paths:
                skipped.append({"type": category, "id": identifier, "reason": "cached model missing", "model": model_name})
                continue
            # Modern cache entries use an ID subfolder; retain those in preference
            # to older root entries when both encodes are cached.
            texture = max(choices, key=lambda p: (p.parent.name == str(identifier), p.as_posix()))
            model = max(paths, key=lambda p: (p.parent.name == model_name, p.as_posix()))
            folder = output / "packs" / category / str(identifier)
            jobs.append((category, entry, metadata, texture, model, folder))

    def convert(job: tuple) -> dict:
        category, entry, metadata, texture, model, folder = job
        folder.mkdir(parents=True, exist_ok=True)
        try:
            config = folder / "pack.json"
            digest = hashlib.sha256(texture.read_bytes()).hexdigest()
            if config.exists():
                previous = json.loads(config.read_text())
                if previous.get("sourceSha256") == digest and all((output / f).is_file() for f in previous["frames"]):
                    return previous
            frames, durations = [], []
            delay = entry.get("animationData", {}).get("frameDelay", 100)
            with Image.open(texture) as image:
                count = getattr(image, "n_frames", 1)
                if count > 256 or max(image.size) > 4096:
                    raise ValueError("texture exceeds supported frame/dimension bounds")
                size = list(image.size)
                for index in range(count):
                    image.seek(index)
                    target = folder / f"frame-{index:03d}.png"
                    image.convert("RGBA").save(target)
                    frames.append(target.relative_to(output).as_posix())
                    duration = delay if delay > 0 else image.info.get("duration", 100)
                    durations.append(max(1, int(duration or 100)))
            animation_paths = {p.stem: p.relative_to(output).as_posix()
                               for p in model.parent.glob("*.md5anim")}
            pack = {"type": category, "id": entry["cosmeticId"], "name": entry["name"],
                    "model": model.relative_to(output).as_posix(), "modelMetadata": metadata,
                    "frames": frames, "frameDurationsMs": durations, "sourceSize": size,
                    "animations": animation_paths, "catalog": entry,
                    "source": texture.relative_to(raw).as_posix(), "sourceSha256": digest}
            config.write_text(json.dumps(pack, indent=2), encoding="utf-8")
            return pack
        except Exception as error:
            return {"type": category, "id": entry["cosmeticId"], "error": str(error)}

    entries = []
    with ThreadPoolExecutor(max_workers=2) as executor:
        for index, pack in enumerate(executor.map(convert, jobs)):
            if "error" in pack:
                skipped.append(pack)
            else:
                entries.append(pack)
            if index % 100 == 0:
                print(f"Prepared {index + 1}/{len(jobs)} mesh texture packs", flush=True)
    manifest = {"formatVersion": 1, "packs": entries, "models": provenance,
                "skipped": skipped, "scope": "Cached assets; original pixel dimensions and geometry preserved",
                "limitations": ["Multipart texture/material variants and effect shaders require separate adaptation",
                                "Original private selection service is not used"]}
    (output / "catalog.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(json.dumps({"prepared": len(entries), "skipped": len(skipped), "models": len(provenance)}), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=ROOT / "exports/mesh-assets")
    arguments = parser.parse_args()
    prepare(arguments.output.resolve())
