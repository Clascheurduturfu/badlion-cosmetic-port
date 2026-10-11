# Badlion Cosmetic Port

A client-side mod that renders cached Badlion cosmetics in ordinary Minecraft. Choose cosmetics locally with a searchable browser, original models, original-resolution textures and animated frames. Badlion and Lunar do not need to be running.

This workspace also contains a **separate Windows native catalog unlocker** for a specific Badlion client build. The standalone mods and the native unlocker have different installation paths and capabilities.

## Minecraft versions

| Minecraft | Loader | Mod release | Java | Artifact |
| --- | --- | --- | --- | --- |
| 1.8.9 | Forge 11.15.1.2318 | 0.5.0 | 8 | `badlion-cosmetic-port-forge-0.5.0.jar` |
| 1.12.2 | Forge 14.23.5.2864 | 0.5.0 | 8 | `badlion-cosmetic-port-forge-1.12.2-0.5.0.jar` |
| 1.20.1 | Fabric Loader 0.16.14 + Fabric API 0.92.6+1.20.1 | 0.5.0 | 17 | `badlion-cosmetic-port-fabric-1.20.1-0.5.0.jar` |
| 1.21.1 | Fabric Loader 0.16.14 + Fabric API 0.116.6+1.21.1 | 0.5.0 | 21 | `badlion-cosmetic-port-fabric-1.21.1-0.5.0.jar` |

Use the exact Minecraft/loader combination for the chosen JAR. Forge and Fabric builds are separate. The Java mod code is portable across Windows, macOS and Linux; the validation performed in this handoff is on macOS. See [validation](docs/validation.md) for the checks and their limits.

The working 0.4.0 Forge 1.8.9 and 0.3.6 Fabric 1.21.1 releases remain preserved as rollback builds. Newer Minecraft releases and NeoForge are not currently supported.

## Cosmetics

The prepared local catalog contains **2,818 packs**:

| Category | Packs | Implemented appearance |
| --- | ---: | --- |
| Cloaks / capes | 1,451 | Procedural cloth and MD5 models; dynamic, curved and rectangular motion |
| Hats | 319 | OBJ/MD5 attachments, texture and supported geometry animation, height adjustment |
| Bandanas | 363 | Head attachments; recovered special shader for ID 345 |
| Shields | 339 | Back/arm attachment; supported animated geometry and special shader for ID 250 |
| Pets | 141 | Shoulder/head/air poses where available; animated models and selected pet effects |
| Wings | 143 | OBJ/MD5 geometry and adapted flap/fall motion |
| Sword cosmetics | 45 | Vanilla sword material variants, original 3D models, GUI art and component animation |
| Bow cosmetics | 17 | Original bow models, four string/pull stages, nocked arrows and animated components |

Sword skins follow the equipped wooden, stone, iron, golden or diamond sword. Modern versions also support netherite. Bow cosmetics apply to the vanilla bow. Held replacements are limited to the local player's actual item stacks; inventory icons use the selected appearance. Item damage, enchantments, durability, crafting and server interactions retain their vanilla behavior. Modded weapons keep their own renderers.

Weapons are not all texture swaps: several have animated geometry, separate handles/flames, string stages, arrows or beam overlays. See [weapon support](docs/weapons.md) for recovered behavior and remaining visual work.

Cosmetics are selected and rendered locally. **Other players do not receive or see your selections through this mod.** Multiplayer cosmetic synchronization is not implemented. The mod does not contact Badlion's ownership or selection service.

## Install

1. Install the matching Forge or Fabric version into your Minecraft launcher. Fabric also needs the matching Fabric API JAR.
2. Put exactly one port JAR from `release/` in that instance's `mods/` directory.
3. Copy the complete prepared `exports/mesh-assets/` directory into `<game directory>/config/badlion-cosmetic-port/mesh-assets/`.
4. Launch Minecraft and press **K**. Search by name or cosmetic ID, click to equip, and use **Unequip** to remove a selection.

The catalog is an external asset bundle of approximately 4.3 GB. **Installing only the mod JAR is insufficient.** Keep `catalog.json`, `packs/`, `models/`, `thumbnails/` and particle sprites together. Minecraft's game directory may differ between launcher profiles.

Selections are saved in `config/badlion-cosmetic-port/mesh-selection.json`. Category selections are independent. Hover previews do not change saved equipment. In game, hold a sword or bow to see its selected weapon cosmetic.

The optional portable installer verifies the release checksum and links the prepared assets into a game directory you explicitly choose:

```sh
python3 tools/install-portable.py --target forge-1.12.2 --game-dir "/path/to/game"
python3 tools/install-portable.py --target fabric-1.20.1 --game-dir "/path/to/game"
```

Other explicit targets are `forge-1.8.9` and `fabric-1.21.1`. The old aliases `forge` and `fabric` deliberately retain the rollback releases; use an explicit version target for 0.5.0. Add `--copy-assets` when a directory link is unsuitable. Move an older port JAR aside before upgrading; the installer refuses conflicting existing port files. It does not install the Minecraft loader or Fabric API.

### macOS previews

The local `.command` launchers and Python helpers create isolated offline preview instances under `instances/`. They do not read account files or edit launcher profiles. The preview identity is for local verification, not an authenticated multiplayer login.

```sh
python3 tools/forge-mac.py --minecraft 1.12.2 --prepare --play
python3 tools/fabric-mac.py --minecraft 1.20.1 --prepare --play --java "/path/to/java17/bin/java"
```

Legacy Forge uses Java 8 **x86_64** and LWJGL 2; Apple Silicon needs Rosetta for this preview configuration. The helper can use the checksum-verified patched macOS LWJGL native archive already provided by Badlion, because the stock archive crashes at shutdown on this Mac. This workaround concerns the preview runtime; the mod is not dependent on Badlion's cosmetic services. Fabric uses the ordinary LWJGL 3 runtime and native Apple Silicon Java.

[MAC-RESUME.md](MAC-RESUME.md) records local toolchains and environment history.

## Windows native unlocker

The Windows project is version **0.1.4**, with the user-confirmed older **0.1.3** archive retained for rollback. It injects additional inactive catalog entries into the response from a supported Badlion native function while preserving owned entries and their data. It forwards the original Windows VERSION exports through `version.dll`.

It works only with the inspected native DLL build and belongs beside the **game's actual Java executable**. A Windows DLL cannot run on macOS. It does not intercept cosmetic activation, grant server-side ownership or establish multiplayer visibility for unowned cosmetics. Read [Windows unlocker setup and limitations](docs/windows-unlocker.md) before using the native project.

## Remaining work

- **Back accessories and backpacks:** 15 `BACK` catalog entries; cached models/textures are available. Add torso attachment, crouch/armor placement and material handling.
- **Chains, necklaces and other accessories:** 27 `NECK` entries, including gold chains; cached assets are available. Add attachment and per-model placement/chroma behavior. Other accessory categories still need integration.
- **Auras:** 2 catalog entries, Power Up and Money Rain. Cached textures exist, but particle supplier behavior still needs recovery and adaptation.
- **Weapon visual parity:** calibrate native first/third-person transforms and left-hand positioning; tint/fullbright beam overlays, projectile appearance, and legacy enchantment glint need completion. Two cached weapon entries lack dedicated GUI artwork and use their 3D model there.
- **Multipart overlays:** 21 previously identified packs need additional textures/material passes. Some cached/procedural entries remain excluded.
- **Pet effects:** finish special callbacks, names, conditional joint visibility and remaining particle suppliers. Fire, sonic and ThirtyVirus paths already have adapted implementations.
- **Rendering parity:** bloom/glow compositing, shield first-person/blocking calibration, skins/helmets/crouching and more accurate grounded/falling wing behavior.
- **Distribution:** package the external assets reproducibly, decide the public distribution/credit/license arrangement, and add a release workflow after the GitHub repository is created.
- **Compatibility:** test on Windows/Linux and popular rendering mods/shader packs; add multiplayer synchronization only as a separate feature.

The newer 1.20.1 adapter was small enough to include. Porting to the latest Minecraft rendering APIs is a separate undertaking and is not part of this release.

## Develop

| Directory | Purpose |
| --- | --- |
| `shared/src/main/java/` | Geometry, recovered animation/motion and common weapon component logic |
| `forge-1.8.9/` | Forge 1.8.9 integration and production audit |
| `forge-1.12.2/` | Forge 1.12.2 integration and production audit |
| `fabric-1.20.1/` | Fabric 1.20.1 integration |
| `mod/` | Fabric 1.21.1 integration |
| `src/`, `vendor/` | Separate Windows native hook and its dependencies |
| `tools/`, `tests/` | Asset preparation, installation, packaging and audits |
| `exports/` | Prepared local assets and captured validation evidence |

Use a Java 8 JDK for Forge and Java 21 JDK for the Fabric builds. The 1.20.1 artifact targets Java 17 bytecode and runs on Java 17. On macOS/Linux:

```sh
JAVA_HOME="/path/to/jdk8" sh forge-1.8.9/build-mac.sh
JAVA_HOME="/path/to/jdk8" sh forge-1.12.2/build-mac.sh
JAVA_HOME="/path/to/jdk21" sh fabric-1.20.1/build-mac.sh
JAVA_HOME="/path/to/jdk21" sh mod/build-mac.sh
python3 tools/audit-core-portable.py --java-home "/path/to/jdk21"
```

Forge 1.8.9's first build needs `sh forge-1.8.9/build-mac.sh setup`; 1.12.2 uses the official ForgeGradle 3 MDK and resolves its mappings automatically. Windows users can run each project's Gradle wrapper with the matching JDK; `build.ps1` applies to the native Windows DLL.

`tools/prepare_weapons.py` reads an installed Badlion asset cache and appends original OBJ models and decoded AVIF frames to an already prepared mesh catalog. It requires Pillow with AVIF support. `--cache` and `--output` select explicit input/output directories. The main mesh, particle, effect shader and thumbnail preparation scripts are under `tools/`; [HANDOFF.md](HANDOFF.md) documents the recovered renderer evidence.

Generated caches, game instances, native binaries, original client/runtime captures and the multi-GB asset bundle are excluded by `.gitignore`. Cloning this source repository does not provide the required assets or built mod JARs. Build the chosen adapter using the commands above and prepare or supply the external asset bundle before installation. Local artifact hashes are preserved in `release/manifest.json`; they describe the tested workspace builds.

## Credits

Original cosmetic artwork, meshes and animations belong to their original creators and Badlion contributors. This project adapts cached assets and recovered behavior; it is not an official Badlion or Lunar product. Minecraft, Forge, Fabric, JOML, MinHook and nlohmann/json retain their respective licenses. Dependency license files are preserved where vendored. A project-wide source license has not yet been selected.
