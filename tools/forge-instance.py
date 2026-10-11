"""Prepare and launch an isolated, production Forge 1.8.9 instance.

Uses public version manifests and an offline audit identity; never reads account tokens.
"""
from pathlib import Path
import argparse,hashlib,json,os,re,shutil,subprocess,zipfile,urllib.request,urllib.error

ROOT=Path(__file__).resolve().parents[1]
MC=Path(os.environ['APPDATA'])/'.minecraft'
INSTANCE=ROOT/'instances/forge-1.8.9-mesh'
CACHE=ROOT/'work/forge-runtime'
VERSION='0.4.0'

def fetch(url,target,sha1=None):
    target.parent.mkdir(parents=True,exist_ok=True)
    if not target.is_file():
        temporary=target.with_suffix(target.suffix+'.download')
        with urllib.request.urlopen(url,timeout=60) as incoming,temporary.open('wb') as out:
            shutil.copyfileobj(incoming,out)
        temporary.replace(target)
    actual=hashlib.sha1(target.read_bytes()).hexdigest()
    if sha1 and actual!=sha1:raise RuntimeError('Checksum mismatch: '+str(target))
    return target

def allowed(lib):
    result=not bool(lib.get('rules'))
    for rule in lib.get('rules',[]):
        platform=rule.get('os',{})
        if platform.get('name','windows')!='windows':continue
        if platform.get('arch','x86_64') not in ('x86_64','amd64'):continue
        result=rule['action']=='allow'
    return result

def coordinate(name):
    group,artifact,version,*classifier=name.split(':')
    suffix='-'+classifier[0] if classifier else ''
    return f'{group.replace(".","/")}/{artifact}/{version}/{artifact}-{version}{suffix}.jar'

def prepare(register):
    INSTANCE.mkdir(parents=True,exist_ok=True);CACHE.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(ROOT/'work/forge-tools/forge-installer.jar') as installer:
        profile=json.loads(installer.read('install_profile.json'))
        forge=profile['versionInfo'];relative=coordinate(profile['install']['path'])
        jar=MC/'libraries'/relative
        jar.parent.mkdir(parents=True,exist_ok=True)
        data=installer.read(profile['install']['filePath'])
        if hashlib.sha1(data).hexdigest()!='beda619c465af293e63952dd573c137c17c0a4cd':raise RuntimeError('Forge checksum mismatch')
        jar.write_bytes(data)
    version_dir=MC/'versions'/forge['id'];version_dir.mkdir(parents=True,exist_ok=True)
    (version_dir/(forge['id']+'.json')).write_text(json.dumps(forge,indent=2))
    base=json.loads((MC/'versions/1.8.9/1.8.9.json').read_text())
    classpath=[]
    natives=CACHE/'natives';natives.mkdir(exist_ok=True)
    for lib in base['libraries']+forge['libraries']:
        if not allowed(lib):continue
        if lib.get('clientreq') is False:continue
        # Forge's retired org.scala-lang XML artifact was a repack. Use the
        # original upstream module coordinate and its own published checksum.
        if lib['name']=='org.scala-lang:scala-xml_2.11:1.0.2':
            lib['name']='org.scala-lang.modules:scala-xml_2.11:1.0.2'
            lib['url']='https://repo.maven.apache.org/maven2/'
            with urllib.request.urlopen(lib['url']+coordinate(lib['name'])+'.sha1',timeout=60) as response:
                lib['checksums']=[response.read().decode().strip().split()[0]]
        print('Resolving '+lib['name'],flush=True)
        classifier=lib.get('natives',{}).get('windows')
        if classifier:
            classifier=classifier.replace('${arch}','64')
            native=lib['downloads']['classifiers'][classifier]
            native_jar=fetch(native['url'],MC/'libraries'/native['path'],native['sha1'])
            with zipfile.ZipFile(native_jar) as archive:
                for name in archive.namelist():
                    if name.lower().endswith('.dll'):
                        (natives/Path(name).name).write_bytes(archive.read(name))
        artifact=lib.get('downloads',{}).get('artifact')
        if lib.get('downloads') and artifact is None:continue
        relative=artifact['path'] if artifact else coordinate(lib['name'])
        path=MC/'libraries'/relative
        url=artifact['url'] if artifact else lib.get('url','https://libraries.minecraft.net/')+relative
        expected=artifact.get('sha1') if artifact else None
        if not expected:
            values=lib.get('checksums')
            if values:expected=values[0]
            elif lib['name'].startswith('net.minecraftforge:forge:'):expected='beda619c465af293e63952dd573c137c17c0a4cd'
            else:
                try:
                    with urllib.request.urlopen(url+'.sha1',timeout=60) as response:expected=response.read().decode().strip().split()[0]
                except urllib.error.HTTPError:
                    url='https://repo.maven.apache.org/maven2/'+relative
                    with urllib.request.urlopen(url+'.sha1',timeout=60) as response:expected=response.read().decode().strip().split()[0]
        if not path.exists() and expected:
            candidates=(ROOT/'forge-1.8.9/.gradle-user/caches/modules-2/files-2.1').rglob(Path(relative).name)
            for candidate in candidates:
                if hashlib.sha1(candidate.read_bytes()).hexdigest()==expected:
                    path.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(candidate,path);break
        try:fetch(url,path,expected)
        except urllib.error.HTTPError:
            central=relative.replace('org/scala-lang/scala-xml_', 'org/scala-lang/modules/scala-xml_').replace('org/scala-lang/scala-swing_', 'org/scala-lang/modules/scala-swing_').replace('org/scala-lang/scala-parser-combinators_', 'org/scala-lang/modules/scala-parser-combinators_')
            fetch('https://repo.maven.apache.org/maven2/'+central,path,expected)
        classpath.append(str(path))
        classifier=lib.get('natives',{}).get('windows')
        if classifier:
            classifier=classifier.replace('${arch}','64')
            native=lib['downloads']['classifiers'][classifier]
            native_jar=fetch(native['url'],MC/'libraries'/native['path'],native['sha1'])
            with zipfile.ZipFile(native_jar) as archive:
                for name in archive.namelist():
                    if name.lower().endswith('.dll'):
                        (natives/Path(name).name).write_bytes(archive.read(name))
    client=fetch(base['downloads']['client']['url'],MC/'versions/1.8.9/1.8.9.jar',base['downloads']['client']['sha1'])
    classpath.append(str(client))
    (version_dir/(forge['id']+'.json')).write_text(json.dumps(forge,indent=2))
    assets=ROOT/'forge-1.8.9/.gradle-user/caches/minecraft/assets'
    fetch(base['assetIndex']['url'],assets/'indexes'/f"{base['assetIndex']['id']}.json",base['assetIndex']['sha1'])
    index=json.loads((assets/'indexes'/f"{base['assetIndex']['id']}.json").read_text())
    for entry in index['objects'].values():
        digest=entry['hash'];fetch('https://resources.download.minecraft.net/'+digest[:2]+'/'+digest,assets/'objects'/digest[:2]/digest,digest)
    config=INSTANCE/'config/badlion-cosmetic-port';config.mkdir(parents=True,exist_ok=True)
    asset_link=config/'mesh-assets'
    if not asset_link.exists():
        subprocess.run(['powershell','-NoProfile','-Command',"New-Item -ItemType Junction -Path '"+str(asset_link).replace("'","''")+"' -Target '"+str(ROOT/'exports/mesh-assets').replace("'","''")+"' | Out-Null"],check=True)
    if asset_link.resolve()!=(ROOT/'exports/mesh-assets').resolve():raise RuntimeError('Unexpected asset junction')
    selection=config/'mesh-selection.json'
    if not selection.exists():shutil.copy2(ROOT/'instances/fabric-1.21.1-mesh/config/badlion-cosmetic-port/mesh-selection.json',selection)
    mods=INSTANCE/'mods';mods.mkdir(exist_ok=True)
    source=ROOT/f'forge-1.8.9/build/libs/badlion-cosmetic-port-forge-{VERSION}.jar'
    installed=mods/source.name
    for old in mods.glob('badlion-cosmetic-port-forge-*.jar'):
        if old.name!=source.name:raise RuntimeError('Older mod preserved: '+str(old))
    shutil.copy2(source,installed)
    options=INSTANCE/'options.txt'
    if not options.exists():options.write_text('guiScale:2\nrenderDistance:6\nmaxFps:120\npauseOnLostFocus:false\nthirdPersonView:1\n')
    jdk=next((ROOT/'work/forge-tools/jdk8').glob('jdk*'))
    if register:
        path=MC/'launcher_profiles.json';profiles=json.loads(path.read_text())
        profiles.setdefault('profiles',{})['cosmetic-port-forge-189']={
            'name':'Cosmetic Port 0.4.0 - Forge 1.8.9','type':'custom','lastVersionId':forge['id'],
            'gameDir':str(INSTANCE),'javaDir':str(jdk/'bin/javaw.exe'),'javaArgs':'-Xmx2G -Dlog4j2.formatMsgNoLookups=true'}
        backup=ROOT/'work/forge-tools/launcher-profiles-before-forge.json'
        if not backup.exists():shutil.copy2(path,backup)
        pending=path.with_suffix('.json.tmp');pending.write_text(json.dumps(profiles,indent=2));pending.replace(path)
    runtime={'java':str(jdk/'bin/java.exe'),'classpath':classpath,'natives':str(natives),'assets':str(assets),'assetIndex':base['assetIndex']['id'],'forgeId':forge['id']}
    (CACHE/'runtime.json').write_text(json.dumps(runtime,indent=2))
    print('Prepared Forge 1.8.9 instance; production mod SHA256 '+hashlib.sha256(installed.read_bytes()).hexdigest())

def launch(audit,write_preview=False):
    runtime=json.loads((CACHE/'runtime.json').read_text())
    args=[runtime['java'] if audit else runtime['java'].replace('java.exe','javaw.exe'),'-Xmx2G','-Dlog4j2.formatMsgNoLookups=true',
          '-Djava.library.path='+runtime['natives'],'-cp',';'.join(runtime['classpath']),'net.minecraft.launchwrapper.Launch',
          '--username','PortAudit','--uuid','00000000000040008000000000000001','--accessToken','0','--userType','legacy',
          '--version',runtime['forgeId'],'--gameDir',str(INSTANCE),'--assetsDir',runtime['assets'],'--assetIndex',runtime['assetIndex'],
          '--userProperties','{}','--tweakClass','net.minecraftforge.fml.common.launcher.FMLTweaker','--width','1280','--height','800']
    env=os.environ.copy();env['BCP_FORGE_AUDIT']='1' if audit else '0';env['BCP_FORGE_PLAY']='0' if audit else '1'
    if write_preview:
        (CACHE/'preview-command.json').write_text(json.dumps({'java':args[0],'arguments':args[1:],'cwd':str(INSTANCE)}))
        return
    selection=INSTANCE/'config/badlion-cosmetic-port/mesh-selection.json';saved=selection.read_bytes()
    with (CACHE/('audit-console.log' if audit else 'preview-console.log')).open('w') as log:
        child=subprocess.Popen(args,cwd=INSTANCE,env=env,stdout=log,stderr=log,creationflags=subprocess.CREATE_NO_WINDOW)
    if not audit:print('Forge 1.8.9 preview PID '+str(child.pid));return
    try:
        code=child.wait(timeout=150)
        latest=(INSTANCE/'logs/latest.log').read_text(errors='replace')
        console=(CACHE/'audit-console.log').read_text(errors='replace')
        (ROOT/'exports/forge-0.4.0-production.log').write_text(console)
        # Legacy Forge's default file appender filters this mod's logger. The
        # captured console contains the explicit test markers and renderer errors.
        combined=console+'\n'+latest
        if code or 'BCP_FORGE_AUDIT PASS' not in console or any(text in combined for text in ['Mesh rendering failed','Mesh catalog initialization failed','Legacy cosmetic shader failed','Cannot equip','Cannot load particle sprite']):
            raise RuntimeError(f'Forge production audit failed (exit {code}); see console and game logs')
        report={'version':VERSION,'minecraft':'1.8.9','forge':'11.15.1.2318','productionAudit':'passed','offlineIdentity':True,
                'modSha256':hashlib.sha256((INSTANCE/f'mods/badlion-cosmetic-port-forge-{VERSION}.jar').read_bytes()).hexdigest(),
                'families':{name:{'draws':int(draws),'textureChanges':int(textures)} for name,draws,textures in re.findall(r'BCP_FORGE_DRAW (\w+) draws=(\d+) textures=(\d+)',console)},
                'effects':dict((key,int(value)) for key,value in re.findall(r'(shaders|cloakSpawned|cloakDrawn|fire|sonic)=(\d+)',console)),
                'frameCycle':re.findall(r'BCP_FORGE_FRAME_CYCLE (\S+)',console),
                'airPet':re.findall(r'BCP_FORGE_PET_AIR joints=(\d+) draws=(\d+)',console),
                'multiplayerSynchronization':'not implemented'}
        (ROOT/'exports/forge-0.4.0-validation.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
    finally:
        if child.poll() is None:child.terminate();child.wait(timeout=15)
        selection.write_bytes(saved)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--prepare',action='store_true');parser.add_argument('--register',action='store_true');parser.add_argument('--audit',action='store_true');parser.add_argument('--play',action='store_true');parser.add_argument('--write-preview',action='store_true');args=parser.parse_args()
    if args.prepare:prepare(args.register)
    if args.audit:launch(True)
    if args.play:launch(False)
    if args.write_preview:launch(False,True)
