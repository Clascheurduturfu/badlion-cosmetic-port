# Portable reverse engineering tools

The scoped runtime capture is already in `reference/cosmetic-port-0.3.6-runtime-capture.zip`. The original, verified CFR 0.152 JAR is included as `reference/cfr-0.152.jar`; it was obtained from the official benf.org release and checked against the published MD5 during the original investigation.

On the Mac, from the extracted project root:

```sh
python3 tools/decompile-portable.py
python3 tools/decompile-portable.py aCH aMI
```

The first command restores and SHA256-verifies all 6,175 captured classes into hash-prefixed directories and builds a combined reference classpath. The second repeats verification and runs CFR for the two named pet classes, using `JAVA_HOME` or Java on PATH. It normalizes the Windows path separators recorded in the original capture manifest, so this does not depend on Windows paths. Recovered Java reconstructions were already preserved under `reference/decompiled-renderer` for immediate reading.

The new portability helper has been syntax-checked on Windows; its Mac execution is not yet tested. The original `decompile_renderer.py` and Windows capture/instance helpers remain as historical tools and use Windows-specific paths. Use this helper for the transferred capture.

`HANDOFF-MANIFEST.json` lists the main archive contents. If present, `HANDOFF-REFERENCE-MANIFEST.json` lists the supplementary portable decompiler entries added after the main pack. The downloadable outer SHA256 covers the entire final ZIP, including both manifests.
