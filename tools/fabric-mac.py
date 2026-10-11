"""Run a checksum-verified, isolated production Fabric preview/audit on macOS."""
import argparse
import concurrent.futures
import json
import os
import platform
import re
import shutil
import subprocess
import zipfile
from pathlib import Path
import importlib.util
spec = importlib.util.spec_from_file_location('forge_support', Path(__file__).with_name('forge-mac.py'))
support = importlib.util.module_from_spec(spec)
spec.loader.exec_module(support)
ROOT = Path(__file__).resolve().parents[1]


def allowed(lib, arch):
    result = not bool(lib.get('rules'))
    for rule in lib.get('rules', []):
        system = rule.get('os', {})
        if system.get('name', 'osx') != 'osx':
            continue
        if 'arch' in system and not re.search(system['arch'], arch):
            continue
        if 'version' in system and not re.search(system['version'], platform.mac_ver()[0]):
            continue
        result = rule['action'] == 'allow'
    return result


def prepare(mc, java):
    cache = ROOT / ('work/fabric-' + mc + '-mac')
    game = ROOT / ('instances/fabric-' + mc + '-mac')
    cache.mkdir(parents=True, exist_ok=True)
    game.mkdir(parents=True, exist_ok=True)
    support.CACHE = cache
    settings = subprocess.run([str(java), '-XshowSettings:properties', '-version'], capture_output=True, text=True, check=True).stderr
    arch = 'arm64' if 'os.arch = aarch64' in settings else 'x86_64'
    with support.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json') as stream:
        record = next(v for v in json.load(stream)['versions'] if v['id'] == mc)
    base = json.loads(support.fetch(record['url'], cache / (mc + '.json'), record['sha1']).read_text())
    loader_url = 'https://meta.fabricmc.net/v2/versions/loader/' + mc + '/0.16.14/profile/json'
    with support.urlopen(loader_url) as stream:
        loader = json.load(stream)
    (cache / 'loader-profile.json').write_text(json.dumps(loader, indent=2))
    libraries = {}
    for lib in base['libraries'] + loader['libraries']:
        if allowed(lib, arch):
            libraries[lib['name']] = lib
    classpath = []
    natives = cache / 'natives'
    natives.mkdir(exist_ok=True)
    for lib in libraries.values():
        jar = support.library(lib)
        if jar:
            classpath.append(str(jar))
            if 'natives-macos' in lib['name']:
                with zipfile.ZipFile(jar) as archive:
                    for name in archive.namelist():
                        if name.endswith(('.dylib', '.jnilib')):
                            (natives / Path(name).name).write_bytes(archive.read(name))
        if lib.get('natives', {}).get('osx'):
            native = lib['downloads']['classifiers'][lib['natives']['osx']]
            jar = support.fetch(native['url'], cache / 'libraries' / native['path'], native['sha1'],
                                reuse=support.EXISTING / 'libraries' / native['path'])
            with zipfile.ZipFile(jar) as archive:
                for name in archive.namelist():
                    if name.endswith(('.dylib', '.jnilib')):
                        (natives / Path(name).name).write_bytes(archive.read(name))
    client = base['downloads']['client']
    classpath.append(str(support.fetch(client['url'], cache / (mc + '.jar'), client['sha1'],
                                     reuse=support.EXISTING / 'versions' / mc / (mc + '.jar'))))
    assets = cache / 'assets'
    index = base['assetIndex']
    index_path = support.fetch(index['url'], assets / 'indexes' / (index['id'] + '.json'), index['sha1'],
                               reuse=support.EXISTING / 'assets/indexes' / (index['id'] + '.json'))
    hashes = sorted({v['hash'] for v in json.loads(index_path.read_text())['objects'].values()})
    def asset(digest):
        relative = 'objects/' + digest[:2] + '/' + digest
        support.fetch('https://resources.download.minecraft.net/' + digest[:2] + '/' + digest,
                      assets / relative, digest, reuse=support.EXISTING / 'assets' / relative)
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        list(pool.map(asset, hashes))
    logging = base['logging']['client']
    log_file = logging['file']
    logging_path = support.fetch(log_file['url'], cache / log_file['id'], log_file['sha1'])
    subprocess.run([shutil.which('python3'), str(ROOT / 'tools/install-portable.py'), '--target',
                    'fabric-' + mc, '--game-dir', str(game)], check=True)
    api_version = '0.92.6+1.20.1' if mc == '1.20.1' else '0.116.6+1.21.1'
    api_url = 'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/' + api_version + '/fabric-api-' + api_version + '.jar'
    with support.urlopen(api_url + '.sha1') as stream:
        checksum = stream.read().decode().strip().split()[0]
    support.fetch(api_url, game / 'mods' / ('fabric-api-' + api_version + '.jar'), checksum)
    if not (game / 'options.txt').exists():
        (game / 'options.txt').write_text('guiScale:2\nrenderDistance:6\nmaxFps:120\npauseOnLostFocus:false\n')
    runtime = dict(java=str(java), classpath=classpath, natives=str(natives), assets=str(assets),
                   assetIndex=index['id'], mainClass=loader['mainClass'], javaArchitecture=arch,
                   loggingArgument=logging['argument'].replace('${path}', logging_path.as_uri()))
    (cache / 'runtime.json').write_text(json.dumps(runtime, indent=2))
    print('Prepared ' + str(game), flush=True)


def launch(mc, audit, timeout):
    cache = ROOT / ('work/fabric-' + mc + '-mac')
    game = ROOT / ('instances/fabric-' + mc + '-mac')
    runtime = json.loads((cache / 'runtime.json').read_text())
    artifact = next(a for a in json.loads((ROOT / 'release/manifest.json').read_text())['artifacts']
                    if a['target'] == 'fabric-' + mc)
    mod_sha256 = support.digest(game / 'mods' / artifact['file'], 'sha256')
    if mod_sha256 != artifact['sha256']:
        raise RuntimeError('Installed mod differs from the release manifest; reinstall before launching')
    selection = game / 'config/badlion-cosmetic-port/mesh-selection.json'
    saved = selection.read_bytes() if selection.exists() else None
    if audit:
        world = game / 'saves/Mesh Audit 0.3.3 Verification'
        if not world.exists():
            source = ROOT / 'instances/forge-1.12.2-mac/saves/Cosmetic Port Forge Verification'
            if not source.exists():
                raise RuntimeError('Complete the isolated Forge 1.12.2 audit first to create a disposable test world')
            shutil.copytree(source, world, ignore=shutil.ignore_patterns('session.lock'))
    args = [runtime['java'], '-XstartOnFirstThread', '-Xmx2G', runtime['loggingArgument'],
            '-Djava.library.path=' + runtime['natives'], '-cp', os.pathsep.join(runtime['classpath']),
            runtime['mainClass'], '--username', 'PortAudit', '--uuid', '00000000000040008000000000000001',
            '--accessToken', '0', '--userType', 'legacy', '--version', mc, '--gameDir', str(game),
            '--assetsDir', runtime['assets'], '--assetIndex', runtime['assetIndex'], '--width', '1280', '--height', '800']
    env = os.environ.copy()
    env['BCP_SMOKE_WORLD'] = '1' if audit else '0'
    env['BCP_SMOKE_TEST'] = '0'
    log_path = cache / ('audit-console.log' if audit else 'preview-console.log')
    with log_path.open('w') as log:
        child = subprocess.Popen(args, cwd=game, env=env, stdout=log, stderr=log, stdin=subprocess.DEVNULL, start_new_session=not audit)
    print('PID ' + str(child.pid) + '; log ' + str(log_path), flush=True)
    if not audit:
        return
    try:
        code = child.wait(timeout=timeout)
        console = log_path.read_text(errors='replace')
        families = {name: dict(draws=int(draws), textureChanges=int(frames)) for name, draws, frames in
                    re.findall(r'BCP_RENDER_AUDIT (\w+) draws=(\d+) textureChanges=(\d+)', console)}
        weapon = re.search(r'BCP_WEAPON_DRAW swords=(\d+) bows=(\d+) gui=(\d+) pulls=(\d+)', console)
        views = re.search(r"BCP_WEAPON_VIEWS first=(\d+) third=(\d+)",console)
        passed = bool(views) and all(int(v)>0 for v in views.groups()) and "BCP_WEAPON_ASSETS PASS packs=62" in console and code == 0 and 'BCP_WORLD_AUDIT PASS' in console and len(families) == 6 and weapon is not None and all(int(v) > 0 for v in weapon.groups())
        report = dict(minecraft=mc, version='0.5.0', javaArchitecture=runtime['javaArchitecture'], exitCode=code,
                      modSha256=mod_sha256,
                      families=families, weaponViews=dict(zip(("firstPerson","thirdPerson"),map(int,views.groups()))) if views else {}, weapons=dict(zip(('swords','bows','gui','pulls'), map(int, weapon.groups()))) if weapon else {},
                      productionAudit='passed' if passed else 'failed', consoleSha256=support.digest(log_path,'sha256'))
        (ROOT / ('exports/fabric-' + mc + '-0.5.0-mac-validation.json')).write_text(json.dumps(report, indent=2))
        print(json.dumps(report, indent=2))
        if not passed:
            raise RuntimeError('Production audit failed; see ' + str(log_path))
    finally:
        if child.poll() is None:
            child.terminate()
            try:
                child.wait(timeout=15)
            except subprocess.TimeoutExpired:
                child.kill(); child.wait()
        if saved is None:
            selection.unlink(missing_ok=True)
        else:
            selection.write_bytes(saved)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft', choices=('1.20.1','1.21.1'), default='1.20.1')
    parser.add_argument('--prepare', action='store_true')
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--audit', action='store_true')
    mode.add_argument('--play', action='store_true')
    parser.add_argument('--java', type=Path)
    parser.add_argument('--timeout', type=int, default=300)
    args = parser.parse_args()
    if platform.system() != 'Darwin':
        parser.error('This launcher helper targets macOS; the mods themselves are portable')
    if args.prepare:
        if args.java:
            java = args.java
        else:
            candidates = list((ROOT/'work/toolchains').glob('*jdk21*/Contents/Home/bin/java')) if args.minecraft=='1.21.1' else list(
                (Path.home()/'Library/Application Support/Badlion Client/Data').glob('zulu-17*.jre/Contents/Home/bin/java'))
            if not candidates:
                parser.error('Provide --java pointing to Java 17 (1.20.1) or Java 21 (1.21.1)')
            java = sorted(candidates)[-1]
        prepare(args.minecraft, java)
    if args.audit or args.play:
        launch(args.minecraft, args.audit, args.timeout)
    elif not args.prepare:
        parser.error('Choose --prepare, --audit or --play')
