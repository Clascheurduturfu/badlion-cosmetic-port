param(
    [string]$GameDirectory = (Join-Path $PSScriptRoot 'instances\fabric-1.21.1'),
    [switch]$RegisterLauncher
)
$ErrorActionPreference = 'Stop'
$instance = [IO.Path]::GetFullPath($GameDirectory)
$jar = Join-Path $PSScriptRoot 'mod\build\libs\badlion-cosmetic-port-0.2.2.jar'
$apiCache = Join-Path $PSScriptRoot 'mod\.gradle-user\caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-api\0.116.6+1.21.1'
$api = Get-ChildItem -LiteralPath $apiCache -Recurse -File -Filter 'fabric-api-0.116.6+1.21.1.jar' | Select-Object -First 1
$capes = Join-Path $PSScriptRoot 'exports\local-assets\capes'
if (!(Test-Path -LiteralPath $jar) -or !$api -or !(Test-Path -LiteralPath $capes)) {
    throw 'Build the mod and export/prepare the local cape assets first.'
}
$mods = Join-Path $instance 'mods'
$config = Join-Path $instance 'config\badlion-cosmetic-port\capes'
New-Item -ItemType Directory -Force -Path $mods,$config | Out-Null
foreach ($old in @(Get-ChildItem -LiteralPath $mods -File -Filter 'badlion-cosmetic-port-*.jar')) {
    if ($old.Name -eq (Split-Path $jar -Leaf)) { continue }
    $previousBuild = Join-Path $PSScriptRoot ('mod\build\libs\' + $old.Name)
    if (!(Test-Path -LiteralPath $previousBuild) -or
        (Get-FileHash -LiteralPath $previousBuild).Hash -ne (Get-FileHash -LiteralPath $old.FullName).Hash) {
        throw "Preserved an unrecognized older port JAR: $($old.Name). Move it aside before installing."
    }
    Remove-Item -LiteralPath $old.FullName
}
Copy-Item -LiteralPath $jar -Destination $mods -Force
Copy-Item -LiteralPath $api.FullName -Destination $mods -Force
& robocopy.exe $capes $config /E /MT:8 /R:1 /W:1 /NFL /NDL /NJH /NJS /NP
if ($LASTEXITCODE -ge 8) { throw "Cape copy failed: robocopy exit $LASTEXITCODE" }

if ($RegisterLauncher) {
    if (Get-Process MinecraftLauncher -ErrorAction SilentlyContinue) {
        throw 'Close Minecraft Launcher before registering the separate profile. Instance files are ready.'
    }
    $launcherRoot = Join-Path $env:APPDATA '.minecraft'
    $profilesPath = Join-Path $launcherRoot 'launcher_profiles.json'
    if (!(Test-Path -LiteralPath $profilesPath)) { throw 'Minecraft Launcher profile file is missing.' }
    $versionId = 'fabric-loader-0.16.14-1.21.1'
    $versionDirectory = Join-Path $launcherRoot "versions\$versionId"
    New-Item -ItemType Directory -Force -Path $versionDirectory | Out-Null
    $versionJson = Invoke-RestMethod -Uri 'https://meta.fabricmc.net/v2/versions/loader/1.21.1/0.16.14/profile/json'
    if ($versionJson.id -ne $versionId -or $versionJson.inheritsFrom -ne '1.21.1') {
        throw 'Unexpected Fabric launcher metadata.'
    }
    $versionJson | ConvertTo-Json -Depth 100 | Set-Content -LiteralPath (Join-Path $versionDirectory "$versionId.json") -Encoding UTF8
    $profiles = Get-Content -LiteralPath $profilesPath -Raw | ConvertFrom-Json
    $backup = Join-Path $PSScriptRoot 'backups\launcher'
    New-Item -ItemType Directory -Force -Path $backup | Out-Null
    Copy-Item -LiteralPath $profilesPath -Destination (Join-Path $backup ('launcher_profiles-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.json'))
    $profile = [pscustomobject]@{
        name = 'Cosmetic Port 0.2.2'; type = 'custom'; lastVersionId = $versionId
        gameDir = $instance; javaDir = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\javaw.exe'
        javaArgs = '-Xmx3G'; icon = 'Grass'; created = [DateTime]::UtcNow.ToString('o')
    }
    $profiles.profiles | Add-Member -MemberType NoteProperty -Name 'badlion-cosmetic-port-1.21.1' -Value $profile -Force
    $temporary = $profilesPath + '.cosmetic-port.tmp'
    $profiles | ConvertTo-Json -Depth 100 | Set-Content -LiteralPath $temporary -Encoding UTF8
    Move-Item -LiteralPath $temporary -Destination $profilesPath -Force
    Write-Output 'Registered Minecraft Launcher profile: Cosmetic Port 0.2.2'
}
$report = [pscustomobject]@{
    version = '0.2.2'; minecraft = '1.21.1'; fabricLoader = '0.16.14'; instance = $instance
    capePacks = @(Get-ChildItem -LiteralPath $config -Recurse -File -Filter cape.json).Count
    ownershipFilter = $false; launcherRegistered = [bool]$RegisterLauncher
    modSha256 = (Get-FileHash -LiteralPath (Join-Path $mods (Split-Path $jar -Leaf))).Hash.ToLower()
}
$report | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'exports\port-installation.json') -Encoding UTF8
$report | ConvertTo-Json
