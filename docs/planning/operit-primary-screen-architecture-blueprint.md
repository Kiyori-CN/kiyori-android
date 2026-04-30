# 正一屏 Operit 架构蓝图

## 文档目的

这份文档不是“想法清单”，而是后续正一屏完美复刻的实施蓝图。

它回答四个问题：

1. `Operit` 的真实聊天页到底由哪些层组成
2. `kiyori` 应该照抄哪些结构，哪些上层壳不该硬搬
3. `kiyori` 里应该如何落包、落状态、落组件
4. 后续每一轮重构应该先拆什么，后拆什么，验收什么

## 研究结论

### 1. `Operit` 真正的入口链

`Operit` 的聊天首页不是单一页面，而是一条完整应用链路：

1. `ui/main/MainActivity.kt`
2. `ui/main/OperitApp.kt`
3. `ui/main/layout/PhoneLayout.kt` / `TabletLayout.kt`
4. `ui/main/components/AppContent.kt`
5. `ui/main/screens/OperitScreens.kt`
6. `Screen.AiChat`
7. `ui/features/chat/screens/AIChatScreen.kt`

### 2. `kiyori` 不能照搬上层壳

`kiyori` 的正一屏只是 `HomeScreen` 中 pager 的一个子页，不是应用根导航页。

因此：

- `MainActivity`
- `OperitApp`
- `PhoneLayout`
- `AppContent`
- `Screen` 导航系统

这些都不能整包照抄。

真正应该完美复刻的起点，是 `AIChatScreen` 及其下游聊天子系统。

### 3. 真正要复刻的是“聊天工作台子系统”

从 `Operit` 当前实现看，正一屏核心不是“一个聊天页面”，而是一套并列协作的子系统：

- 聊天消息区
- 顶部状态区
- 历史侧滑面板
- 角色选择面板
- 输入工作台
- 附件与输入扩展面板
- workspace 覆盖层
- computer 覆盖层
- toast / dialog / popup 系统
- 会话状态与输入状态调度系统

如果只抄视觉，而不抄这些层的职责边界，最终一定会越改越不像 `Operit`。

## `Operit` 核心结构拆解

### A. 页面总装层

`AIChatScreen.kt`

关键骨架：

- `CustomScaffold(...)`
- `ChatScreenContent(...)`
- `ChatInputBottomBar(...)`
- `CharacterSelectorPanel(...)`
- `ChatHistorySelectorPanel(...)`
- `WorkspaceScreen(...)`
- `ComputerScreen(...)`

这个文件不是“一个组件”，而是页面装配器，负责：

- 收集 ViewModel 状态
- 决定显示哪个覆盖层
- 组织 header / content / input / overlays 的层级
- 处理 IME 位移和底栏测量

### B. 内容区

`ChatScreenContent.kt`

它负责：

- Header 与消息区的覆盖/非覆盖关系
- 消息区滚动
- 消息多选、编辑、删除、回滚、分享
- 历史面板的遮罩层与左滑面板
- 角色面板开关
- Header 高度与消息区 top padding 协调

这说明 `ChatScreenContent` 不是单纯消息列表，而是“主内容层管理器”。

### C. Header 区

`ChatScreenHeader.kt`
`ChatHeader.kt`

它负责：

- 左侧历史入口
- PiP / 浮窗入口
- 当前角色卡展示
- 顶部标题与模型/角色信息
- 右侧上下文统计入口

Header 在 `Operit` 里不是静态标题栏，而是状态密集入口层。

### D. 输入区

`AgentChatInputSection.kt`

这是完美复刻最容易被低估、但最不能随便简化的部分。

它负责：

- 输入框本体
- reply preview
- attachment chips
- attachment popup
- send / cancel / queue
- thinking / tools / memory / max-context / auto-read / permission 等开关
- fullscreen input
- pending queue
- token limit / model selector / extra settings popup

也就是说，底部输入区本质是一个“输入工作台”。

### E. 覆盖层

`AIChatScreen.kt`
`WorkspaceScreen.kt`
`ComputerScreen.kt`
`CharacterSelectorPanel.kt`

重点不是“有没有弹层”，而是弹层生命周期：

- 历史面板：左滑进入，带遮罩
- CharacterSelector：浮层面板
- Workspace：隐藏时仍保活组合状态，但布局尺寸可压到 0
- Computer：关闭时直接移出组合，确保底层资源释放

这里的层级和生命周期，后面必须照抄，不能只做“show/hide 一个 Box”。

## `ChatViewModel` 的真实状态结构

`Operit` 聊天页不是一个大状态对象，而是多个 delegate 汇总。

### 1. 配置与开关层

来源：

- `ApiConfigDelegate`

状态范围：

- `apiKey`
- `apiEndpoint`
- `modelName`
- `apiProviderType`
- `featureToggles`
- `enableThinkingMode`
- `thinkingQualityLevel`
- `enableTools`
- `enableMemoryAutoUpdate`
- `enableMaxContextMode`
- `disableStreamOutput`
- `disableUserPreferenceDescription`
- `disableStatusTags`
- `summaryTokenThreshold`
- `maxWindowSizeInK`

结论：

- 顶部统计菜单
- 输入区设置按钮
- 模型标签

这些都不应该直接散落在 Composable `rememberSaveable` 里。

### 2. 会话与历史层

来源：

- `ChatHistoryDelegate`

状态范围：

- `chatHistory`
- `chatHistories`
- `currentChatId`
- `showChatHistorySelector`
- `chatHistorySearchQuery`
- `historyDisplayMode`

结论：

- 正一屏必须有独立的会话仓和当前会话状态
- 历史面板不是单次弹窗数据，而是长期状态系统

### 3. 输入处理层

来源：

- `MessageProcessingDelegate`
- `MessageCoordinationDelegate`

状态范围：

- `userMessage`
- `isLoading`
- `currentChatIsLoading`
- `currentChatInputProcessingState`
- `scrollToBottomEvent`
- `isSummarizing`
- `isSendTriggeredSummarizing`

结论：

- “发送中”
- “工具执行中”
- “总结中”
- “滚动到底部”

这些都要做成独立状态切片，不能继续用纯本地伪状态模拟。

### 4. 统计层

来源：

- `TokenStatisticsDelegate`

状态范围：

- `currentWindowSize`
- `inputTokenCount`
- `outputTokenCount`
- `perRequestTokenCount`

结论：

- 顶部圆环和统计菜单必须从统一统计状态切片读取
- `kiyori` 当前已完成首轮落地：
  - `contextUsagePercentage`
  - `currentWindowSize`
  - `inputTokenCount`
  - `outputTokenCount`
  - 上述字段已经由 `KiyoriOperitReplicaViewModel.withDerivedState()` 统一派生

### 5. 附件层

来源：

- `AttachmentDelegate`

状态范围：

- `attachments`
- `attachmentPanelState`

结论：

- attachments 不能再是单纯 `listOf("屏幕内容", "工作区")`
- 应该是独立附件模型与附件面板状态

### 6. 覆盖层与辅助能力层

状态范围：

- `showWebView`
- `isWorkspacePreparing`
- `showAiComputer`
- `showWorkspaceFileSelector`
- `workspaceFileSearchQuery`
- `workspaceCommandExecutionState`
- `replyToMessage`
- `errorMessage`
- `popupMessage`
- `toastEvent`

结论：

- workspace
- computer
- reply
- toast/dialog

都应单独建状态切片，不要混在页面主体状态里。

## `kiyori` 的目标架构

## 总原则

- `HomeScreen.kt` 只保留宿主挂载职责
- `operitreplica/` 内部形成独立子系统
- 先抄层级和状态，再抄视觉细节
- UI 组件只消费状态，不直接拥有业务状态

## 推荐目录结构

```text
app/src/main/java/com/android/kiyori/operitreplica/
  model/
    KiyoriOperitReplicaModels.kt
    KiyoriOperitHistoryModels.kt
    KiyoriOperitInputModels.kt
    KiyoriOperitOverlayModels.kt
  state/
    KiyoriOperitReplicaViewModel.kt
    KiyoriOperitReplicaUiState.kt
    KiyoriOperitHistoryState.kt
    KiyoriOperitInputState.kt
    KiyoriOperitWorkspaceState.kt
    KiyoriOperitHeaderState.kt
  bridge/
    KiyoriOperitHostBridge.kt
    KiyoriOperitWorkspaceBridge.kt
    KiyoriOperitAttachmentBridge.kt
  ui/
    KiyoriOperitReplicaScreen.kt
    components/
      KiyoriOperitHeader.kt
      KiyoriOperitChatContent.kt
      KiyoriOperitInputBar.kt
      KiyoriOperitHistoryPanel.kt
      KiyoriOperitOverlayHost.kt
      KiyoriOperitToastHost.kt
      message/
        KiyoriOperitChatArea.kt
        KiyoriOperitMessageBubble.kt
        KiyoriOperitSystemMessage.kt
      input/
        KiyoriOperitAgentInputSection.kt
        KiyoriOperitAttachmentPanel.kt
        KiyoriOperitFullscreenInput.kt
      overlay/
        KiyoriOperitCharacterSelectorPanel.kt
        KiyoriOperitWorkspacePanel.kt
        KiyoriOperitComputerPanel.kt
```

## 包职责说明

### `model/`

只定义：

- 会话模型
- 消息模型
- 输入菜单项
- overlay 显示模型
- token/统计展示模型

不放：

- Compose UI
- 业务逻辑

### `state/`

只定义：

- 页面主状态
- 会话状态
- 输入状态
- Header 状态
- Workspace 状态
- overlay 状态

它是未来 `Operit` 的 delegate 结构在 `kiyori` 中的等价物。

### `bridge/`

这是 `kiyori` 和 `Operit` 风格子系统之间的适配层。

因为 `kiyori` 不是 `Operit`，所以需要桥接：

- pager 宿主行为
- 本地附件/工作区能力
- 后续真实工具入口

桥接层的作用是隔离宿主差异，不让 `ui/` 组件直接知道 `HomeScreen` 或其它业务细节。

### `ui/`

只做三件事：

- 组装页面层级
- 渲染状态
- 分发事件回调

不负责：

- 生成假数据
- 保存页面核心状态
- 直接处理复杂业务

## 建议的状态切片

为了后面稳定拆分，建议 `ViewModel` 至少分成下面 7 个切片。

### 1. `HeaderState`

字段建议：

- `title`
- `subtitle`
- `avatarRes`
- `contextUsagePercent`
- `contextTokenText`
- `responseBudgetText`
- `showStatsMenu`

### 2. `HistoryState`

字段建议：

- `showHistoryPanel`
- `searchQuery`
- `displayMode`
- `sections`
- `activeConversationId`

### 3. `ChatState`

字段建议：

- `activeConversation`
- `messages`
- `isLoading`
- `processingState`
- `autoScrollToBottom`
- `replyToMessage`

### 4. `InputState`

字段建议：

- `draft`
- `attachments`
- `showAttachmentPanel`
- `showFullscreenInput`
- `showFeaturePanel`
- `showPendingQueue`
- `pendingQueueItems`

### 5. `FeatureToggleState`

字段建议：

- `enableThinking`
- `thinkingQualityLevel`
- `enableTools`
- `enableMemory`
- `enableMaxContext`
- `enableNotification`
- `enableWorkspace`
- `enableVoice`

### 6. `WorkspaceState`

字段建议：

- `showWorkspace`
- `isWorkspacePreparing`
- `showWorkspaceFileSelector`
- `workspaceFileSearchQuery`
- `workspaceCommandExecutionState`

### 7. `OverlayState`

字段建议：

- `showCharacterSelector`
- `showHistoryScrim`
- `showComputer`
- `toastMessage`
- `popupMessage`
- `errorMessage`

## 页面层级照抄规则

后续实现必须遵守以下层级顺序。

### 基础层

- 页面背景
- 主内容容器

### 内容层

- Header
- ChatArea
- 底部 InputBar

### 半覆盖层

- 历史遮罩
- 历史左滑面板
- CharacterSelectorPanel
- AttachmentPanel / FeaturePanel / FullscreenInput

### 全屏覆盖层

- WorkspacePanel
- ComputerPanel
- 全局 Preparing Overlay
- Toast / Dialog / Export / Popup

这个顺序不能乱。尤其：

- HistoryPanel 必须压在内容层之上
- Workspace 必须高于聊天页
- Computer 必须高于聊天页且可完全脱离组合

## 不能直接照搬的部分

以下是“结构参考”，不是“应直接复制源码”：

- `MainActivity` 中协议、权限、插件加载流程
- `OperitApp` 的全应用导航系统
- `PhoneLayout` 的抽屉导航与 app shell
- `AppContent` 的多页面缓存、toolbar、drawer 动画

原因：

- `kiyori` 正一屏只是局部页面，不是整个应用
- 如果整包搬运这些上层壳，只会把宿主系统耦死

## 必须尽量照抄的部分

以下是后面应尽量 1:1 对齐的区域：

- `AIChatScreen` 的页面装配顺序
- `ChatScreenContent` 的 header/content/history 关系
- `ChatScreenHeader` / `ChatHeader` 的结构
- `ChatArea` 的消息布局节奏
- `AgentChatInputSection` 的输入工作台布局
- `CharacterSelectorPanel` 的角色浮层结构
- `WorkspaceScreen` / `ComputerScreen` 的覆盖层策略

## 重构顺序

### 阶段 1

目标：

- 完成宿主隔离

当前状态：

- 已完成

标准：

- `HomeScreen.kt` 不再持有旧正一屏实现

### 阶段 2

目标：

- 抽离模型与状态

当前状态：

- 已完成第 1 步：会话列表 / 当前会话 / 消息映射 / 历史搜索词 已进入 `KiyoriOperitReplicaViewModel`
- 已补上首轮派生状态收敛：header 统计、输入区状态条、当前消息列表、当前会话、历史分组
- `Screen` 已去掉本地时间戳生成、历史可见集过滤和消息兜底逻辑
- overlay host 首轮结构已补齐：`CharacterSelector / Workspace / Computer / WorkspacePreparing`
- workspace 宿主已补上显式重建版本号：`workspaceReloadVersion`
- workspace 已升级为真实 WebView 宿主首版，不再只是说明占位层
- workspace 已具备会话级内部工作区目录、文件树和文本预览首版
- workspace 文件工作流已补上首轮对齐：
  - `Preview` 标签 + 打开文件标签
  - 标签栏右侧预览切换 / 保存动作区
  - 编辑态 / 预览态切换
  - 文件保存
  - 未保存关闭确认
  - HTML 文件首次打开默认预览
  - 文件树排序 / 隐藏文件 / 新建文件 / 新建文件夹 / 长按删除确认
  - 当前目录视角 / 返回上级 / 固定快捷路径 / 左侧目录面包屑
  - 文件预览面包屑 / Markdown 预览 / HTML WebView 预览
  - 图片 / 音频 / 视频 / PDF-Office 文档只读预览首版
- 角色与历史关系已开始落进状态层：
  - conversation `characterId`
  - character sort option
  - history display mode
  - auto-switch toggles
- 尚未继续切片的仍有：这些覆盖层背后的真实能力状态、workspace 文件态、computer 终端态，以及更细的 host bridge

当前优先级：

- 最高

标准：

- `rememberSaveable` 假状态逐步退出主屏文件

### 阶段 3

目标：

- 按 `AIChatScreen` 装配顺序拆组件

顺序：

1. `Header`
2. `ChatContent`
3. `InputBar`
4. `HistoryPanel`
5. `OverlayHost`

### 阶段 4

目标：

- 对齐真实 overlay 生命周期

标准：

- workspace 可保活
- workspace 可显式重建宿主，但不破坏主聊天状态
- workspace 文件工作流要继续向 `Operit WorkspaceManager` 对齐：
  - `openFiles`
  - `filePreviewStates`
  - `unsavedFiles`
  - close confirm
  - `FileManager` richer quick-path details / path bar behavior / image-audio-video-document preview routing

## Workspace shell blueprint notes 2026-04-29

For the current `Workspace` replica track, the next comparisons should keep using `Operit` `WorkspaceManager.VSCodeTab(...)` and `FileManager.FileListItem(...)` as the visual and interaction baseline.

Current shell decisions now locked in:

- keep `HomeScreen.kt` as host only
- keep workspace state inside `operitreplica/` host files
- keep conversation-scoped workspace roots until real external workspace binding is added
- keep `openRelativePaths / selectedRelativePath / unsavedRelativePaths / previewModes` as the local state bridge that mirrors the Operit workspace workflow

Current fidelity decisions now locked in:

- top tabs should behave like editor tabs, not rounded filter chips
- unsaved state should be represented in the close area, not appended to the filename text
- file rows should follow the actual `Operit FileManager` row simplicity; do not force custom `size / modified` metadata back into the list if the source row stays one-line
- file icons should be extension-aware so the left pane and top tabs feel like a real editor surface

Next workspace-specific passes should focus on:

1. tab spacing, close hit target, and active/inactive color rhythm against real Operit screenshots/code
2. path bar detail parity, including quick-path wording and breadcrumb emphasis
3. stronger native preview handling for image/audio/video/pdf-office content
4. external or user-selected workspace directory binding without breaking the existing overlay lifecycle

Additional stage-4 baseline:
- computer 可完全移出组合
- history 为遮罩 + 左滑面板，不是普通弹窗

Workspace navigation notes now locked:

- quick-path chips are not just text links; they should read like tool-entry chips with icons
- `Root / Notes / Prompts / Preview / Scratch` should stay stable as the first fixed local scaffold
- explorer rows should keep metadata plus a visible row rhythm so the pane feels like an editor explorer, not a plain settings list
- status summary should expose at least sort mode, current item count, and hidden-file state
- both path bars should use the same editor-like chip grammar rather than plain text buttons and slash separators
- visible path text should stay workspace-relative and should not leak the internal conversation storage folder name
- preview-header actions should use the same compact toolbar language as the tabs area rather than full text buttons
- the current file breadcrumb segment should be visually heavier than directory segments
- explorer title, path, actions, summary, and quick paths should read as one compact header block before the file list instead of five separate stacked sections
- after the structure is correct, continue tightening density by reducing padding/gaps/chip size before attempting any new workspace features
- when direct source comparison shows a previous visual refinement diverged from `Operit` interaction language, prefer correcting back toward the source even if the custom UI looked “cleaner”
- for the left file-manager header specifically, keep `path row + icon actions + quick paths` as the source-truth structure; any extra summary/breadcrumb rows must stay secondary
- for left header actions and quick paths, prefer matching the source component class and size rhythm (`IconButton`/`FilterChip`-style behavior) before inventing custom variants
- for top tabs and workspace action areas, do not reuse unrelated shared icon-button helpers if their hit area or sizing differs from `WorkspaceManager`; keep workspace-specific button rhythm
- workspace root wording must stay consistent across quick paths, breadcrumb root, and visible path text; do not mix `Root` and `Workspace`
- for secondary quick-path labels, prefer actual folder names over productized or decorative aliases unless the source project clearly names them otherwise
- dialog and read-only-state wording should prefer short file-manager/workspace verbs over explanatory product copy
- generated workspace seed content, empty-state landing html, and default preview html must stay file/task-oriented; do not let them explain that the page is a replica, host, stage build, or internal implementation
- default workspace-visible content should not leak internal container naming such as conversation storage folder identifiers unless the source project clearly exposes that path
- if legacy generated files already contain old replica-oriented copy, migrate them with marker-based replacement instead of blind overwrite so user edits remain intact
- when current `Operit` source and a previous “editor enhancement” conflict, revert to the source even if the custom version looked richer; example: file-list row metadata should not be retained if `FileManager` uses a simpler one-line item
- do not duplicate workspace actions inside the right content pane if the source already places them in the top tabs/action bar; avoid stacking `file header + breadcrumb + content` when `Operit` goes more directly from the top bar into content
- for editable workspace text/code files, avoid form-field components (`OutlinedTextField`/general input styling); even before full editor migration, the surface should read like an editor host with direct content and optional symbol strip, not like settings-form input
- text/code read-only and editable states should stay inside the same editor family where possible; do not render read-only plain text with a completely different Compose-only layout if the goal is to approach `Operit CodeEditor`
- before full editor-subsystem migration, language detection and token coloring should still follow the source project’s extension semantics and editor-theme direction; do not introduce arbitrary note-app styling into code files
- editor behavior should be driven by a dedicated local controller instead of raw `TextWatcher -> state map` piping once undo/redo or future format actions enter the workspace; this keeps history and external file state from drifting apart
- source-aligned editor actions should prefer a distinct workspace action menu entry rather than being scattered into unrelated top bars once the underlying editor controller exists
- while the local editor controller remains a lightweight bridge, `Format` must stay conditional and source-scoped: only expose it for the same language family currently handled by `Operit CodeFormatter` (`JavaScript / CSS / HTML`), and always route the result through both editor replacement and unsaved-file state sync
- once `Files` is restored to the workspace FAB menu, treat the bottom-sheet file browser as the source-truth entry path; any temporary always-visible explorer should be considered transitional and scheduled for removal rather than expanded further
- after the runtime layout switches back to a single-content-area workspace, keep follow-up cleanup focused on deleting retired inline-explorer branches rather than reactivating them for convenience

## Workspace editor action notes 2026-04-29

The current workspace editor branch now has a locked interim action model:

- action entry stays in the bottom-right expandable FAB menu, not in the right-pane header
- first required parity actions are `Undo / Redo / Format / Files / Export`
- `Format` is not a generic beautifier hook; it is a source-compatibility action and should keep matching `Operit` formatter coverage before any broader formatter idea is considered
- any later additions such as `Files / Export / Rename / Unbind` should be added by continuing to compare against `WorkspaceManager.ExpandableFabMenu(...)`, not by inventing new local editor actions
- `Files` should remain reachable even when no active editor is bound, as long as the IME is not occupying the screen; this matches the fact that the source FAB menu is a workspace-level action entry, not an editor-only one
- the visible workspace body should stay single-pane at runtime once file browsing is sheet-driven; do not drift back to a permanent split-view explorer unless direct source comparison proves that branch is required
- `Export` should stay a workspace-level directory export path rather than a per-file action; the replica should keep exporting the current workspace root through a shareable archive instead of scattering export entry points into tabs or preview headers

### 阶段 5

目标：

- 视觉对齐与交互节奏校准

标准：

- 颜色、圆角、阴影、留白
- 进入节奏、关闭节奏、拖动节奏

### 阶段 6

目标：

- 能力入口替换

标准：

- 附件、workspace、角色选择、统计菜单逐步由假入口切到真状态

## 后续实施禁令

- 不允许再把 Operit 逻辑写回 `HomeScreen.kt`
- 不允许继续在单个 `KiyoriOperitReplicaScreen.kt` 中无节制加功能
- 不允许先改视觉、后补结构
- 不允许把 workspace/computer 简化成普通弹窗替代

## 当前最合理的下一步

不是继续修颜色，也不是继续补假功能。

而是：

1. 在现有 `KiyoriOperitReplicaViewModel` 上继续补齐 `HeaderState / HistoryState / ChatState / InputState`
2. 把当前已经收敛到 `UiState` 的派生字段，进一步整理成接近 `Operit delegate` 的子状态对象
3. 先拆 `KiyoriOperitHeader.kt`
4. 再拆 `KiyoriOperitHistoryPanel.kt`
5. 再拆 `KiyoriOperitInputBar.kt`
6. 继续把当前 `OverlayHost` 背后的占位状态升级成真实 workspace / computer / character 数据源

这样后面每一次“照抄 `Operit` 某个组件”才有正确落点，不会再次回到“大文件假复刻”的旧路。

## 2026-04-30 First-screen architecture note

- do not judge the first screen by whether local placeholder features work
- judge it by whether the visible composition matches the source screen hierarchy:
  - top row = history, pip, avatar/name, context ring
  - middle area = blank chat surface when there is no real user message
  - bottom area = text field, model chip, settings, add, send/mic
- this means the first-screen replica must prefer source-faithful composition over custom convenience widgets
- any local state that produces explanatory seed content on entry is architectural noise and should eventually be removed from the state layer, not designed around
- if there is a conflict between keeping an old local placeholder interaction and preserving screenshot-level first-screen parity, choose screenshot-level parity first

## 2026-04-30 Source-transplant directive

- stop treating `operitreplica` as the final UI implementation
- the target is now a direct subtree transplant from `Operit` chat first-screen
- preferred transplant order:
  - `ChatHeader.kt`
  - `ChatScreenHeader.kt` visual contract
  - `AgentChatInputSection.kt`
  - `ChatScreenContent.kt` visible composition shell
- `kiyori` should supply compatibility only where the source component needs host-specific state or resources:
  - strings
  - avatar/resource mapping
  - click callbacks
  - lightweight token/history/floating state
- if a choice must be made between preserving old local helpers and running original `Operit` components, choose the original component path
- first acceptance checkpoint:
  - top row must be rendered by transplanted `Operit` `ChatHeader`, not by a handwritten imitation

## 2026-04-30 Input popup ownership note

- input-owned popup controls must stay inside the source-package input bridge path
- page-level overlays should not own the first-screen settings popup anymore
- this rule now has a concrete implementation step:
  - `AgentExtraSettingsPopupBridge.kt` hosts the visible settings popup from the input workbench path
- next bridge target under the same rule:
  - replace the simplified model dropdown with a source-faithful model selector popup bridge

## 2026-04-30 Model popup note

- the model selector has now also moved to a source-package popup bridge path:
  - `AgentModelSelectorPopupBridge.kt`
- this establishes a stronger architecture rule for the first screen:
  - the model/settings/add row belongs to one input workbench subtree
  - popup ownership for that row should not leak back to page-level overlays or inline material menus
- next consequence:
  - attachment popup should follow the same ownership rule before moving on to broader `ChatScreenContent` shell transplant

## 2026-04-30 Attachment popup note

- the attachment popup now also follows the same rule:
  - `AttachmentSelectorPopupPanelBridge.kt`
- this means the visible `model / settings / add` row now has all three popup surfaces owned by the input workbench subtree
- architectural consequence:
  - future `ChatScreenContent` work should not add page-level popup shortcuts back into this row
  - remaining attachment work should focus on replacing compatibility actions with source-faithful intake flows, not rebuilding another popup shell

## 2026-04-30 Content shell note

- the first-screen content area now has a dedicated source-package shell bridge:
  - `ChatScreenContentWorkbenchBridge.kt`
- this establishes the next architecture rule:
  - header/content stacking belongs to a content-shell bridge, not a local `Column` in replica UI
  - later `ChatArea` migration should plug into this shell instead of replacing the whole page structure again

## 2026-04-30 Chat-area behavior note

- visible chat-area behavior can now be transplanted incrementally under the content shell
- first example already landed:
  - `ScrollToBottomButton.kt`
- practical implication:
  - continue migrating chat-area sub-behaviors as source slices
  - avoid rewriting a brand-new local message-area framework if an original behavior component can be dropped in first

## 2026-04-30 Pagination-window note

- `ChatArea` page-window behavior is also suitable for bridge-first migration
- first helper slice now exists:
  - `ChatAreaPaginationBridge.kt`
- this establishes another rule for later rounds:
  - when reducing `ChatArea` mismatch, prefer transplanting source windowing / navigation behavior before rewriting message widgets again

## 2026-04-30 ChatScrollNavigator note

- the next `ChatArea` control slice has now landed in the same source package:
  - `ChatScrollNavigatorBridge.kt`
- architecture rule confirmed by this step:
  - message-area navigation should be transplanted from source behavior first
  - local `operitreplica` code should only provide compatibility inputs such as:
    - lightweight message mapping
    - anchor coordinates
    - active conversation key
- `KiyoriOperitReplicaChatContent.kt` now owns less visual policy than before
- it mainly hosts:
  - local message row rendering
  - source-package pagination helpers
  - source-package scroll-to-bottom behavior
  - source-package scroll locator/navigation behavior
- next blueprint implication:
  - do not spend another round hand-tuning chat bubble cosmetics first
  - continue collapsing `KiyoriOperitReplicaChatContent.kt` toward a true `ChatArea` host shell

## 2026-04-30 Loading indicator note

- the `ChatArea` loading animation is also now transplanted as a source component:
  - `LoadingDotsIndicator.kt`
- blueprint implication confirmed again:
  - processing feedback inside the message stream belongs to the chat-area subtree
  - do not let the input bar become the only source of “working” feedback on the first screen
- next architecture target remains unchanged:
  - keep moving behavior from local message widgets into source-structured chat-area slices before spending effort on appearance-only tuning
