$ErrorActionPreference = 'Stop'
$jdkPath = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
$previousJava = $env:JAVA_HOME
$previousGradle = $env:GRADLE_USER_HOME
try {
    $env:JAVA_HOME = $jdkPath
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-user'
    Push-Location $PSScriptRoot
    try {
        .\gradlew.bat --no-daemon build
        if ($LASTEXITCODE) { throw 'Fabric mod build failed' }
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:GRADLE_USER_HOME = $previousGradle
}
