# Credits, license and provenance

## Proposed project credit
- **Fork author and enhancements:** Zelyff
- **RedProtect original creator:** ikillforeyou
- **RedProtect upstream maintainer:** FabioZumbi12
- **GP-inspired interaction concept:** GriefPrevention project (give attribution where code is actually reused).

## License rules
- RedProtect upstream README/repository declares **GNU GPL-3.0**. Keep the original LICENSE, source headers, copyright and attribution intact; disclose modifications and make corresponding source available as the license requires when redistributing.
- Do not state that Zelyff created the *original* RedProtect. In plugin metadata, adding `Zelyff` as contributor/maintainer is fine, but preserve upstream credits.
- Confirm license terms and specific borrowed files if directly copying GPL GP code. Prefer an independently implemented adapter of documented behavior over a blind source copy.
- Verify dependencies' redistribution rights and notice requirements before shading/packaging them.
- Avoid using 'official RedProtect' branding when distributing the fork; use an explicit unofficial community fork note.

## Proposed README snippet
> RedProtect — Zelyff Fork is an independent, modified version of RedProtect. Original RedProtect was created by ikillforeyou and maintained by FabioZumbi12. Enhancements by Zelyff include optional shovel/stick workflows, border highlighting, YAML configuration migration, and Bedrock Forms. Not affiliated with or endorsed by original maintainers.

## Proposed version metadata
`RedProtect 8.1.4-zelyff.1` (placeholder until upstream pin); plugin name remains `RedProtect` by default. Put fork revision/build SHA into separate diagnostic output. Do not conflate upstream 8.2.0 snapshots with 8.1.4 release.
