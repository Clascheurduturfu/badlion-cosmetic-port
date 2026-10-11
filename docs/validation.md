# Release 0.5.0 validation

All four standalone releases were built and tested on an Apple Silicon Mac on **2026-10-11**. Each test launched the production JAR through the actual Minecraft loader, entered a disposable creative world, exercised the cosmetics and exited with code zero. Release checksums are in [the manifest](../release/manifest.json) and [SHA256SUMS](../release/SHA256SUMS).

## Production world tests

| Target | Result | Sword draws | Bow draws | GUI draws | Bow pull draws | First-person draws | Third-person draws |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Forge 1.8.9 | PASS | 1,096 | 1,568 | 1,762 | 89 | 35 | 867 |
| Forge 1.12.2 | PASS | 1,083 | 1,790 | 1,908 | 84 | 26 | 939 |
| Fabric 1.20.1 | PASS | 2,294 | 2,748 | 3,204 | 85 | 170 | 1,668 |
| Fabric 1.21.1 | PASS | 2,302 | 2,849 | 3,246 | 96 | 180 | 1,725 |

These are instrumented renderer calls from the audit fixtures, not a frame-rate benchmark. The weapon fixtures equip Katana Sword and Cupid Bow, send creative inventory changes to the integrated server, and use Minecraft's bow-draw control. They exercise first-person, third-person and GUI rendering. All 62 weapon packs separately pass the metadata/component audit: 409 component selections across bow pull stages and 823 valid texture-frame references.

Each world audit also exercises the six existing wearable categories, animated textures, adapted special shaders, cloak particles, fire and sonic pet effects, a 32-joint air pet and ThirtyVirus clip cycling. Fabric tests include the held shield and the empty-hand hide/show option.

Screenshot review confirmed held weapons and GUI icons in both Forge versions, and visible wearable/weapon meshes in both Fabric versions. The final 1.20.1 browser, first-person sword and third-person effects screenshots were reviewed after fixing an older-API vertex termination issue. An earlier run had positive draw counters while meshes were invisible; its log is preserved as failed visual evidence. The release manifest points to the corrected JAR. Counters alone are insufficient evidence of appearance.

## Shared geometry and assets

- The shared parser loaded **111 MD5 meshes, 463 OBJ models and 217 animation clips**, with zero parser failures. It exercised 220 compatible skinning pairs and an independent fixture.
- Native motion comparisons: **556,000**, maximum error **0**.
- Ribbon geometry comparisons: **490,460**, maximum error **9.54e-7**.
- Wing child-local comparisons: **2,250** over 90 frames, maximum error **1.4305115e-6**.
- Pillow fully decoded all **823 newly prepared weapon PNG frames** without failures; weapon thumbnails are 128×128.
- Weapon preparation preserved 144 cached OBJ resources and original texture dimensions. The complete catalog now contains **2,818 packs**. Earlier wearable inventory checks are retained separately; this release does not claim a fresh full pixel decode of every wearable frame.

## Evidence and reproduction

Local evidence is retained under `exports/` and `work/`, excluded from the future source checkout:

- `exports/forge-1.8.9-0.5.0-mac-validation.json`
- `exports/forge-1.12.2-0.5.0-mac-validation.json`
- `exports/fabric-1.20.1-0.5.0-mac-validation.json`
- `exports/fabric-1.21.1-0.5.0-mac-validation.json`
- `exports/ports-0.5.0-validation.json`: release/installed-JAR hashes, log hashes and reviewed screenshots.
- `exports/mac-core-validation.json`, `exports/weapon-assets.json` and `exports/weapon-texture-validation.json`.

After preparing the matching isolated runtime and obtaining the required assets:

```sh
python3 tools/forge-mac.py --minecraft 1.8.9 --audit
python3 tools/forge-mac.py --minecraft 1.12.2 --audit
python3 tools/fabric-mac.py --minecraft 1.20.1 --audit
python3 tools/fabric-mac.py --minecraft 1.21.1 --audit
python3 tools/audit-core-portable.py --java-home "/path/to/jdk21"
```

Fabric's disposable test world is initially copied from the completed Forge 1.12.2 audit. Helpers preserve the prior cosmetic selection, including an originally absent selection file. They use an offline test identity. Forge runs Java 8 x86_64 through Rosetta with the checked patched LWJGL 2 native archive; Fabric runs native ARM Java 17/21 and LWJGL 3. Class-file versions were checked as 52, 52, 61 and 65 respectively. Fabric refmaps and Forge coremod manifest entries were checked inside the delivered JARs.

## Limits

The tests establish working adapters and exercised render paths. They do not establish pixel-perfect Badlion parity for all 2,818 packs, all bow/sword materials, every camera/left-hand transform, or compatibility with other renderer mods. See [weapon limits](weapons.md) and the [remaining work](../README.md#remaining-work).

There was no fresh Windows/Linux runtime test in this Mac session. The separate Windows native unlocker retains earlier evidence and its transferred DLL; it cannot be tested by loading it on macOS. Multiplayer selection synchronization and server-side cosmetic ownership are not implemented by the standalone mods.
