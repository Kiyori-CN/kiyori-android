# Browser Refactor Plan

## Goal

Turn the project into a browser-led product incrementally without changing UI behavior or existing features during the refactor itself.

The refactor should stay safe, reversible, and friendly to future feature porting from the local `hikerView` workspace.

## Constraints

- No UI redesign in the refactor phases.
- No intentional feature changes in the refactor phases.
- Prefer single-module internal reorganization first.
- Keep each migration batch small enough to compile and verify immediately.
- Avoid mixing structural refactors with new browser capabilities in the same batch.

## Reference Direction

`hikerView` is organized as a browser product with many capability modules.

This project should move in that direction gradually:

1. Align physical source layout with actual package names.
2. Consolidate browser-related code under a browser-first source tree.
3. Separate browser concerns from player-only concerns.
4. Introduce clean boundaries before any Gradle multi-module split.
5. Split modules only after package layout is stable.

## Target Shape

Near-term target inside the existing `:app` module:

- `com/android/kiyori/browser/...`
- `com/android/kiyori/player/...`
- `com/android/kiyori/remote/...`
- `com/android/kiyori/settings/...`
- `com/android/kiyori/shared/...`
- `com/android/kiyori/app/...`

Long-term target as Gradle modules:

- `:app`
- `:browser-core`
- `:browser-ui`
- `:browser-settings`
- `:browser-feature-download`
- `:browser-feature-adblock`
- `:player-core`
- `:shared`

## Migration Strategy

### Phase 1: Physical Path Correction

Purpose:

- Fix the mismatch between package names and on-disk folder paths.
- Reduce future rename noise.

Scope:

- Move browser source files whose package is already `com.android.kiyori.browser.*`
- Do not change packages.
- Do not split Gradle modules.
- Compile immediately after the move.

### Phase 2: Browser-Centric Package Consolidation

Purpose:

- Move browser-adjacent settings and support code closer to browser ownership.

Scope:

- Browser settings UI
- Browser permissions
- Browser downloads
- Browser compatibility diagnostics
- Browser engine abstraction

### Phase 3: Dependency Direction Cleanup

Purpose:

- Make browser code depend on shared utilities, not on unrelated player screens.

Scope:

- Extract shared helpers
- Remove cyclic or cross-domain references
- Introduce browser-facing facades where needed

### Phase 4: Multi-Module Split

Purpose:

- Prepare for large-scale browser feature growth and selective feature porting from `hikerView`.

Prerequisite:

- Phase 1 to 3 stable
- Package layout already matching intended ownership

## First Safe Batch

This batch is intentionally narrow:

1. Create `app/src/main/java/com/android/kiyori/`
2. Move the existing browser source tree from the old physical path into the matching physical path
3. Keep package names unchanged
4. Build and verify

Why this batch:

- High structural value
- Very low behavioral risk
- Easy to revert if needed

## What Not To Do Yet

- Do not introduce X5 in the same batch
- Do not split into many Gradle modules yet
- Do not move all `compose` files at once
- Do not rename every root activity in one pass
- Do not mix browser feature development into the same commit/batch

## Next Suggested Batch

After Phase 1 is stable:

1. Move browser settings-related code into a browser-owned area
2. Introduce a `browser/engine` boundary around the current system WebView implementation
3. Start collecting browser capability gaps against `hikerView`
