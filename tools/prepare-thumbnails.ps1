param([string]$Category)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot)
try {
    $jdk = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot/bin'
    $joml = (Get-ChildItem -LiteralPath 'mod/.gradle-user/caches/modules-2/files-2.1/org.joml/joml/1.10.5' -Recurse -Filter '*.jar' | Select-Object -First 1).FullName
    $gson = (Get-ChildItem -LiteralPath 'mod/.gradle-user/caches/modules-2/files-2.1/com.google.code.gson/gson/2.10.1' -Recurse -Filter '*.jar' | Select-Object -First 1).FullName
    New-Item -ItemType Directory -Force -Path 'work/thumbnail-classes' | Out-Null
    $models=(Get-ChildItem 'mod/src/main/java/local/badlioncosmeticport/model' -Filter '*.java').FullName
    & "$jdk/javac.exe" -cp "$joml;$gson" -d work/thumbnail-classes $models mod/src/main/java/local/badlioncosmeticport/render/ObjGeometry.java tools/PrepareThumbnails.java
    if($LASTEXITCODE){throw 'Thumbnail compiler failed'}
    $arguments = @('exports/mesh-assets')
    if ($Category) { $arguments += $Category }
    & "$jdk/java.exe" -Xmx768m -cp "work/thumbnail-classes;$joml;$gson" PrepareThumbnails @arguments
    if($LASTEXITCODE){throw 'Thumbnail preparation failed'}
} finally { Pop-Location }
