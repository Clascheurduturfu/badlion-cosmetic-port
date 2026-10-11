# Sword and bow cosmetics

Version 0.5.0 adds `SWORD` and `BOW` to the cosmetic browser and saved selections on all four Minecraft targets. The locally available catalog has **45 swords and 17 bows**. Required main models and material textures were found for every entry; none were blocked by unavailable Mac cosmetics.

## Assets and recovered behavior

`tools/prepare_weapons.py` reads the installed Badlion `data*.jar` files, copies original OBJ geometry, and decodes AVIF frames into PNGs at their original dimensions. The preparation report is `exports/weapon-assets.json`. It adds 144 OBJ files and 823 PNG texture frames to the existing mesh bundle. The shared parser audited all 463 OBJ files now present, including the original 319 wearable models.

The implementation follows the captured renderer's distinctions:

- `aDs` selects wood, stone, iron, gold, diamond and netherite sword textures, with separate GUI images.
- `aDq` uses a separate sword glow texture. The current port draws that additional component; its native tint and fullbright behavior remain to be finished for consistent cross-version parity.
- `aDm` uses bow geometry, a separate string, nocked arrow geometry and model-specific arrow offsets. Pull stages are 0 ticks, 1–13 ticks, 14–17 ticks and 18+ ticks.
- `aDl` adds animated bow components and a ping-pong frame clock. Models with separate flames or arrow tails are included.

These are captured runtime classes already preserved in the handoff. A fresh Windows capture was not needed to locate the weapons. Stock class captures and proprietary resource archives are local research inputs, excluded from the future source checkout.

`WeaponGeometry` shares component/frame selection between the four adapters. Sword and bow skins retain the item's normal gameplay behavior. Fabric uses an item-renderer mixin; Forge uses a narrowly targeted `RenderItem` transformer with vanilla rendering as the fallback. Forge item-renderer coremods and Fabric item-renderer mixins may conflict with other mods replacing the same render path; broader compatibility testing remains outstanding.

## Use

Open the browser with **K**, select **Swords** or **Bows**, and click a cosmetic. Hold the corresponding vanilla item. Its material selects the matching sword skin automatically. Bow strings advance as the item is drawn. GUI icons display dedicated cached art where present. The browser also displays weapon artwork without requiring a weapon in the player's hand.

The local held stack check prevents the selected skin from being applied to another player's held weapon. Dropped and framed items retain normal rendering. GUI items use the selected appearance. Vanilla sword material variants are supported; swords provided by other mods retain their own appearance.

Candle Sword (`SWORD 45`) and Woodland Elf Bow (`BOW 18`) lack dedicated GUI artwork in this cache. Their item icon falls back to the 3D model. The browser uses the available base texture for those two entries.

## Validation and limits

The production audit parses all 62 prepared packs, validates 409 component selections over the four bow pull stages, checks all 823 texture-frame paths, and parses cached sibling geometry frames. Pillow fully decoded all 823 PNG files. Runtime fixtures exercise an equipped Katana Sword and Cupid Bow, hotbar GUI icons, held rendering and actual item use. See [validation](validation.md) for release-specific results.

This establishes working weapon support, not pixel-for-pixel equivalence for every cosmetic. Remaining work includes native first/third-person scale and orientation calibration, left-hand calibration, beam tint/fullbright consistency, legacy enchantment glint and cosmetic projectile rendering. Recovered special shader effects, beam bloom and native arrow flight transforms have not been claimed as complete. Fabric uses Minecraft's item glint consumer for held weapons; Forge custom meshes currently omit the enchantment glint pass.
