# AGENTS

## Icon Usage

- Canonical black icon library source: `icons`
- Do not move this folder unless the repo structure is being reorganized globally. Its current root-level location is stable and easy to find.
- For future black icon buttons, prefer assets from `icons` instead of Material icons or newly drawn replacements.
- Prefer `icons/android-xml/` first for Android UI, because those assets can be copied directly into `app/src/main/res/drawable/`.
- Only copy the specific icons that are actually used by the app into `app/src/main/res/drawable/`; do not duplicate the whole library into the app module.
- When copying an icon into app resources, give it a `ic_kiyori_...` name to avoid collisions with existing drawables.
- Keep these icons monochrome black unless the design explicitly requires another color.
- If an exact icon is missing from `android-xml/`, use the matching asset from `icons/png/` or `icons/svg/` only when necessary.
