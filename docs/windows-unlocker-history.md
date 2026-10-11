# Badlion Cosmetic Port

Current standalone releases: Forge 1.8.9 **0.4.0** and Fabric 1.21.1 **0.3.6**.
Start with [HANDOFF.md](HANDOFF.md) and [MAC-RESUME.md](MAC-RESUME.md).
On this Mac, double-click **Play Forge 1.8.9.command** for the tested offline
preview; press **K** for the cosmetic browser. The instructions below describe
the separate historical Windows native hook.

Native unlocker v0.1.3 was confirmed working in game by the user and archived in
releases/badlion-cosmetic-port-0.1.3-confirmed.zip. Installed native v0.1.4 retains
that behavior and adds an optional local owned-list export. Its export marker is
absent, so exporting is disabled. All eight native tests passed. The new standalone
Fabric 1.21.1 cape mod is v0.2.2; see mod/README.md and exports/REVERSE-ENGINEERING.md.

The stock response captured in diagnostic mode contains 68 cosmetics and requires
cosmeticData/sourceData objects. See dist/stock-response-schema.json for field
names and types only. The former numeric active issue and missing object fields
are both addressed. The diagnostic marker was removed when v0.1.3 was installed.

## What it does

`dist/version.dll` forwards all 17 original VERSION exports to the Windows system
DLL. Its startup worker reads the adjacent `cosmetics.json`, waits up to 120 seconds
for `badlion_electron.dll`, checks the native export's first 24 bytes, and detours
`Java_net_badlion_client_Wrapper_getAvailableCosmetics` with MinHook. It returns:

```json
{"cosmetics":[{"cosmeticId":123,"cosmeticType":"CAPE","active":false,"cosmeticData":{},"sourceData":{},"globalData":{}}]}
```

Version 0.1.3 first calls the original native function and preserves its response.
Missing catalog entries are appended with boolean active=false and empty
cosmeticData, sourceData and globalData objects. Owned records and their active
state, styling data and other response fields are preserved. Merge failures return
the stock response unchanged. IDs and types come from the registered catalog, with duplicates removed. The
payload mirrors the old DLL's field names and inactive state. No credential
access, networking, download, startup registration, driver or launcher modification
is implemented. Runtime writes are the adjacent diagnostic log and, only when
an explicit export marker is present, the selected local owned-cosmetics file.
Other cosmetic enable/disable functions are not intercepted.

The catalog was extracted from the existing stock `data5.jar`, not the older
downloaded catalog. Its SHA256 is
`56883a127d2d95d033d7a4f3fa176f49ab089750876d8b32b609b982723b8700`.
The inspected native DLL SHA256 is
`c5a70f343b40c9b74859417788cdc643f2b8188eda38e8614c9810eb25cad72c`.
The function RVA is `0x103740`. The installer requires this native DLL hash.

## Checking and trying it

Run `./manage.ps1 -Action Check` while the Badlion game is running. This prints
Java executable paths without printing command lines or account tokens. Identify
the actual game's Java bin folder, then close Java games.

```powershell
./manage.ps1 -Action Install -RuntimeBin 'FULL PATH TO THE GAME JAVA BIN FOLDER'
```

This copies only the release `version.dll` and catalog into that bin folder. It
refuses to overwrite other files with those names. **Do not put it in the Lunar
launcher folder.** It belongs beside the actual game `java.exe`/`javaw.exe`.
The selected runtime must load VERSION from that directory; some older Java
builds may not. A missing log means the proxy did not load. `hook enabled` in the
log means the detour was installed, not that Badlion accepted the response.
An unsupported-code message means the function was left unchanged.

After closing the game, remove only this project's matching files with:

```powershell
./manage.ps1 -Action Remove -RuntimeBin 'THE SAME JAVA BIN FOLDER'
```

The diagnostic log remains for inspection. Restarting the game removes all
in-memory hooks. Keep `version-test.dll` and `badlion_electron.dll` from the test
build directory out of client folders; they are test artifacts only.

## Building

Run `./build.ps1`. Requires the existing Visual Studio Community 2026 x64 toolchain,
CMake, Python and JDK JNI headers. Override `JDK_INCLUDE` in CMake if needed.
Source dependencies are vendored: MinHook v1.3.4 commit
`c3fcafdc10146beb5919319d0683e44e3c30d537` and nlohmann/json v3.11.3.
Their licenses are in `vendor/minhook/LICENSE.txt` and `vendor/json-LICENSE.MIT`.
Dependencies: https://github.com/TsudaKageyu/minhook and https://github.com/nlohmann/json.

MSVC export generation uses LIB's `.exp` output. `patch_forwarders.py` replaces
reserved forwarder module strings with the absolute Windows system VERSION path;
it changes only those strings in the PE export directory, preserving file layout.
Consequently, rebuild when moving to a machine with a different Windows directory.
