$ErrorActionPreference = 'Stop'
$previousJava = $env:JAVA_HOME
$previousGradle = $env:GRADLE_USER_HOME
$previousSmoke = $env:BCP_SMOKE_TEST
try {
    $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-user'
    $env:BCP_SMOKE_TEST = $null
    $capes = Join-Path (Split-Path $PSScriptRoot) 'exports\local-assets\capes'
    $destination = Join-Path $PSScriptRoot 'run\config\badlion-cosmetic-port\capes'
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    & robocopy.exe $capes $destination /E /MT:8 /R:1 /W:1 /NFL /NDL /NJH /NJS /NP
    if ($LASTEXITCODE -ge 8) { throw 'Cape copy failed' }
    Push-Location $PSScriptRoot
    try {
        .\gradlew.bat --no-daemon runClient
        if ($LASTEXITCODE) { throw 'Development client failed' }
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:GRADLE_USER_HOME = $previousGradle
    $env:BCP_SMOKE_TEST = $previousSmoke
}
