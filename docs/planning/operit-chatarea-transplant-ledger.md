# Operit ChatArea Transplant Ledger

## Purpose

Keep ChatArea migration decisions explicit so later rounds do not re-check the same source boundaries or drift back into local replica UI.

Source baseline:

- `D:\10_Project\Operit\app\src\main\java\com\ai\assistance\operit\ui\features\chat\components\ChatArea.kt`

Kiyori host:

- `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaChatContent.kt`

## Current Direction

The current route is `transplant by dependency slices`.

Kiyori local UI should keep shrinking toward:

- content shell assembly
- header overlay assembly
- ViewModel callback wiring
- compatibility model mapping

Operit-source-package bridges should own:

- ChatArea state behavior
- pagination/window behavior
- scroll navigation behavior
- message item/action behavior
- editor and multi-select overlays
- source-style popup/dialog compatibility wrappers

## Migrated Source Behavior

### Content Shell

Bridge:

- `ChatScreenContentWorkbenchBridge.kt`

Source behavior covered:

- header/content stacking
- header overlay mode
- measured header height -> message top padding
- white content surface preservation

Remaining:

- full `ChatScreenContent` overlay coordination
- history/character/workspace rollback interaction paths

### ChatArea Workbench

Bridges:

- `ChatAreaWorkbenchBridge.kt`
- `ChatAreaWorkbenchControllerBridge.kt`
- `ChatAreaPaginationBridge.kt`

Source behavior covered:

- `newestVisibleDepth` / `oldestVisibleDepth` window state
- pagination window calculation
- load older / load newer history controls
- pending jump can reopen hidden pages before scrolling
- `onHasHiddenNewerMessagesChange` compatibility callback
- multi-select state pruning/toggle helpers

Remaining:

- real stream-first-chunk behavior for blank AI messages
- full `autoScrollToBottom` external contract
- stronger equivalence to original `ChatMessage` pagination triggers, including summary messages

### Scroll Navigation

Bridge:

- `ChatScrollNavigatorBridge.kt`

Source behavior covered:

- viewport height measurement
- message anchor map
- centered-message locator chip
- message locator dialog
- jump-to-message callback path

Remaining:

- exact source styling differences after Material3 compatibility is resolved

### Message List

Bridge:

- `ChatAreaMessageListBridge.kt`
- `ChatAreaLoadingIndicatorBridge.kt`
- `ChatAreaDisplayPreferencesBridge.kt`

Source behavior covered:

- vertical scroll message list
- top content padding
- horizontal padding parameter
- pagination rows
- loading dots row
- `ChatStyle.CURSOR` / `ChatStyle.BUBBLE` loading-indicator offset branch
- source-style display preferences are now carried through the ChatArea subtree as one compatibility object
- source-style thinking/status/tool-collapse preferences now reach the markup renderer path
- bottom spacer

Remaining:

- exact item spacing parity
- summary row handling
- full source style dispatch into `CursorStyleChatMessage` / `BubbleStyleChatMessage`

### Message Item And Actions

Bridges:

- `ChatAreaMessageItemBridge.kt`
- `style/cursor/UserMessageComposableBridge.kt`
- `style/cursor/AiMessageComposableBridge.kt`
- `ChatMessageActionSurfaceBridge.kt`
- `ChatAreaActionControllerBridge.kt`
- `ChatMessageActionMenuBridge.kt`
- `ChatMessageActionDialogsBridge.kt`
- `part/ThinkToolsXmlNodeGrouperBridge.kt`
- `part/ChatMarkupParserBridge.kt`

Source behavior covered:

- user / AI / system item dispatch
- hidden user placeholder display
- long-press context menu host
- copy preview bottom sheet
- hidden user message dialog
- message info dialog
- edit / regenerate / rollback / reply / insert summary / branch callbacks
- AI variant footer
- token/timing footer
- cursor user message parsing boundary now mirrors source `UserMessageComposable` for memory/proxy/reply/workspace/trailing attachment tags
- cursor hidden-user placeholder rendering moved into the cursor user-message bridge
- cursor AI message rendering boundary now mirrors source `AiMessageComposable` for the `Response` title row, role/model/provider detail-text construction, timestamp-keyed static content branch, and direct markdown body padding
- think/tools consecutive XML grouping now mirrors the source `ThinkToolsXmlNodeGrouper` collapse rules for `READ_ONLY`, `ALL`, and `FULL`
- think/tools grouping logic is now isolated in a source-style bridge instead of living inside `ChatMarkupRendererBridge.kt`
- lightweight markup parsing is now isolated in a source-style bridge, including source-compatible `tool_xxxx` / `tool_result_xxxx` tag normalization

Recent compatibility decision:

- Kiyori uses `composeBom = 2023.10.01`.
- Source `DropdownMenu(shape/containerColor/tonalElevation/shadowElevation)` parameters are not available.
- Do not scatter fallback code.
- Use `OperitDropdownMenuBridge.kt` as the compatibility point for dropdown menu API differences.
- Source `DisplayPreferencesManager` is not imported wholesale yet; use `ChatAreaDisplayPreferencesBridge.kt` as the compatibility point for `showModelProvider`, `showModelName`, `showRoleName`, `showMessageTokenStats`, and `showMessageTimingStats`.
- Source `ToolCollapseMode` is not imported wholesale yet; use `ToolCollapseModeBridge` inside `ChatAreaDisplayPreferencesBridge.kt` to keep grouping behavior source-shaped without pulling the full preference stack.

Remaining:

- expose a visible source-style preference toggle surface for display preferences
- improve copy cleanup toward exact `cleanXmlTags(...)`
- replace the lightweight parser bridge with the real `StreamMarkdownRenderer` / `ThinkToolsXmlNodeGrouper` stack after markdown runtime dependencies are bridged
- restore exact menu shape/color/elevation either through a future BOM upgrade or a wrapper implementation
- align `createBranch` source semantics; current bridge callback still passes local index even though message timestamps are now available
- role/model/provider/timestamp now flow through the Kiyori compatibility message model; remaining work is persistence and real source manager mapping
- replace lightweight attachment tags with real source attachment/image preview behavior after attachment viewer dependencies are bridged

### Message Footer

Bridge:

- `ChatMessageFooterBridge.kt`

Source behavior covered:

- AI-only footer visibility gate from `ChatArea.kt`
- variant left/right switch row
- token stats compact summary
- timing stats compact summary
- source-style `LocalContext` + `remember(...)` formatting path for stats strings
- source-style display gate is now compatibility-state-backed instead of hardcoded in the action surface

Remaining:

- persist display preferences through a source-style settings/DataStore bridge instead of UiState defaults only
- switch to source auto-mirrored arrow icons after Compose/Material dependency compatibility is confirmed

## Kiyori Compatibility Boundaries

Adapters should live outside visible UI files.

Current target:

- move `OperitReplicaMessage -> ChatActionSurfaceMessage` mapping out of `KiyoriOperitReplicaChatContent.kt`

Preferred location:

- `app/src/main/java/com/android/kiyori/operitreplica/bridge/`

Reason:

- this mapping depends on Kiyori-local models
- source-package UI bridges should not need to import Kiyori models when a compatibility adapter can isolate that dependency

## Next Execution Targets

1. Extract ChatArea message model adapter from `KiyoriOperitReplicaChatContent.kt`.
2. Continue replacing `ChatAreaMessageItemBridge.kt` internals with source-structured `MessageItem` behavior.
3. Add a source-style display-preferences bridge for message token/timing stats visibility.
4. Only after ChatArea item behavior is closer, resume deeper `AgentChatInputSection` manager wiring.

## Per-Round Checks

Before finishing each round:

- no new Kiyori-only imitation widget for an existing Operit source behavior
- no ChatArea behavior moved back into `KiyoriOperitReplicaChatContent.kt`
- any source/API mismatch gets one compatibility bridge or ledger note
- run `.\gradlew.bat assembleDebug` after code edits
- report APK from `D:\10_Project\kiyori-android\app\build\outputs\apk\debug`
