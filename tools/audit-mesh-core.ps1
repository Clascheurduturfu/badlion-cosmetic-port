$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot
$jdk = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin'
$joml = Get-ChildItem -LiteralPath (Join-Path $project 'mod\.gradle-user\caches\modules-2\files-2.1\org.joml\joml\1.10.5') -Filter '*.jar' -Recurse | Select-Object -First 1
$classes = Join-Path $project 'work\mesh-audit-classes'
New-Item -ItemType Directory -Force $classes | Out-Null
Push-Location $project
try {
    & (Join-Path $jdk 'javac.exe') -cp $joml.FullName -d $classes shared\src\main\java\local\badlioncosmeticport\model\*.java shared\src\main\java\local\badlioncosmeticport\render\ObjGeometry.java tests\MeshCoreAudit.java
    if ($LASTEXITCODE) { throw 'Mesh audit compilation failed' }
    & (Join-Path $jdk 'java.exe') -Xmx768m -cp "$classes;$($joml.FullName)" MeshCoreAudit exports\mesh-assets\models 1> exports\mesh-core-validation.json 2> work\mesh-core-errors.log
    $result = $LASTEXITCODE
    Get-Content exports\mesh-core-validation.json
    if ($result) { Get-Content work\mesh-core-errors.log; throw 'Mesh audit failed' }
} finally { Pop-Location }
