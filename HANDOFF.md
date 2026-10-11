# Badlion Cosmetic Port handoff - 2026-10-11

**Mac continuation:** see [MAC-RESUME.md](MAC-RESUME.md) for the tested Apple Silicon
build/runtime setup, production audit results and the ready Funny Ghost preview.
The original transfer state below is preserved as historical context.

## Current continuation: 0.5.0

Forge **1.12.2** and Fabric **1.20.1** have been added. Forge 1.8.9 and Fabric 1.21.1 are also updated to **0.5.0**, with **45 sword and 17 bow cosmetics** across all four targets. Models, material textures, GUI art, bow pull/string stages and animated components were available in the Mac cache; no fresh Windows capture was necessary.

All four production world audits passed with clean exits. The final Fabric 1.20.1 build fixes explicit vertex termination and browser background handling; screenshot review verified visible cosmetics and held weapons. Core geometry, native motion comparisons and the 823 new weapon texture frames pass. See [validation](docs/validation.md) for scope, draw counts and limitations.

- [README.md](README.md): future GitHub project guide, installation, four version targets, native Windows project and roadmap.
- [MAC-RESUME.md](MAC-RESUME.md): current toolchains, launchers and isolated instances.
- [Weapon support](docs/weapons.md): recovered components and remaining visual parity work.
- [Windows unlocker](docs/windows-unlocker.md): separate 0.1.4 project, current DLL hash, supported native build and historical evidence.
- [Release manifest](release/manifest.json): authoritative 0.5.0 artifact filenames and SHA256 values.

The external catalog now contains **2,818 packs**. Source adapters are `forge-1.8.9/`, `forge-1.12.2/`, `fabric-1.20.1/` and `mod/`, with common code under `shared/`. Use explicit installer targets such as `forge-1.12.2`; old `forge`/`fabric` aliases preserve rollback versions.

Next: weapon positioning/beam/glint calibration; cached backpacks/back attachments (15 BACK entries), chains/necklaces (27 NECK entries) and aura suppliers (2 AURA entries). These accessory/aura categories are not implemented yet. Further latest-version ports remain deferred. Preserve the older Funny Ghost instance separately and follow the root README for the full roadmap.

Original releases and pre-change backups remain intact. The current public source repository is [Clascheurduturfu/badlion-cosmetic-port](https://github.com/Clascheurduturfu/badlion-cosmetic-port). The material below records the earlier Windows transfer and is historical where it names old releases or installation commands.

Start here. This document carries the working state across machines; the entire chat history is not needed to resume. Historical reports describe older versions. Current 0.5.0 information is in the section above and the matching validation reports.

## Original transferred releases (historical baseline)

- Fabric Minecraft 1.21.1: **0.3.6**, SHA256 `8a0e0959ef6807329cc696b930b02febb432f235c4642af2da835abe67ae2177`.
- Forge Minecraft 1.8.9: **0.4.0**, SHA256 `1fb35136661f642f5a9832611f256b1864a20ae6130fddcfad9c1a42f0b1eb10`.
- Original Windows native catalog hook: **0.1.4**, source under `src`, confirmed older 0.1.3 archive retained under `reference`. This is separate from the standalone mods. The DLL cannot run on macOS.

Both built mods are included under `release`. The complete decoded catalog is under `exports/mesh-assets`: 2,756 prepared packs, original HD frames, models, clips, particle sprites and browser thumbnails. About 4.1 GiB of unpacked assets. Keep this directory intact; the JAR alone is insufficient.

The modern build and Forge build share the Java 8 compatible geometry/animation core in `shared/src`. Version-specific integrations live in `mod/src` and `forge-1.8.9/src`. Do not regenerate them with the one-off migration scripts previously used under `work/forge-tools`; those scripts are deliberately not part of this transfer.

## Install or resume on a Mac

1. Extract the ZIP into a normal project directory with adequate free space. Open that folder as the Codex project, then ask it to read `HANDOFF.md` and the reports under `exports`.
2. Install the matching Minecraft loader using your chosen launcher: Forge 11.15.1.2318 for 1.8.9, or Fabric Loader 0.16.14 plus Fabric API 0.116.6+1.21.1 for 1.21.1. Minecraft and launcher login files are not included.
3. Run `python3 tools/install-portable.py --target forge --game-dir "/absolute/path/to/your/1.8.9/game"` or use `--target fabric` for 1.21.1. It installs the included JAR and links the assets into that game's config directory. Move the project only after recreating that link. Use `--copy-assets` instead if the game directory must be independent.
4. Start the appropriate profile and press **K** for the visual cosmetics browser. Hover previews without saving; click equips. Cloak modes, pet placement, shield arm/back placement and hat height are available. Selections are local.

For builds, use a suitable Java 8 JDK for the legacy adapter and Java 21 for the modern adapter. `sh forge-1.8.9/build-mac.sh setup` performs first-time Forge setup and build; `sh forge-1.8.9/build-mac.sh` rebuilds. `sh mod/build-mac.sh` builds Fabric. Set `JAVA_HOME` to the matching JDK first. Gradle wrappers resolve public dependencies online. Windows JDKs, Gradle caches and native Minecraft libraries are excluded. These Mac scripts are new portability helpers, not a tested Mac execution claim. Apple Silicon compatibility of Java 8 / legacy LWJGL must be checked on the actual Mac; the Windows-specific instance preparation and preview helpers should not be used there.

## What was validated

The production reobfuscated Forge JAR rendered all six families (cape, hat, bandana, shield, pet, wings), changed animated textures, ran original shader adapters, spawned cloak particles and the adapted fire/sonic pet effects, selected ThirtyVirus clips, and rendered a 32-joint air-pet variant. See `exports/forge-0.4.0-validation.json` and the offline production log. Modern 0.3.6 likewise passed its production world audit, including held shield replacement and the hide-without-item option.

Native motion differential checks cover 556,000 comparisons with zero error; ribbon geometry 490,460 comparisons with maximum error about 9.54e-7; wing child-local animation 2,250 comparisons across 90 frames with maximum error about 1.43e-6. The shared model audit parsed 111 MD5 meshes, 319 OBJ files and 217 clips with zero failures. These checks establish the tested components, not full visual parity for every cosmetic.

The user visually judged the corrected cloaks and wings almost identical to Badlion. The modern third-person shield tilt was subsequently corrected to use the original player-arm attachment rather than the vanilla held-item transform; that last tilt still needs side-by-side user confirmation.

Funny Ghost (PET 8) uses the original 448-triangle / 316-vertex / 7-joint mesh and a 1024x1024 texture, byte-identical to the stock cached resource. It was not decimated. Forge 0.4.0 now applies the recovered pet lighting, smooth shading and culling flags, which were missing and could cause a faceted appearance. This was production-tested, but its close-up visual match to stock Badlion 1.8.9 remains unconfirmed. See `exports/funny-ghost-model-check.json`.

## Recovered reference material

`reference/cosmetic-port-0.3.6-runtime-capture.zip` contains 6,175 runtime class records plus indexed CFR reconstructions. The useful extraction point was the native JVMTI ClassFileLoadHook output; on-disk protected JAR bytes are not equivalent to the decoded runtime classes. Class paths use hash-prefixed folders to preserve case-sensitive obfuscated names on Windows. The capture reflects the recovered modern client, not a complete decoded 1.8.9 capture. Follow `renderer-decompilation-index.json`, `renderer-recovery-report.md`, `renderer-followup.md` and `rea-native/NATIVE-LOADER-FINDINGS.md` for class responsibilities and provenance. Preserved focused decompiler output is under `reference/decompiled-renderer`.

Useful class families: aGe/aGd MD5 parsing, aGi/aGl animation clocks and skinning, aCP/aCR procedural cape geometry, aCJ/aEJ wings, aCH/aBY/aCE pets, aMI pet metadata, aDe/aEz accessories/shields, aEd/MD cloak particles. Obfuscated names across client versions must not be assumed to have the same role from name alone.

## Next work and limits

- Controlled close-up Funny Ghost comparison on Badlion 1.8.9; capture that runtime only if it exposes a remaining version-specific discrepancy.
- Remaining pet callbacks, joint-visibility behavior, names and cosmetic-specific particle suppliers. Fire, sonic and ThirtyVirus controllers are adapted; about 20 other supplier packs remain.
- Multipart overlays: 21 packs (5 hats, 8 wings, 4 cloaks, 2 bandanas, 2 shields) have unfinished additional layer paths. User chose to defer these. Thirteen other catalog entries were excluded during preparation.
- Full bloom/postprocessing is absent. Five cloak families request glowing particles; brightness is present, soft bloom halos are not. Japanese Temple's leaf motion has a recovered adapter, still needs visual comparison.
- Some skin, helmet, crouch and first-person/blocking shield calibration remains. Forge's native vanilla-cape overlap and compatibility with OptiFine/other render mods are untested.
- Multiplayer synchronization is not implemented. The standalone mod renders the local player only. Whether an unowned selection in the earlier native Badlion hook is visible on another client remains unverified; menu unlocking is not proof of server-side selection acceptance. The user has a second player available for a later test.

## Badlion 1.8.9 crash fix on the Windows host

The latest two native JVM fatal reports showed allocation failure of about 1.1 MB and system-wide available commit/pagefile capacity of 7 MB and then 0 MB. Lunar launched with `-Xmx8273M`. An EpicWebHelper process was using approximately 53.67 GiB of private memory. With explicit user approval that exact helper was closed, leaving F1 running. Lunar's saved game memory allocation was reduced to 3146 MB (about 3 GB in its UI). The user confirmed that the cosmetics page now works. A subsequent live launch was verified to use `-Xmx3146M`; free system commit capacity was about 55 GiB, and no newer JVM fatal report had appeared at that check.

No native hook modification was needed for this crash. The newest native hook log separately says merge failed and the stock response was returned unchanged on 1.8.9; therefore this successful menu test does not establish unowned cosmetic multiplayer behavior. Full JVM crash reports, launcher/account configurations and tokens are excluded from the handoff. See the sanitized `exports/badlion-1.8.9-memory-fix-v1.json`.

## Windows build paths and preservation

Original working folder: a Windows checkout under `Documents/GitHub/Projects/badlion cosmetic port`. Fabric instance: `instances/fabric-1.21.1-mesh`; Forge instance: `instances/forge-1.8.9-mesh`. External assets are linked into each instance. Build with the corresponding `build.ps1` on Windows; Forge uses the project-local Temurin JDK 8 under `work/forge-tools/jdk8`. Audits: `tools/production-mesh.ps1`, `tools/forge-instance.py --audit`, `tools/audit-mesh-core.ps1`, `tools/audit-native-motion.ps1`.

No Git commits were made. The project is currently an untracked folder inside a larger Git workspace; do not commit to that workspace's main branch. Archive build outputs and validation evidence before further renderer changes, bump the visible mod version for each fix, and preserve selection files during tests.

The ZIP excludes launcher profiles, accounts, secrets, private chat logs, unrelated files, full JVM crash reports, saves, Windows JDKs and disposable caches. It contains development state, not an authenticated Minecraft installation or a copy of the conversation. The local HTTP download serves only this ZIP and its checksum, temporarily.
