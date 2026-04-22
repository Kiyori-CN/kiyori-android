# AGENTS

## Build Output

- Default debug APK output directory: `app/build/outputs/apk/debug/`
- When reporting or locating a freshly built debug APK, use `D:\10_Project\kiyori-android\app\build\outputs\apk\debug`
- After each code change by default, run a debug APK build and ensure the output is produced in `D:\10_Project\kiyori-android\app\build\outputs\apk\debug`
- Do not use any other output directory when reporting packaged APK results unless the user explicitly asks for a different build target

## Icon Usage

- Canonical black icon library source: `icons`
- Do not move this folder unless the repo structure is being reorganized globally. Its current root-level location is stable and easy to find.
- For future black icon buttons, prefer assets from `icons` instead of Material icons or newly drawn replacements.
- The current `icons/` layout is flat at the top level: most entries are per-icon folders named in Chinese, and each standard icon folder contains the same icon exported as `svg`, multiple `png` sizes, `webp`, `ico`, and a white-background `png`.
- `icons/软件图标/kiyori-app-icon-1024.png` is the launcher icon source file for the app brand icon and should be treated separately from the standard UI icon folders.
- For Android UI, select the needed asset from the target icon's own folder; do not assume legacy folders such as `icons/android-xml/`, `icons/png/`, or `icons/svg/` still exist.
- Only copy the specific icons that are actually used by the app into `app/src/main/res/drawable/`; do not duplicate the whole library into the app module.
- When copying an icon into app resources, give it a `ic_kiyori_...` name to avoid collisions with existing drawables.
- Keep these icons monochrome black unless the design explicitly requires another color.
- Prefer the icon folder's same-name SVG as the editable source when converting for Android; use the bundled PNG exports only when a bitmap asset is the better fit.
