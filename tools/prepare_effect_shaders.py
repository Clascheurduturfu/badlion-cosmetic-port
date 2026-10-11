"""Adapt captured aCv shader literals to Minecraft's GLSL 150 entity pipeline."""
import hashlib
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "work/decompiled-renderer/fdacf3fb8075/net/badlion/a/aCv.java"
text = source.read_text()

def literal(method):
    start = text.index("private static String " + method)
    end = text.index("\n    }", start)
    body = text[start:end]
    expression = body[body.index("return ") + 7:].rstrip().removesuffix(";")
    # The reconstructed Java expression substitutes its fragment output variable.
    expression = expression.replace('" + string + "', 'fragColor')
    return json.loads(expression)

definitions = literal("bFf()")
effect = literal("hE(boolean bl)").replace("outTexCoord", "texCoord0")
jar = next((ROOT / "mod/.gradle/loom-cache/minecraftMaven").rglob("*.jar"))
destination = ROOT / "mod/src/main/resources/assets/badlioncosmeticport/shaders/core"
destination.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(jar) as archive:
    prefix = "assets/minecraft/shaders/core/rendertype_entity_translucent"
    config = json.loads(archive.read(prefix + ".json"))
    fragment = archive.read(prefix + ".fsh").decode()
fragment = fragment.replace("void main() {", definitions + "\nvoid main() {\n" + effect)
(destination / "cosmetic_fx.fsh").write_text(fragment)
config["vertex"] = "minecraft:rendertype_entity_translucent"
config["fragment"] = "badlioncosmeticport:cosmetic_fx"
config["uniforms"] += [
    {"name": "iTime", "type": "float", "count": 1, "values": [0]},
    {"name": "shieldFX", "type": "int", "count": 1, "values": [0]},
    {"name": "bandanaFX", "type": "int", "count": 1, "values": [0]},
]
for name in ("shield_fx", "bandana_fx"):
    (destination / (name + ".json")).write_text(json.dumps(config, indent=2))
index = json.loads((ROOT / "exports/renderer-decompilation-index.json").read_text())
report = {"capturedClassSha256": index["net/badlion/a/aCv"]["sha256"], "reconstructedSourceSha256": hashlib.sha256(source.read_bytes()).hexdigest(),
          "effectFragmentSha256": hashlib.sha256(fragment.encode()).hexdigest(), "adaptation": "aCv bFf/hE literals; outTexCoord renamed texCoord0; vanilla entity lighting and fog retained",
          "specialCosmetics": {"SHIELD": 250, "BANDANA": 345}}
(ROOT / "exports/effect-shader-provenance.json").write_text(json.dumps(report, indent=2))
print("Prepared captured shield/bandana shader effects")
