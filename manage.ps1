param(
    [ValidateSet('Check', 'Install', 'Remove')][string]$Action = 'Check',
    [string]$RuntimeBin
)
$ErrorActionPreference = 'Stop'
$native = Join-Path $env:LOCALAPPDATA 'Programs\Lunar Client\native-modules\badlion_electron.dll'
$expected = 'c5a70f343b40c9b74859417788cdc643f2b8188eda38e8614c9810eb25cad72c'
Write-Output 'Badlion Cosmetic Port 0.1.4'
if (!(Test-Path -LiteralPath $native) -or (Get-FileHash -LiteralPath $native -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) {
    throw 'Installed native module differs from the inspected Badlion 4.5.13. Recheck compatibility before installing.'
}
Write-Output 'Installed native module matches the inspected build.'
$java = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'")
if ($Action -eq 'Check') {
    $java | Select-Object ProcessId, ExecutablePath | Format-Table -AutoSize
    Write-Output 'Use the Badlion game Java executable shown above to identify its bin folder. Launcher Java version probes are not sufficient.'
    return
}
if ($java.Count -gt 0) { throw 'Close Java games before installing or removing the proxy.' }
if (!$RuntimeBin) { throw 'Specify -RuntimeBin with the bin directory actually used by the Badlion game.' }
$resolved = (Resolve-Path -LiteralPath $RuntimeBin).Path.TrimEnd('\')
$roots = @((Join-Path $env:APPDATA 'Badlion Client\Data'), (Join-Path $env:USERPROFILE '.lunarclient\jre'))
$allowed = $false
foreach ($root in $roots) {
    if ($resolved.StartsWith($root.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { $allowed = $true }
}
if (!$allowed -or !(Test-Path -LiteralPath (Join-Path $resolved 'java.exe'))) {
    throw 'RuntimeBin must be a Badlion or Lunar Java bin folder containing java.exe.'
}
foreach ($name in @('version.dll', 'cosmetics.json')) {
    $source = Join-Path $PSScriptRoot "dist\$name"
    $destination = Join-Path $resolved $name
    if (Test-Path -LiteralPath $destination) {
        if ((Get-FileHash -LiteralPath $source).Hash -ne (Get-FileHash -LiteralPath $destination).Hash) {
            throw "Existing $name is not this project's file; leaving it untouched."
        }
    }
}
foreach ($name in @('version.dll', 'cosmetics.json')) {
    $destination = Join-Path $resolved $name
    if ($Action -eq 'Install') {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot "dist\$name") -Destination $destination
    } elseif (Test-Path -LiteralPath $destination) {
        Remove-Item -LiteralPath $destination
    }
}
Write-Output "$Action complete: $resolved"
if ($Action -eq 'Install') {
    Write-Output "After launching Badlion, inspect $resolved\badlion-cosmetic-port.log for 'hook enabled'."
}
