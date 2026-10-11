# Launch the remapped production JAR with an offline audit identity, without
# reading account files or launching through the development classpath.
param([switch]$Play)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot
$launcher = Join-Path $env:APPDATA '.minecraft'
$instance = Join-Path $project 'instances\fabric-1.21.1'
$minecraft = Get-Content -LiteralPath (Join-Path $launcher 'versions\1.21.1\1.21.1.json') -Raw | ConvertFrom-Json
$fabric = Get-Content -LiteralPath (Join-Path $launcher 'versions\fabric-loader-0.16.14-1.21.1\fabric-loader-0.16.14-1.21.1.json') -Raw | ConvertFrom-Json
$classpath = [Collections.Generic.List[string]]::new()
$cache = Join-Path $project 'work\production-runtime\libraries'
New-Item -ItemType Directory -Force -Path $cache | Out-Null
foreach ($library in $minecraft.libraries) {
    $allowed = $true
    if ($library.rules) {
        $allowed = $false
        foreach ($rule in $library.rules) {
            if ($rule.os.name -and $rule.os.name -ne 'windows') { continue }
            if ($rule.os.arch -and $rule.os.arch -ne 'x86_64') { continue }
            $allowed = $rule.action -eq 'allow'
        }
    }
    if (!$allowed -or !$library.downloads.artifact) { continue }
    $artifact = $library.downloads.artifact
    $path = Join-Path (Join-Path $launcher 'libraries') $artifact.path
    if (!(Test-Path -LiteralPath $path)) {
        $path = Join-Path $cache $artifact.path
        New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
        if (!(Test-Path -LiteralPath $path)) { Invoke-WebRequest -Uri $artifact.url -OutFile $path }
    }
    if ((Get-FileHash -LiteralPath $path -Algorithm SHA1).Hash.ToLower() -ne $artifact.sha1) {
        throw "Minecraft library checksum mismatch: $($library.name)"
    }
    $classpath.Add($path)
}
foreach ($library in $fabric.libraries) {
    $parts = $library.name.Split(':')
    $relative = $parts[0].Replace('.','/') + '/' + $parts[1] + '/' + $parts[2] + '/' + $parts[1] + '-' + $parts[2] + '.jar'
    $path = Join-Path (Join-Path $launcher 'libraries') $relative
    if (!(Test-Path -LiteralPath $path)) {
        $path = Join-Path $cache $relative
        New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
        $url = $library.url + $relative
        if (!(Test-Path -LiteralPath $path)) { Invoke-WebRequest -Uri $url -OutFile $path }
        $sha1 = ((Invoke-WebRequest -Uri ($url + '.sha1')).Content.Trim().Split(' ')[0]).ToLower()
        if ((Get-FileHash -LiteralPath $path -Algorithm SHA1).Hash.ToLower() -ne $sha1) {
            throw "Fabric library checksum mismatch: $($library.name)"
        }
    }
    $classpath.Add($path)
}
$minecraftJar = Join-Path $launcher 'versions\1.21.1\1.21.1.jar'
if ((Get-FileHash -LiteralPath $minecraftJar -Algorithm SHA1).Hash.ToLower() -ne $minecraft.downloads.client.sha1) {
    throw 'Minecraft client checksum mismatch'
}
$classpath.Add($minecraftJar)
$java = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe'
$nativeDirectory = Join-Path $project 'mod\.gradle\loom-cache\natives\1.21.1'
$assetDirectory = Join-Path $project 'mod\.gradle-user\caches\fabric-loom\assets'
$index = Join-Path $assetDirectory ('indexes\' + $minecraft.assetIndex.id + '.json')
if (!(Test-Path -LiteralPath $index)) {
    $loomIndex = Join-Path $assetDirectory ('indexes\1.21.1-' + $minecraft.assetIndex.id + '.json')
    if (!(Test-Path -LiteralPath $loomIndex)) { throw 'Minecraft asset index not found in the build cache' }
    Copy-Item -LiteralPath $loomIndex -Destination $index
}
$selectionPath = Join-Path $instance 'config\badlion-cosmetic-port\selection.json'
$savedSelection = if (Test-Path -LiteralPath $selectionPath) { [IO.File]::ReadAllText($selectionPath) } else { '{"capeId":1438}' }
if (!$Play) { [IO.File]::WriteAllText($selectionPath, '{"capeId":-1}') }
$previous = $env:BCP_SMOKE_TEST
try {
    $env:BCP_SMOKE_TEST = if ($Play) { $null } else { '1' }
    $arguments = @('-Xmx2G', "-Djava.library.path=$nativeDirectory", "-Dorg.lwjgl.librarypath=$nativeDirectory",
        '-cp', ($classpath -join ';'), $fabric.mainClass, '--username', 'PortAudit',
        '--uuid', '00000000000040008000000000000001', '--accessToken', '0', '--userType', 'legacy',
        '--version', $fabric.id, '--gameDir', $instance, '--assetsDir', $assetDirectory,
        '--assetIndex', $minecraft.assetIndex.id, '--width', '960', '--height', '540')
    if ($Play) {
        $argumentFile = Join-Path $project 'work\port-preview-java.args'
        $lines = foreach ($argument in $arguments) { '"' + $argument.Replace('\','\\').Replace('"','\"') + '"' }
        [IO.File]::WriteAllLines($argumentFile, $lines, [Text.UTF8Encoding]::new($false))
        $game = Start-Process -FilePath ($java.Replace('java.exe','javaw.exe')) -ArgumentList ('"@' + $argumentFile + '"') -WorkingDirectory $instance -PassThru
        Write-Output "Started offline Cosmetic Port preview, PID $($game.Id). The game stays open."
        return
    }
    Push-Location $instance
    try {
        & $java @arguments
        if ($LASTEXITCODE) { throw "Production client exited with $LASTEXITCODE" }
    } finally { Pop-Location }
    $log = Get-Content -LiteralPath (Join-Path $instance 'logs\latest.log') -Raw
    if ($log -notmatch 'BCP_SMOKE_PASS capePacks=1405' -or
        $log -match 'Shutdown failure|InjectionError|Mixin apply failed|Cape initialization failed|Cape texture loading failed') {
        throw 'Production JAR smoke check failed'
    }
    $report = [pscustomobject]@{
        version = '0.2.2'; runtime = 'production Fabric Loader with obfuscated vanilla client'
        capePacks = 1405; startup = 'passed'; mixinTargetLoaded = $true; selector = 'opened'
        textureRegistration = 'passed'; selectionChange = 'passed'; cleanShutdown = 'passed'
        identity = 'offline audit player; no account credentials read'
        inWorldAppearance = 'unverified'; multiplayerSynchronization = 'not implemented'
        modSha256 = (Get-FileHash -LiteralPath (Join-Path $instance 'mods\badlion-cosmetic-port-0.2.2.jar')).Hash.ToLower()
        testedAt = [DateTime]::UtcNow.ToString('o')
    }
    $report | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $project 'exports\port-validation.json') -Encoding UTF8
    $report | ConvertTo-Json
} finally {
    $env:BCP_SMOKE_TEST = $previous
    if (!$Play) { [IO.File]::WriteAllText($selectionPath, $savedSelection) }
}
