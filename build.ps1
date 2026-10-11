$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    cmake -S . -B build -G 'Visual Studio 18 2026' -A x64
    if ($LASTEXITCODE) { throw 'CMake configuration failed' }
    cmake --build build --config Release --parallel 4
    if ($LASTEXITCODE) { throw 'Build failed' }
    ctest --test-dir build -C Release --output-on-failure
    if ($LASTEXITCODE) { throw 'Tests failed' }
    Copy-Item -LiteralPath 'build\Release\version.dll' -Destination 'dist\version.dll'
    Get-FileHash -LiteralPath 'dist\version.dll', 'dist\cosmetics.json' -Algorithm SHA256
} finally { Pop-Location }
