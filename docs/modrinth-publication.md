# Modrinth submission — 2026-10-11

[Badlion Cosmetic Port](https://modrinth.com/mod/badlion-cosmetic-port) was successfully submitted for moderator review under the existing Modrinth account. The moderation page displayed **Under review** and confirmed that the project was in the review queue. Approval has not been claimed.

## Uploaded files

All four files match the production-audited SHA256 values in [the release manifest](../release/manifest.json). Each is a separate **beta** version with one exact Minecraft version and its correct loader:

| Target | Modrinth version number | Version ID |
| --- | --- | --- |
| Forge 1.8.9 | `0.5.0-forge-1.8.9` | `JTX5O3gp` |
| Forge 1.12.2 | `0.5.0-forge-1.12.2` | `NVunCLyu` |
| Fabric 1.20.1 | `0.5.0-fabric-1.20.1` | `CpjCbthP` |
| Fabric 1.21.1 | `0.5.0-fabric-1.21.1` | `zr2nDJ3e` |

Fabric API is recorded as a required dependency on both Fabric versions. All four versions are client-side only. Java and tested loader/API versions are stated in the project description. The Windows native DLL is not uploaded as a mod version.

## Page and disclosures

The description documents the eight cosmetic categories, controls, installation, test scope, roadmap and remaining visual work. It prominently states that the approximately 4.3 GB prepared asset bundle is external: the JAR alone cannot provide the cosmetic collection. Cosmetic models, textures, original archives, accounts and runtime captures were not uploaded. Users must prepare or supply the separate bundle.

The source, issue tracker and documentation link to this repository. Decoration and Equipment tags are selected, with Decoration featured. A real Fabric 1.20.1 browser screenshot from the production audit is featured in the gallery; its caption repeats the separate asset requirement. No AI-generated promotional image was used.

The page has **Contains AI-generated code and text** and **Contains derivative content** enabled. Its explanation describes Codex's code adaptation, porting, validation, documentation and page preparation. Badlion's original work is credited. There is no in-game generative AI service, telemetry or paid unlock feature in the standalone mod.

The license selection is **All Rights Reserved / No License**, consistent with the source repository having no project-wide reuse license. Dependency licenses and original creators' rights remain applicable; this selection does not grant rights to redistribute their assets.

## Visibility and next step

The submission uses **Unlisted** visibility. Modrinth's [AI guidance](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai) restricts public discovery for predominantly AI-generated projects and assesses AI-assisted forks by their new contribution. It indicates that unlisted publication may still be permitted. The project's disclosures were retained rather than presenting the adaptations as human-authored.

Modrinth moderators now control approval. Once approved, an unlisted project can be accessed by its URL but does not appear in search or on the public profile. Any moderator request should be handled in the project dashboard; do not describe this submission as an already-approved public listing. No automated monitoring was configured.

Local submission evidence is saved at `exports/modrinth-submitted-2026-10-11.jpg`; it is excluded from source history. [The submission record](../release/modrinth-submission.json) preserves the version identifiers and artifact hashes without account credentials.
