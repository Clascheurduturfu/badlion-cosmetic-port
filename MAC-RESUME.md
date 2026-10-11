# Mac resume — 2026-10-11, release 0.5.0

The project now has four working standalone adapters: Forge **1.8.9 and 1.12.2**, Fabric **1.20.1 and 1.21.1**. All four production world audits passed on this Apple Silicon Mac and exited cleanly. Screenshot review confirmed visible meshes; the 1.20.1 release includes the corrected vertex termination calls required by its older API.

Start with [README.md](README.md), [validation](docs/validation.md), [weapon support](docs/weapons.md) and [the release manifest](release/manifest.json). The public source repository is [Clascheurduturfu/badlion-cosmetic-port](https://github.com/Clascheurduturfu/badlion-cosmetic-port). Generated binaries and the external asset bundle are kept out of source history.

## Play

Double-click the matching launcher in the project root:

- `Play Forge 1.8.9.command`
- `Play Forge 1.12.2.command`
- `Play Fabric 1.20.1.command`
- `Play Fabric 1.21.1.command`

Press **K** for the cosmetics browser and **F5** to change camera. Hold a vanilla sword or bow after selecting its skin. Forge previews open an offline creative world; Fabric previews open the normal title screen, where a local world can be selected. The test identity is not an authenticated multiplayer login.

The new instances are `instances/forge-1.8.9-0.5.0-mac`, `instances/forge-1.12.2-mac`, `instances/fabric-1.20.1-mac` and `instances/fabric-1.21.1-mac`. The earlier Funny Ghost preview at `instances/forge-1.8.9-mac` is preserved separately. Its selections were not overwritten by the new tests.

The prepared asset catalog now contains **2,818 packs**, including **45 swords and 17 bows**. Keep `exports/mesh-assets` intact; all instances link to this approximately 4.3 GB bundle. The mod JAR alone is insufficient. Sword/bow resources were available locally; no fresh Windows capture was needed.

## Runtime and rebuild

Java 8 x86_64 is at `work/toolchains/zulu8.96.0.205-ca-jdk8.0.504-macosx_x64/Contents/Home`. Legacy Forge uses Rosetta and Badlion's checksum-verified patched LWJGL 2 archive, SHA1 `d94ed8060c920cd35d9be520dda30e39e70e8652`. Stock LWJGL 2 crashes during shutdown on this Mac. The mod itself does not depend on Badlion cosmetic services.

Java 21 ARM is at `work/toolchains/zulu21.52.203-ca-jdk21.0.12.1-macosx_aarch64/Contents/Home`. The 1.20.1 runtime uses the installed Java 17 ARM JRE under `~/Library/Application Support/Badlion Client/Data/zulu-17.0.14_7.jre/Contents/Home`; it builds with Java 21 targeting Java 17 bytecode. Fabric uses ordinary LWJGL 3. No account files, launcher profiles or system JDKs were changed.

```sh
JAVA_HOME="$PWD/work/toolchains/zulu8.96.0.205-ca-jdk8.0.504-macosx_x64/Contents/Home" sh forge-1.8.9/build-mac.sh
JAVA_HOME="$PWD/work/toolchains/zulu8.96.0.205-ca-jdk8.0.504-macosx_x64/Contents/Home" sh forge-1.12.2/build-mac.sh
JAVA_HOME="$PWD/work/toolchains/zulu21.52.203-ca-jdk21.0.12.1-macosx_aarch64/Contents/Home" sh fabric-1.20.1/build-mac.sh
JAVA_HOME="$PWD/work/toolchains/zulu21.52.203-ca-jdk21.0.12.1-macosx_aarch64/Contents/Home" sh mod/build-mac.sh
```

Use `tools/forge-mac.py` and `tools/fabric-mac.py` for this environment. Their `--prepare` mode recreates isolated runtime metadata after moving the project. Audit commands and evidence paths are in [validation](docs/validation.md). The Windows PowerShell production helpers describe earlier versions and should not be assumed to exercise the new ports.

## Preservation and next work

Original Forge 0.4.0 and Fabric 0.3.6 artifacts remain in `release/`. Pre-change source/assets metadata and notes were backed up under `work/backups/ports-weapons-2026-10-11`. Earlier bootstrap evidence is retained under `work/backups/mac-bootstrap-2026-10-11`; its notes are [historical](docs/mac-bootstrap-history.md).

Continue weapon visual calibration, then add cached `BACK` (15 entries) and `NECK` (27 entries) attachments and recover the two aura suppliers. Remaining pet callbacks, joint visibility, names, multipart overlays, bloom, equipment/skin/crouch calibration and compatibility tests are listed in the root README. Do not regenerate the adapters with old one-off migration scripts.

The Windows native unlocker is a separate 0.1.4 project. See [its guide](docs/windows-unlocker.md). Its transferred binary and earlier Windows evidence are preserved; it was not newly run on this Mac. Multiplayer synchronization remains unimplemented.
