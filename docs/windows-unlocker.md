# Windows native catalog unlocker

This is separate from the Forge/Fabric mods. Its source and management scripts are version **0.1.4**; the earlier **0.1.3** build was confirmed in game by the user. The confirmed archive is `reference/badlion-cosmetic-port-0.1.3-confirmed.zip`. The current `dist/build-report.json` describes the older artifact and must not be treated as a checksum manifest for the current DLL.

## Behavior and scope

`dist/version.dll` forwards all 17 original VERSION exports to the Windows system DLL. Its worker reads the adjacent `cosmetics.json`, waits for `badlion_electron.dll`, verifies the target's expected code, and uses MinHook to detour `Java_net_badlion_client_Wrapper_getAvailableCosmetics`.

The hook calls the original function first. Missing registered catalog entries are appended with boolean `active: false` and empty `cosmeticData`, `sourceData` and `globalData` objects. Owned records, styling data, active state and other fields are preserved. A merge failure returns the original response. Other activation/deactivation functions are not intercepted.

This is local catalog expansion. It does not grant server-side ownership or demonstrate multiplayer visibility for unowned entries. A `hook enabled` log line proves the detour was installed; the user-confirmed 0.1.3 result is the separate evidence that the client accepted that response in the tested build.

The optional owned-list export added in 0.1.4 is disabled without an explicit marker. See the implementation and [historical guide](windows-unlocker-history.md) for the exact marker and schema. It has not been newly tested on Windows during the Mac handoff.

## Supported build

- Inspected `badlion_electron.dll` SHA256: `c5a70f343b40c9b74859417788cdc643f2b8188eda38e8614c9810eb25cad72c`.
- Stock catalog source `data5.jar` SHA256: `56883a127d2d95d033d7a4f3fa176f49ab089750876d8b32b609b982723b8700`.
- Target function RVA in the inspected native DLL: `0x103740`.
- Current transferred `dist/version.dll` SHA256: `39c48d082c96e8538dd496351573adbdb6d2975b63a8a484be8f0ce29c210cde`.

The installer restricts the target to the inspected native build. Different Badlion versions require a new inspection. The DLL is Windows-only; it cannot load on macOS or Linux.

## Install and remove

While the Badlion **game** is running, inspect the runtime location:

```powershell
./manage.ps1 -Action Check
```

This reports Java executable paths without printing account tokens or command lines. Close Java games, then install into that game's actual Java `bin` folder:

```powershell
./manage.ps1 -Action Install -RuntimeBin 'FULL PATH TO THE GAME JAVA BIN FOLDER'
```

The script copies the matching DLL and catalog and refuses conflicting files. Do not install in the Lunar launcher folder. The proxy belongs beside `java.exe`/`javaw.exe`, and the selected runtime must load VERSION from that location.

After closing the game, remove the project's matching files:

```powershell
./manage.ps1 -Action Remove -RuntimeBin 'THE SAME JAVA BIN FOLDER'
```

An absent hook log means the proxy did not load. An unsupported-code message means the hook left the function unchanged. Native logs and explicitly enabled owned-list exports are local diagnostic outputs.

## Build and evidence

Build on Windows with the CMake/MSVC workflow in `build.ps1`. The native sources are in `src/`, with MinHook and nlohmann/json under `vendor/`. The historical handoff records eight passing native tests. This macOS session preserves that evidence and the transferred DLL; it does not claim a fresh Windows runtime validation.
