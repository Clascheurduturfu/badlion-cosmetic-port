"""Prepare decoded Badlion cape atlases for a basic vanilla cape renderer.

This maps the common 88x92 two-panel Badlion layout onto Minecraft's 64x32
cape layout. It preserves texture frame timing, not Badlion cloth deformation,
custom meshes, particles or glow. Original exports remain untouched.
"""
import argparse
import json
from pathlib import Path
from PIL import Image

PROJECT = Path(__file__).resolve().parents[1]

def convert(image):
    image = image.convert("RGBA")
    width, height = image.size
    if width == height * 2:
        return image.resize((256, 128), Image.Resampling.NEAREST)
    # Some cached encodes trim or round a few pixels from the common atlas.
    # Scale each axis independently to retain the same panel boundaries.
    if abs(width / height - 88 / 92) > (88 / 92) * 0.01:
        raise ValueError(f"Unsupported Badlion cape atlas {width}x{height}")
    scale_x, scale_y = width / 88, height / 92
    atlas = Image.new("RGBA", (256, 128))
    def panel(source, destination):
        crop = image.crop(tuple(round(value * (scale_x if index % 2 == 0 else scale_y))
                                for index, value in enumerate(source)))
        x, y, w, h = destination
        atlas.paste(crop.resize((w * 4, h * 4), Image.Resampling.NEAREST), (x * 4, y * 4))
    panel((4, 4, 44, 68), (1, 1, 10, 16))
    panel((44, 4, 84, 68), (12, 1, 10, 16))
    panel((0, 4, 4, 68), (0, 1, 1, 16))
    panel((84, 4, 88, 68), (11, 1, 1, 16))
    panel((4, 0, 44, 4), (1, 0, 10, 1))
    panel((4, 68, 44, 72), (11, 0, 10, 1))
    return atlas

def prepare(root):
    count = 0
    for manifest in sorted(root.glob("*/cape.json")):
        pack = json.loads(manifest.read_text())
        filenames = []
        for index, frame in enumerate(pack["frames"]):
            filename = f"portable-{index:03d}.png"
            with Image.open(manifest.parent / frame) as image:
                result = convert(image)
                result.save(manifest.parent / filename)
                assert result.size == (256, 128)
            filenames.append(filename)
        pack["portableFrames"] = filenames
        pack["portableRenderer"] = "vanilla cape; common atlas panel mapping; texture animation only"
        manifest.write_text(json.dumps(pack, indent=2) + "\n")
        count += 1
        print(f"Prepared {pack['name']}: {len(filenames)} animated frames")
    return count

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--capes", type=Path, default=PROJECT / "exports/local-assets/capes")
    args = parser.parse_args()
    prepare(args.capes)
