# Historical Mac bootstrap notes

These notes describe the original 0.4.0/0.3.6 environment setup. For current 0.5.0 ports, use [MAC-RESUME.md](../MAC-RESUME.md) and [validation](validation.md).

# Mac resume — 2026-10-11

The transferred standalone mods now build on this Apple Silicon Mac. Forge
1.8.9 also passes the full production world audit, including clean process exit.
The Windows native unlocker remains separate. No cosmetic renderer source or
release version was changed during this environment setup.

## Play and rebuild

Double-click **Play Forge 1.8.9.command** in this project, or run:

```sh
python3 tools/forge-mac.py --play
```

This opens an offline creative preview and its cosmetic browser. **K** opens the
browser; **F5** changes camera view. The new instance is
`instances/forge-1.8.9-mac`, with assets linked to `exports/mesh-assets`. Its
selection is separate from Badlion's settings. An initial Funny Ghost (PET 8)
selection is provided for close-up review. The preview uses a default offline
skin. It is not an authenticated multiplayer launch.

For preparation after moving the project, rerun:

```sh
python3 tools/forge-mac.py --prepare
```

Local, checksum-verified Azul JDKs are under `work/toolchains`; no system JDK,
launcher profile, account file, or Badlion application was modified. Forge uses
an x86_64 Java 8 JDK through Rosetta and LWJGL 2.9.2. The Mac native library is
Badlion's official 2025-01-17 archive, verified against its installed version
metadata (`d94ed8060c920cd35d9be520dda30e39e70e8652`). The older stock library
rendered successfully but crashed in AppKit during shutdown. The newer library
completed the tested run cleanly. `--native-mode stock` remains available for
diagnostics.

Forge depends on Log4j beta9 internals, so substituting 2.17.1 fails at startup.
The helper follows Mojang's official manifest and verifies/applies its
`client-1.7.xml` logging configuration. Dependencies are downloaded into this
project's cache, reusing installed Minecraft files only after hash checks.

Build commands from the project root:

```sh
JAVA_HOME="$PWD/work/toolchains/zulu8.96.0.205-ca-jdk8.0.504-macosx_x64/Contents/Home" sh forge-1.8.9/build-mac.sh
JAVA_HOME="$PWD/work/toolchains/zulu21.52.203-ca-jdk21.0.12.1-macosx_aarch64/Contents/Home" sh mod/build-mac.sh
```

Forge's first workspace setup has already completed. If its cache is removed,
append `setup` to its build command. The original `tools/forge-instance.py` and
PowerShell helpers remain Windows-specific; use the Mac helper here.

## Verification and preservation

```sh
python3 tools/audit-core-portable.py
python3 tools/forge-mac.py --audit
```

The audit saves its report separately from the original Windows evidence and
restores the instance's cosmetic selection, including an originally absent file.
It explicitly starts a test world; ordinary mod launches do not do that. The
preview helper opts into a local creative world via the existing PLAY mode.

- `exports/mac-handoff-integrity.json`: before these note updates, checked 30,121 transferred entries;
  no changed file bytes. The original root `.gitignore` was absent from the
  extraction; all development/runtime assets were present.
- `exports/mac-asset-inventory.json`: all 2,756 packs, 23,595 texture frames and
  647 models/clips present. Model/clip hashes and all PNG headers/dimensions pass.
  This does not decode every texture pixel or establish visual parity.
- `exports/mac-build-validation.json`: both builds succeed. Every class/resource
  matches the transferred release. Fabric's whole JAR hash also matches;
  Forge omits three empty example directories, changing only its ZIP hash/layout.
- `exports/mac-core-validation.json`: 111 MD5 meshes, 319 OBJ models, 217 clips,
  220 compatible skinning pairs and the independent fixture pass. Motion has
  556,000 comparisons with zero error; ribbon has 490,460 comparisons with maximum
  error 9.54e-7; wings have 2,250 comparisons with maximum error 1.43e-6.
- `exports/forge-0.4.0-mac-validation.json`: production Forge passes with all six
  families, animated textures, shader draws, cloak particles, fire and sonic
  effects, ThirtyVirus clips and a 32-joint air pet. Process exit is zero.
- Build artifacts, audit log, screenshots and reports are preserved under
  `work/backups/mac-bootstrap-2026-10-11`. Original release JARs and Windows
  validation reports remain intact.

One intermediate run missed the Warden's short supplier window before the fixed
audit tick deadline and reported zero sonic emissions. The final run at a 120 FPS
cap passed. This timing sensitivity remains in the existing audit; a future audit
change should wait for the effect with a bounded deadline. Failed attempts are
preserved under `work/forge-mac`, rather than treated as passes. Only Forge has
been runtime-tested on this Mac; Fabric has been built and checked byte for byte.

## Next development work

Continue the controlled Funny Ghost comparison against stock Badlion 1.8.9.
Its model/texture were not simplified; the lighting/smooth-shading/culling fix is
already in Forge 0.4.0. Capture that version's runtime only if a visual discrepancy
remains. Then resume pet joint visibility/callbacks/names and the remaining special
particle suppliers. The user's deferral of multipart overlays and the final
multiplayer phase still applies. Bloom and equipment/skin/crouch calibration
remain as described in `HANDOFF.md`.

Public sources used for setup: [Forge 1.8.9 downloads](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html),
[Azul macOS installation](https://docs.azul.com/core/install/macos), and the
[Mojang 1.8.9 manifest](https://piston-meta.mojang.com/v1/packages/d546f1707a3f2b7d034eece5ea2e311eda875787/1.8.9.json).
