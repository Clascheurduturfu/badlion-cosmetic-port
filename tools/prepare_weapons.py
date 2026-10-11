"""Append cached Badlion sword/bow assets to the portable catalog (Pillow with AVIF)."""
import argparse
import hashlib
import json
import os
import zipfile
from pathlib import Path
from PIL import Image
import io

ROOT = Path(__file__).resolve().parents[1]


def prepare(cache, output):
    resources = {}
    jars = sorted(cache.glob('data*.jar'))
    for source in jars:
        with zipfile.ZipFile(source) as archive:
            for name in archive.namelist():
                if name.endswith('/'):
                    continue
                if name in ('blc/cosmetics/cosmetics.json', 'blc/cosmetics/models.json') or any(
                        '/cosmetics/' + kind + '/' in name for kind in ('sword', 'bow')):
                    resources[name] = archive.read(name)
    stock = json.loads(resources['blc/cosmetics/cosmetics.json'])['registeredCosmetics']
    metadata = json.loads(resources['blc/cosmetics/models.json'])['modelData']
    catalog_path = output / 'catalog.json'
    catalog = json.loads(catalog_path.read_text())
    prepared, skipped, checksums = [], [], {}

    def convert(source, folder, prefix, delay):
        frames = []
        data = resources[source]
        with Image.open(io.BytesIO(data)) as image:
            if max(image.size) > 4096 or image.n_frames > 256:
                raise ValueError('Texture bounds exceeded: ' + source)
            size = list(image.size)
            for index in range(image.n_frames):
                image.seek(index)
                path = folder / (prefix + '-%03d.png' % index)
                image.convert('RGBA').save(path)
                frames.append(path.relative_to(output).as_posix())
        checksums[source] = hashlib.sha256(data).hexdigest()
        return {'frames': frames, 'frameDurationsMs': [max(10, delay)] * len(frames), 'sourceSize': size}

    for kind in ('SWORD', 'BOW'):
        directory = kind.lower()
        prefix = 'assets/minecraft/blc/textures/cosmetics/' + directory + '/'
        for name, data in resources.items():
            if name.startswith(prefix + 'models/') and name.endswith('.obj'):
                path = output / 'models' / directory / name[len(prefix + 'models/'):]
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(data)
                catalog['models'][path.relative_to(output).as_posix()] = {
                    'source': name, 'sha256': hashlib.sha256(data).hexdigest()}
        for entry in stock.get(kind, []):
            identifier = entry['cosmeticId']
            info = metadata[kind].get(entry.get('modelName'))
            if not info:
                skipped.append({'type': kind, 'id': identifier, 'reason': 'Missing model metadata'})
                continue
            geometry_name = info['modelName'] + ('_0' if info.get('animationData') else '')
            candidates = sorted((output / 'models' / directory).rglob(geometry_name + '.obj'))
            if not candidates:
                skipped.append({'type': kind, 'id': identifier, 'reason': 'Missing model'})
                continue
            model = max(candidates, key=lambda p: p.parent.name == info['modelName'])
            sources = {Path(n).stem: n for n in resources if n.startswith(prefix + str(identifier) + '/')
                       and n.endswith('.avif') and '/gui/' not in n}
            required = ('wood', 'stone', 'iron', 'gold', 'diamond') if kind == 'SWORD' else ('bow',)
            if any(name not in sources for name in required):
                skipped.append({'type': kind, 'id': identifier, 'reason': 'Missing material textures'})
                continue
            folder = output / 'packs' / kind / str(identifier)
            folder.mkdir(parents=True, exist_ok=True)
            delay = entry.get('animationData', {}).get('frameDelay', 100)
            textures = {name: convert(source, folder, name, delay) for name, source in sources.items()}
            base = textures['diamond' if kind == 'SWORD' else 'bow']
            pack = dict(type=kind, id=identifier, name=entry['name'], model=model.relative_to(output).as_posix(),
                        modelMetadata=info, catalog=entry, animations={}, textures=textures, **base)
            pack['source'] = sources['diamond' if kind == 'SWORD' else 'bow']
            pack['sourceSha256'] = checksums[pack['source']]
            # GUI images are dedicated artwork, not projections of the 3D mesh.
            if kind == 'SWORD':
                for material in ('wood', 'stone', 'iron', 'gold', 'diamond', 'netherite'):
                    gui = prefix + str(identifier) + '/gui/' + material + '.avif'
                    if gui in resources:
                        textures[material + '-gui'] = convert(gui, folder, material + '-gui', delay)
            thumb = textures.get('diamond-gui', textures.get('bow-gui', base))['frames'][0]
            thumb_path = output / 'thumbnails' / kind / (str(identifier) + '.png')
            thumb_path.parent.mkdir(parents=True, exist_ok=True)
            with Image.open(output / thumb) as image:
                image.thumbnail((128, 128), Image.Resampling.LANCZOS)
                canvas = Image.new('RGBA', (128, 128))
                canvas.paste(image.convert('RGBA'), ((128-image.width)//2, (128-image.height)//2))
                canvas.save(thumb_path)
            (folder / 'pack.json').write_text(json.dumps(pack, indent=2))
            prepared.append(pack)
    catalog['packs'] = [p for p in catalog['packs'] if p['type'] not in ('SWORD', 'BOW')] + prepared
    catalog['skipped'] = [p for p in catalog['skipped'] if p['type'] not in ('SWORD', 'BOW')] + skipped
    catalog_path.write_text(json.dumps(catalog, indent=2))
    report = {'prepared': {kind: sum(p['type'] == kind for p in prepared) for kind in ('SWORD', 'BOW')},
              'skipped': skipped, 'sourceTextureSha256': checksums,
              'cache': str(cache), 'source': 'Original cached OBJ/AVIF; no replacement artwork'}
    (ROOT / 'exports/weapon-assets.json').write_text(json.dumps(report, indent=2))
    print(json.dumps({k: v for k, v in report.items() if k != 'sourceTextureSha256'}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    default_game = Path(os.environ.get('APPDATA', Path.home() / 'Library/Application Support')) / (
        '.minecraft' if os.name == 'nt' else 'minecraft')
    parser.add_argument('--cache', type=Path, default=default_game / 'assets/blclient/objects')
    parser.add_argument('--output', type=Path, default=ROOT / 'exports/mesh-assets')
    args = parser.parse_args()
    prepare(args.cache, args.output)
