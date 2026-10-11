$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot
Push-Location $project
try {
    $jdk = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin'
    $joml = (Get-ChildItem -LiteralPath 'mod/.gradle-user/caches/modules-2/files-2.1/org.joml/joml/1.10.5' -Recurse -Filter '*.jar' | Select-Object -First 1).FullName
    $classes = Join-Path $project 'work/native-motion-reference/classes'
    New-Item -ItemType Directory -Force -Path $classes | Out-Null
    $sources = (Get-ChildItem -LiteralPath 'tests/native-reference' -Recurse -Filter '*.java').FullName
    $models = (Get-ChildItem -LiteralPath 'shared/src/main/java/local/badlioncosmeticport/model' -Filter '*.java').FullName
    & "$jdk/javac.exe" -cp $joml -d $classes $sources $models tests/NativeMotionParity.java tests/NativeRibbonParity.java tests/WingJointParity.java
    if ($LASTEXITCODE) { throw 'Native differential fixtures did not compile' }
    foreach ($test in @('NativeMotionParity', 'NativeRibbonParity')) {
        $result = & "$jdk/java.exe" -Xmx512m -cp "$classes;$joml" $test exports/mesh-assets/models
        if ($LASTEXITCODE) { throw "$test failed" }
        $file = if ($test -eq 'NativeMotionParity') { 'exports/native-motion-parity.json' } else { 'exports/native-ribbon-parity.json' }
        $result | Set-Content -LiteralPath $file -Encoding utf8
        Write-Output $result
    }
    $wing = & "$jdk/java.exe" -cp "$classes;$joml" local.badlioncosmeticport.model.WingJointParity exports/mesh-assets/models
    if ($LASTEXITCODE) { throw 'Wing child-local parity failed' }
    $wing | Set-Content -LiteralPath 'exports/wing-joint-parity.json' -Encoding utf8
    Write-Output $wing
} finally { Pop-Location }

