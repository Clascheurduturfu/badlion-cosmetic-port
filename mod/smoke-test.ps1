$ErrorActionPreference = 'Stop'
$previousJava = $env:JAVA_HOME
$previousGradle = $env:GRADLE_USER_HOME
$previousSmoke = $env:BCP_SMOKE_TEST
try {
    $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-user'
    $env:BCP_SMOKE_TEST = '1'
    $capes = Join-Path (Split-Path $PSScriptRoot) 'exports\local-assets\capes'
    $expected = (Get-Content -LiteralPath (Join-Path (Split-Path $PSScriptRoot) 'exports\local-assets\cape-conversion-report.json') -Raw | ConvertFrom-Json).prepared
    $destination = Join-Path $PSScriptRoot 'run\config\badlion-cosmetic-port\capes'
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    & robocopy.exe $capes $destination /E /MT:8 /R:1 /W:1 /NFL /NDL /NJH /NJS /NP
    if ($LASTEXITCODE -ge 8) { throw 'Cape copy failed' }
    Push-Location $PSScriptRoot
    try {
        .\gradlew.bat --no-daemon runClient
        if ($LASTEXITCODE) { throw 'Development client startup failed' }
        $log = Get-Content -LiteralPath 'run\logs\latest.log' -Raw
        if ($log -notmatch "BCP_SMOKE_PASS capePacks=$expected\b") { throw 'Cape startup marker missing or pack count differs from the export' }
        if ($log -match 'Shutdown failure|InjectionError|Mixin apply failed|Cape initialization failed|Cape texture loading failed') {
            throw 'Client log contains a startup, mixin or shutdown failure'
        }
        Write-Output 'PASS isolated Fabric startup, searchable selector, animated texture registration and clean shutdown.'
        [pscustomobject]@{
            version = '0.2.2'; capePacks = $expected; startup = 'passed'; selector = 'opened'
            textureRegistration = 'passed'; selectionChange = 'passed'; cleanShutdown = 'passed'
            mixinTargetLoaded = $true; inWorldAppearance = 'unverified'; multiplayerSynchronization = 'not implemented'
            testedAt = [DateTime]::UtcNow.ToString('o')
        } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path (Split-Path $PSScriptRoot) 'exports\port-validation.json') -Encoding UTF8
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:GRADLE_USER_HOME = $previousGradle
    $env:BCP_SMOKE_TEST = $previousSmoke
}
