param([switch]$RegisterLauncher)
$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$instance = Join-Path $project 'instances\fabric-1.21.1-mesh'
$version = '0.3.6'
$source = Join-Path $project 'exports\mesh-assets'
$jar = Join-Path $project "mod\build\libs\badlion-cosmetic-port-$version.jar"
$mods = Join-Path $instance 'mods'
$assets = Join-Path $instance 'config\badlion-cosmetic-port\mesh-assets'
if (!(Test-Path -LiteralPath (Join-Path $source 'catalog.json')) -or !(Test-Path -LiteralPath $jar)) { throw 'Build and prepare mesh assets first.' }
New-Item -ItemType Directory -Force $mods,(Split-Path $assets) | Out-Null
if (!(Test-Path -LiteralPath $assets)) { New-Item -ItemType Junction -Path $assets -Target $source | Out-Null }
else {
    $item = Get-Item -LiteralPath $assets
    if ($item.LinkType -ne 'Junction' -or [IO.Path]::GetFullPath($item.Target) -ne [IO.Path]::GetFullPath($source)) {
        throw 'The instance asset directory already contains other files; preserved it.'
    }
}
$api = Get-ChildItem -LiteralPath (Join-Path $project 'mod\.gradle-user\caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-api\0.116.6+1.21.1') -Filter '*.jar' -File -Recurse | Select-Object -First 1
if (!$api) { throw 'Fabric API build dependency is missing.' }
foreach ($old in Get-ChildItem -LiteralPath $mods -Filter 'badlion-cosmetic-port-*.jar' -File) {
    if ($old.Name -ne (Split-Path $jar -Leaf)) { throw "An older mod is still in the mesh instance: $($old.Name). Preserved it." }
}
Copy-Item -LiteralPath $jar -Destination $mods -Force
Copy-Item -LiteralPath $api.FullName -Destination $mods -Force
if ($RegisterLauncher) {
    if (Get-Process MinecraftLauncher -ErrorAction SilentlyContinue) { throw 'Close Minecraft Launcher before editing its profile; mesh instance is prepared.' }
    $profilesPath = Join-Path $env:APPDATA '.minecraft\launcher_profiles.json'
    $profiles = Get-Content -LiteralPath $profilesPath -Raw | ConvertFrom-Json
    $backup = Join-Path $project 'backups\launcher'
    New-Item -ItemType Directory -Force $backup | Out-Null
    Copy-Item -LiteralPath $profilesPath -Destination (Join-Path $backup ('mesh-profiles-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.json'))
    $profile = [pscustomobject]@{
        name = "Cosmetic Port $version"; type = 'custom'; lastVersionId = 'fabric-loader-0.16.14-1.21.1'
        gameDir = $instance; javaDir = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\javaw.exe'
        javaArgs = '-Xmx2G'; icon = 'Grass'; created = [DateTime]::UtcNow.ToString('o')
    }
    $profiles.profiles | Add-Member -MemberType NoteProperty -Name 'badlion-cosmetic-port-mesh-1.21.1' -Value $profile -Force
    $temporary = $profilesPath + '.mesh-port.tmp'
    $profiles | ConvertTo-Json -Depth 100 | Set-Content -LiteralPath $temporary -Encoding UTF8
    Move-Item -LiteralPath $temporary -Destination $profilesPath -Force
    Write-Output "Registered Minecraft Launcher profile: Cosmetic Port $version"
}
Write-Output "Cosmetic Port $version installed in the separate mesh instance. K opens its selector."
