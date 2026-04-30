# Operit Source Transplant Bridge Checklist

## Goal

Stop building the first screen as a visual imitation.

Target the real `Operit` chat first-screen subtree and let `kiyori` provide only the host compatibility needed to run it.

## Why direct copy is not one-step

The source is visible, but the first screen is not a single isolated Composable.

It is a dependency cluster:

- `AIChatScreen.kt`
  - app-level state collection
  - preferences
  - floating-window permission flow
  - workspace/computer overlays
  - shared-file intake
- `ChatScreenContent.kt`
  - message editing
  - multi-select
  - export flow
  - history overlay
  - workspace rollback preview
  - gesture coordination
- `ChatScreenHeader.kt`
  - `ChatViewModel`
  - active prompt managers
  - character-card/group managers
  - floating-window launcher
  - avatar preference flows
- `AgentChatInputSection.kt`
  - `ChatViewModel`
  - attachment system
  - fullscreen input
  - pending queue
  - model config managers
  - function config managers
  - preference profile/model lookup
  - tool-progress bus
  - permission-level system

So “照抄” is correct as a direction, but not correct as a literal one-file paste. The subtree has to be transplanted in dependency order.

## Current status

### Completed

- first-screen seed messages removed from active state path
- first-screen top row started migrating from local imitation to original source subtree
- transplanted original component:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatHeader.kt`
- transplanted original input subcomponents:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/AttachmentChip.kt`
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/FullscreenInputDialog.kt`
- transplanted source-faithful input shell bridge:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentChatInputWorkbenchBridge.kt`
- local bridge already supplied:
  - history strings
  - floating-window strings
  - local avatar resource mapping
  - local click callbacks
  - local token ring state
  - lightweight local `AttachmentInfo` model
  - string-to-`TextFieldValue` fullscreen bridge

### Confirmed build

- `.\gradlew.bat assembleDebug`
- output:
  - `D:\10_Project\kiyori-android\app\build\outputs\apk\debug\app-arm64-v8a-debug.apk`

## Required bridge layers

### Bridge A: header host bridge

Purpose:

- feed original header subtree with local state instead of `Operit` managers

Needs:

- active character label
- active avatar source
- history toggle state
- floating button callback
- running task count
- token statistics

Status:

- partially done
- `ChatHeader` is already running
- `ChatScreenHeader` full source contract is not yet transplanted

### Bridge B: input workbench bridge

Purpose:

- let `AgentChatInputSection` run without the full original `ChatViewModel`

Needs:

- draft text state
- processing state
- attachment list model
- reply-to message model
- feature toggles
- permission level
- queue state
- model label / model config bridge
- attachment callbacks
- fullscreen editor callbacks

Risk:

- this is the heaviest visible dependency slice of the first screen

Status:

- first bridge layer is now running
- local `KiyoriOperitReplicaInputBar.kt` no longer owns the visible input-workbench layout directly
- the visible shell now sits in the source package:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentChatInputWorkbenchBridge.kt`
- this bridge already hosts:
  - source-like text field shell
  - source-like model pill / settings / add / action-button row
  - transplanted `AttachmentChip`
  - transplanted `FullscreenInputDialog`
- first behavior bridge is now running:
  - model-selector popup open/close state
  - local model-label selection
  - processing label + progress ring
  - cancel-processing action path
- queue bridge is now running:
  - pending queue panel
  - processing-while-drafting switches the action button into queue-add behavior
  - queued items can be edit/delete/send
  - completed processing auto-continues into the next queued prompt
- extra-settings popup bridge is now running:
  - source-package popup bridge added:
    - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentExtraSettingsPopupBridge.kt`
  - settings popup ownership moved out of local overlay imitation and back into the input workbench path
  - popup layout now follows the `AgentChatInputSection` bottom-end popup structure instead of the old local left-bottom card
  - compatibility state added for:
    - current memory profile label
    - memory auto update
    - auto read
    - auto approve
    - disable stream output
    - disable user preference description
    - disable status tags
- model-selector popup bridge is now running:
  - source-package popup bridge added:
    - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentModelSelectorPopupBridge.kt`
  - old inline `DropdownMenu` path removed from the visible first-screen workbench
  - model popup now uses the same bottom-end popup ownership pattern as the real `AgentChatInputSection`
  - first-pass compatibility state added for:
    - thinking mode
    - thinking quality level
    - max-context mode
    - base/max context length display
- attachment popup bridge is now running:
  - source-package popup bridge added:
    - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AttachmentSelectorPopupPanelBridge.kt`
  - old page-level `KiyoriOperitReplicaAttachmentPanel` is no longer the visible first-screen attachment path
  - attachment popup now follows the same bottom-end popup ownership pattern as model/settings
  - current bridge scope is visual/layer ownership first:
    - popup size and row rhythm follow the source `AttachmentSelectorPopupPanel`
    - selection currently feeds local compatibility attachment labels rather than full file/camera flows
- remaining gap:
  - full `AgentChatInputSection` state machine, real attachment flows, processing states, queue/cancel, permission/model managers

### Bridge C: content shell bridge

Purpose:

- move from local message-column replica to the real `ChatScreenContent` composition contract

Needs:

- message model mapping
- scroll state
- history overlay state
- gesture state
- edit/delete/regenerate callbacks
- export and share policy decisions
- workspace rollback preview stubs or bridges

Risk:

- `ChatScreenContent` is not just a list container; it is the main interaction shell

Status:

- first visible shell bridge is now running
- source-package shell bridge added:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatScreenContentWorkbenchBridge.kt`
- source-package visible behavior component added:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ScrollToBottomButton.kt`
- source-package pagination helper added:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatAreaPaginationBridge.kt`
- local `KiyoriOperitReplicaChatContent.kt` no longer owns the top-level header/content stacking directly
- the first-screen content shell now follows the `ChatScreenContent` overlay pattern:
  - header rendered as an overlay layer
  - message area receives measured top padding from header height
  - empty state preserves header rhythm instead of collapsing into a plain column start
- first-pass `ChatArea` visible behavior is now bridged:
  - local scroll state is wired into transplanted `ScrollToBottomButton`
  - auto-scroll to bottom resumes when the list reaches the end again
- first-pass pagination window behavior is now bridged:
  - local message area no longer always renders the full message list
  - visible window now exposes source-style `load more history` / `load newer history` controls
  - scroll-to-bottom button now respects whether newer pages are hidden
- remaining gap:
  - real `ChatArea` behavior
  - message-level context menu / edit / select / rollback flows
  - message editing / multi-select / export / rewind / rollback systems

### Bridge D: overlay bridge

Purpose:

- support the original layering assumptions of history, character, workspace, computer

Needs:

- history left drawer lifecycle
- character selector lifecycle
- workspace keep-alive lifecycle
- computer detach lifecycle

Status:

- local overlay lifecycle direction is already close
- real source components are not yet transplanted

## Recommended transplant order

1. `ChatHeader.kt`
2. `ChatScreenHeader` visual contract
3. `AgentChatInputSection.kt`
4. `ChatScreenContent.kt` visible shell
5. history panel source alignment
6. workspace/computer source alignment

## Hard rules

- do not add new handwritten “Operit-like” widgets if an original source component can be transplanted instead
- do not redesign spacing, icon order, or control hierarchy for convenience
- do not pull `Operit` app-shell files into `HomeScreen`
- keep `kiyori` compatibility inside bridge/state/resource layers, not inside ad-hoc UI rewrites

## Next execution target

Build the next bridge around `AgentChatInputSection`.

Next concrete slice after this round:

1. replace the simplified model dropdown with a source-faithful model selector popup bridge
2. keep shrinking local overlay ownership so input popups belong to the source-package workbench path
3. replace local compatibility attachment actions with source-faithful file/camera/screen/memory flows where feasible
4. then continue toward `ChatScreenContent` shell transplant

## 2026-04-30 Chat-area navigator checkpoint

- the first-screen chat area now has a source-package locator/navigation bridge:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatScrollNavigatorBridge.kt`
- this bridge is copied from the `Operit` `ChatScrollNavigator` interaction path, but adapted to a lightweight compatibility message model so `kiyori` does not need the full original `ChatMessage` stack yet
- `KiyoriOperitReplicaChatContent.kt` now follows the same control pattern as `Operit` `ChatArea` for scroll navigation:
  - viewport height is measured from the real message surface
  - each visible message row reports an anchor position
  - jump-to-message no longer assumes a flat full-list scroll
  - hidden pages are reopened before the jump when the target message is outside the current pagination window
- supporting pagination helpers were extended in the source package:
  - `findChatAreaPaginationDepthForIndex(...)`
  - `resolveChatAreaWindowDepthsForTargetPage(...)`
- this matters because the next visible mismatch is no longer just message bubble styling
- the remaining gap is the real `ChatArea` interaction stack:
  - message edit/regenerate/rollback/select actions
  - hidden placeholder handling
  - loading indicator and more exact message spacing rhythm

## 2026-04-30 Chat-area loading checkpoint

- the source `ChatArea` loading animation is now also mirrored into the source package:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/LoadingDotsIndicator.kt`
- `KiyoriOperitReplicaChatContent.kt` now shows this source loading indicator in the same message-flow region used by `Operit`, instead of relying only on the bottom input processing state
- current compatibility rule for this slice:
  - while the replica conversation is processing and the newest visible message is the latest user message, render the source loading dots directly under the chat stream
- this keeps the migration direction correct:
  - chat progress feedback belongs to `ChatArea` behavior
  - it should not be recreated as a custom input-only affordance

## Updated next execution target

The input workbench still needs deeper source alignment, but the most direct first-screen fidelity target after this round is now the rest of `ChatArea`.

Next concrete slice after this round:

1. transplant the `ChatArea` loading indicator behavior from source
2. continue mapping `ChatArea` message-level controls instead of re-styling local bubbles
3. only after the chat-area interaction shell is closer, resume deeper `AgentChatInputSection` manager wiring

Updated next concrete slice:

1. keep collapsing local message rendering toward `ChatArea` item behavior
2. target message-level action surfaces next: edit / regenerate / rollback / select
3. keep deferring purely cosmetic bubble retuning until the source interaction shell is closer
