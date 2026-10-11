param([switch]$Setup)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot
$jdk = Get-ChildItem -LiteralPath (Join-Path $project 'work\forge-tools\jdk8') -Directory | Select-Object -First 1
if (!$jdk -or !(Test-Path -LiteralPath (Join-Path $jdk.FullName 'bin\javac.exe'))) {
    throw 'Project-local Java 8 JDK is missing. See forge-1.8.9/README.md.'
}
$previousJava = $env:JAVA_HOME
$previousCache = $env:GRADLE_USER_HOME
try {
    $env:JAVA_HOME = $jdk.FullName
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-user'
    Push-Location $PSScriptRoot
    try {
        if ($Setup) {
            & .\gradlew.bat setupDecompWorkspace --no-daemon
            if ($LASTEXITCODE) { throw 'Forge workspace setup failed' }
        }
        & .\gradlew.bat build --no-daemon
        if ($LASTEXITCODE) { throw 'Forge build failed' }
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:GRADLE_USER_HOME = $previousCache
}
