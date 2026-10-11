# Differential reference fixtures

These fixtures are compiled only by `tools/audit-native-motion.ps1`, never into the mod.

`aGi`, `aGg`, `aGh` and `aGd` retain the recovered CFR controller, pose and parser code. Rendering and logging dependencies are inert stubs. `NativeRibbonReference` retains the original aCR geometry method before its GL upload. `NativeCurvedReference` retains the aCV geometry builder with allocation and temporary-vector dependencies replaced by Java/JOML equivalents.

The original captured class hashes and corresponding CFR files are recorded in `exports/renderer-decompilation-index.json`. These are source-level differential checks, not a claim that Minecraft's attachment transforms or final images are identical.
