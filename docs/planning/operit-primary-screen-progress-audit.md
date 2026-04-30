# 正一屏 Operit 复刻进度总审计

## 审计时间

- 日期：2026-04-29
- 审计对象：`kiyori-android` 正一屏 `operitreplica/`
- 对照基线：`D:\10_Project\Operit\app\src\main\java\com\ai\assistance\operit\ui\features\chat`

## 结论

当前进度还没有达到“完全一样”。

已经拉齐的是“宿主骨架、状态归拢方向、历史面板层级方向”。

还没有拉齐的是“真实 Workspace / Computer 宿主能力、真实 ChatArea 组织方式、真实 Agent 输入工作台细节、历史项滑动和拖拽交互”。

所以现在不能把正一屏视为“完成复刻”，只能视为“已经进入正确骨架阶段”。

## 已经对齐的部分

### 1. 宿主挂载边界

- `HomeScreen.kt` 只做宿主挂载，不再承载整页复刻逻辑。
- 正一屏主实现已经集中在 `operitreplica/`。
- 这一点和“只照抄 `AIChatScreen` 子系统，不照抄 `Operit` 顶层 app shell”的原则一致。

### 2. 页面装配结构

- 已拆出：
  - `KiyoriOperitReplicaHeader.kt`
  - `KiyoriOperitReplicaChatContent.kt`
  - `KiyoriOperitReplicaInputBar.kt`
  - `KiyoriOperitReplicaHistoryPanel.kt`
  - `KiyoriOperitReplicaOverlays.kt`
- `KiyoriOperitReplicaScreen.kt` 现在主要承担页面装配职责，这个方向是对的。

### 3. ViewModel 归拢方向

- 会话、消息、历史搜索、角色选择、附件、reply preview、overlay 可见性，已经进入 `KiyoriOperitReplicaViewModel`。
- 头部统计派生字段也已经统一从 `withDerivedState()` 输出。
- 这一步避免了后续继续把关键状态散落在 Composable 本地状态里。

### 4. History 面板骨架

- 已从单层 section 结构升级为：

```text
CharacterBucket
  GroupHeader
    ConversationRow
```

- 已支持：
  - `BY_CHARACTER_CARD`
  - `BY_FOLDER`
  - `CURRENT_CHARACTER_ONLY`
- 已支持分组实体、空分组、分组管理、会话管理。

### 5. Overlay 生命周期方向

- History：遮罩 + 左滑面板，方向正确。
- Workspace：隐藏时继续保活，不直接销毁，方向正确。
- Computer：关闭时直接移出组合，方向正确。
- 本轮新增了 `workspaceReloadVersion`，作为工作区宿主的显式重建信号，后续接真实 `WorkspaceScreen` 时不会再返工宿主生命周期。

## 还没有对齐的关键差距

### A. Workspace 还不是 Operit 的真实工作区

当前已经从“纯说明占位层”升级成“真实 WebView 宿主首版”，但还不是 `Operit` 的真实 `WorkspaceScreen`。

已完成：

- 全屏可交互 Workspace 宿主
- 真正的 WebView 内容区
- 地址输入 / 回退 / 前进 / 刷新 / 宿主重建入口
- 会话级真实工作区内部目录
- 左侧文件树
- 右侧文本文件预览
- 顶部打开文件标签条
- 文本文件编辑态 / 预览态切换
- 文件保存链路
- 未保存文件关闭确认
- HTML 文件首次打开默认进入预览态
- 文件树排序切换
- 隐藏文件显隐切换
- 根工作区新建文件 / 文件夹
- 长按文件项删除确认
- 当前目录视角导航
- 返回上级目录
- 快捷路径入口（Root / Notes / Prompts / Preview / Scratch）
- 文件预览面包屑导航
- 左侧当前目录面包屑导航
- Markdown 专用预览首版
- HTML 专用 WebView 预览首版
- 标签栏右侧预览切换 / 保存动作区
- 图片专用预览首版
- 音频专用预览首版
- 视频专用预览首版
- PDF / Office 文档只读预览首版
- 二进制文件编辑态限制
- 继续保留 `Operit` 风格的保活与重建宿主思路

还缺：

- 外部或用户指定的真实工作目录绑定
- 更接近 Operit 的 VSCode 风格标签栏细节与关闭/切换节奏
- 更接近 Operit 的文件浏览器快捷路径细节与路径栏表现
- 更接近 Operit 的媒体 / 文档预览质量与原生播放器能力
- 更接近 Operit 的本地服务预览逻辑
- 工作区文件选择器
- 工作区与当前会话上下文的真实联动

这是离“进入正一屏像进入另一个软件”最近、也最关键的缺口。

### B. Computer 还不是 Operit 的真实 ComputerScreen

当前只是全屏占位宿主，生命周期策略已对，但能力没接上。

还缺：

- 独立终端或桌面容器
- 全触摸拦截
- 真实资源释放链路

### C. ChatContent 还不是 Operit 的 ChatScreenContent + ChatArea

当前聊天区仍然偏向“本地纵向列表拼装”，还没有完全变成 `Operit` 的内容组织方式。

还缺：

- Header 与消息区的真实叠层关系
- 更接近 `ChatArea` 的滚动与消息节奏
- 消息级编辑 / 多选 / 更细的交互骨架

### D. InputBar 还不是 Operit 的 Agent 输入工作台

当前底部输入区还只是接近，不是 1:1。

还缺：

- 更完整的 processing state
- queue / cancel / memory / max-context / tool prompt 等结构
- 更接近 `AgentChatInputSection` 的布局和状态切换

### E. History 交互还不够像

当前历史面板的数据骨架方向对了，但交互还不够像 `Operit`。

还缺：

- `SwipeableActionsBox` 风格操作
- 拖拽重排
- 更贴近原项目的分组行、新建行、管理入口节奏

## 当前复刻成熟度判断

按“是否能说已经完全照抄”来判断：

- 宿主骨架：70%
- 历史数据结构：75%
- 历史交互细节：45%
- 头部结构：60%
- 聊天内容结构：40%
- 输入工作台：35%
- Workspace：40%
- Computer：20%

综合判断：当前更接近“骨架已立住，细部和真实宿主未完成”，不能判定为完成复刻。

## 现在最应该继续做什么

顺序不要乱。

### 第一优先级

先把 Workspace 从“会话级内部工作区容器”继续推进到“真实工作目录容器”。

原因：

- 这是 `Operit` 正一屏最有“进入另一套软件”感觉的层。
- 这是当前和原项目差距最大的功能层。
- 这层如果后做，前面的输入区和聊天区很容易因为上下文共享方式不对而返工。

### 第二优先级

把 ChatContent 从“本地消息列表”继续拉向 `ChatScreenContent + ChatArea`。

### 第三优先级

把 InputBar 从“简化输入条”继续拉向 `AgentChatInputSection`。

### 第四优先级

把 History 面板交互从“对话框管理”升级到更接近原项目的滑动 / 拖拽管理。

## 当前阶段的硬约束

- 不要回退到在 `HomeScreen.kt` 里堆逻辑。
- 不要为了看起来像而先乱改视觉细节。
- 不要把 Workspace / Computer 简化成普通弹窗思路。
- 继续坚持：先对齐层级、宿主、状态，再对齐视觉和交互细节。

## 本轮新增的参考点

- `workspaceReloadVersion`
  - 作用：给工作区宿主一个显式重建入口
  - 用途：后续接真实工作区 WebView / 文件系统宿主时，可用它强制重建宿主，不必破坏整个页面状态

- Workspace 文件工作流首轮落地
  - 已有：`Preview` 标签、打开文件标签、编辑/预览切换、保存、未保存关闭确认、HTML 默认预览
  - 已新增：文件树排序、隐藏文件开关、新建文件/文件夹、长按删除确认、当前目录导航、返回上级、固定快捷路径、左右两侧面包屑导航、Markdown/HTML 预览分流、标签栏动作区、图片/音视频/文档预览首版、二进制只读限制、文件树状态摘要
  - 已新增：编辑器右下角动作菜单已补齐到 `撤销 / 重做 / 格式化` 首版，其中 `格式化` 只对 `JavaScript / CSS / HTML` 打开，范围与 `Operit` `CodeFormatter` 对齐
  - 已新增：`Files` 动作已接回右下角悬浮菜单，并能从底部弹出文件浏览器面板，开始贴近 `Operit` `showFileManager + FileBrowser` 这条交互链
  - 已新增：主工作区画面已停用常驻左侧文件栏，文件浏览正式收口到 `Files` 底部面板入口，主内容区回到更接近 `Operit` 的单内容区节奏
  - 已新增：`Export` 动作已接回 workspace FAB，可将当前工作区目录打包为 zip 并通过系统分享导出，开始贴近 `Operit` `workspaceConfig.export + onExportClick` 这条链
  - 对照：已开始贴近 `Operit` 的 `openFiles / filePreviewStates / unsavedFiles / close confirm / FileManager basic actions / currentPath navigation / preview routing` 这条链
  - 仍缺：把当前已停用的内联文件栏代码彻底清理出主实现，继续补齐剩余的源项目 FAB 菜单分支（如 `Rename / Unbind`），以及更原生的媒体/文档预览质量

- History 设置文案
  - `BY_FOLDER` 已改成“按文件夹分组”
  - 目的：语义和当前分组模型保持一致，避免后续继续在错误语义上复刻

## 构建验证要求

后续每次代码改动后，继续执行：

```powershell
.\gradlew.bat assembleDebug
```

调试包输出位置固定为：

`D:\10_Project\kiyori-android\app\build\outputs\apk\debug`

## 2026-04-29 Workspace fidelity update

- `KiyoriOperitReplicaWorkspaceHost.kt` tab bar moved closer to `Operit` `VSCodeTab` rhythm:
  - preview tab now has an explicit icon
  - opened file tabs now use file-type icons instead of text-only chips
  - unsaved state now uses a dot indicator in the close area instead of a trailing `*`
  - active tab keeps a clearer top-tab silhouette with a bottom accent line
- left file rows moved closer to `Operit` `FileListItem` information density:
  - file/folder rows now resolve richer type icons
  - files now show `size | modified time`
  - directories now show a folder meta label
  - parent navigation row now uses an explicit back-style icon and `Up` meta label
- this round is still a fidelity refinement, not feature completion:
  - real external workspace binding is still missing
  - media/document preview quality is still first-pass, not yet equal to Operit native handling
  - path bar spacing and tab close hit-target rhythm still need another comparison pass

## Latest build verification

- verified with `.\gradlew.bat assembleDebug`
- build date: `2026-04-29`
- debug APK path: `D:\10_Project\kiyori-android\app\build\outputs\apk\debug\app-arm64-v8a-debug.apk`

## 2026-04-30 input popup bridge update

- the old local `KiyoriOperitReplicaFeaturePanel` is no longer the visible settings popup path for the first screen
- a new source-package bridge now hosts the settings popup:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentExtraSettingsPopupBridge.kt`
- `AgentChatInputWorkbenchBridge.kt` now owns:
  - queue panel
  - attachment chips
  - fullscreen dialog bridge
  - bottom-end extra settings popup bridge
- compatibility state was added in `KiyoriOperitReplicaUiState` / `KiyoriOperitReplicaViewModel` for source-popup semantics:
  - memory profile label
  - memory auto update
  - auto read
  - auto approve
  - disable stream output
  - disable user preference description
  - disable status tags
- this round fixes a structural mismatch, not the whole first-screen gap:
  - the settings popup now belongs to the input subtree like `Operit`
  - the model selector is still simplified and is the next bridge target

## 2026-04-30 model popup bridge update

- the input workbench no longer uses an inline `DropdownMenu` as the visible model selector path
- a new source-package bridge now hosts the model popup:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentModelSelectorPopupBridge.kt`
- `AgentChatInputWorkbenchBridge.kt` now owns both source-style popups that sit around the model/settings row:
  - model selector popup
  - extra settings popup
- first-pass compatibility state was added for source popup semantics:
  - thinking quality level
  - max-context mode
  - base/max context length display
- this round still does not mean `AgentChatInputSection` is fully transplanted:
  - attachment popup ownership is still not on the same source path
  - the model popup uses local compatibility data, not the original config-manager stack yet

## 2026-04-30 attachment popup bridge update

- the first-screen attachment popup is no longer rendered from page-level overlay code
- a new source-package bridge now hosts the visible popup:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AttachmentSelectorPopupPanelBridge.kt`
- `AgentChatInputWorkbenchBridge.kt` now owns the full visible popup set around the input action row:
  - model selector popup
  - extra settings popup
  - attachment popup
- this round fixes popup ownership and visible structure first:
  - the popup now appears from the input subtree instead of the page overlay host
  - row order and popup dimensions follow the source `AttachmentSelectorPopupPanel`
- still missing from full source parity:
  - real file/image/camera intake flows
  - source-side URI/path handling and permission flow

## 2026-04-30 content shell bridge update

- the first-screen content area is no longer structured as a plain local `Column(header + list)`
- a new source-package shell bridge now hosts the top-level content stacking:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatScreenContentWorkbenchBridge.kt`
- `KiyoriOperitReplicaChatContent.kt` is now mostly adapter logic:
  - passes header content into the bridge
  - passes message content into the bridge
  - stops owning top-level header overlay logic directly
- visible consequence:
  - header now overlays the chat surface like `Operit` `ChatScreenContent`
  - message area gets measured top padding from actual header height
  - empty first-screen state keeps source-like header spacing instead of a simple blank column
- still missing from full parity:
  - real `ChatArea` behavior and controls
  - source-like scroll affordances and message interaction systems

## 2026-04-30 chat-area visible behavior update

- the first-screen message area now uses a transplanted source component for one visible behavior slice:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ScrollToBottomButton.kt`
- `KiyoriOperitReplicaChatContent.kt` now wires:
  - local `ScrollState`
  - local auto-scroll state
  - source-style scroll-to-bottom affordance
- this is still a shell-level bridge, not a full `ChatArea` transplant:
  - the message list is still local
  - hidden-page pagination, context menus, edit/rollback systems are not yet on the source path

## 2026-04-30 pagination window bridge update

- the first-screen message area now also uses a source-package pagination helper slice:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatAreaPaginationBridge.kt`
- `KiyoriOperitReplicaChatContent.kt` now applies a visible message window instead of always flattening the full list
- visible consequence:
  - top `load more history` bar now appears when older pages are hidden
  - bottom `load newer history` bar now appears when the visible window is not the newest page
  - scroll-to-bottom affordance now knows whether newer pages are hidden
- this is still not a full `ChatArea` transplant:
  - page-window behavior is now closer
  - message-item interaction stack is still local

## 2026-04-29 Workspace navigation refinement

- left workspace navigation moved closer to a real editor explorer instead of a plain utility list:
  - quick paths now carry explicit icons
  - quick path labels are aligned to the workspace folder semantics: `Root / Notes / Prompts / Preview / Scratch`
  - preview tab label and breadcrumb root label now use the same `Preview / Root` wording
- current-directory feedback is denser:
  - status summary now includes current sort mode, visible item count, and hidden-file state
  - file rows now include a subtle divider rhythm between items
- this round still does not complete Operit parity:
  - breadcrumb visual emphasis is still lighter than Operit
  - quick-path chip spacing and active-state rhythm still need another pass
  - workspace binding is still conversation-scoped local storage, not a real external directory binding

## 2026-04-29 Workspace path-bar refinement

- both workspace path bars now use chip-style path segments instead of `TextButton + /`:
  - left explorer directory path
  - right file breadcrumb path
- path separators now behave like editor navigation separators instead of raw slash text
- the explorer header path display now shows a stable workspace-relative path (`Root / ...`) instead of exposing the internal conversation folder name
- this improves structural fidelity, but still does not finish the comparison:
  - active path chip weight and spacing still need another screenshot-level pass
  - file breadcrumb still needs stronger last-segment emphasis to match Operit exactly

## 2026-04-29 Workspace preview-header refinement

- right preview pane header moved closer to an editor toolbar rhythm:
  - file title is now separated from the full relative path
  - mode toggle and save action now use compact toolbar chips instead of loose text buttons
  - the small workspace context chip is now visually aligned with the breadcrumb grammar
- file breadcrumb final segment now has a stronger “current file” emphasis than directory segments
- this round still does not complete parity:
  - top action chip spacing and active-state tint still need a side-by-side pass against Operit
  - preview header information density may still differ from the original if Operit shows more runtime context

## 2026-04-29 Workspace explorer-header rhythm refinement

- left explorer header is no longer a loose stack of title, path, actions, summary, and quick links
- title and status summary are now grouped into one header line
- current path, directory bar, action row, and quick paths are now packed into a single explorer-header block with one divider before the file list
- this gets closer to Operit’s denser file-manager rhythm, but there is still visible gap:
  - exact vertical spacing between path row, action row, and quick-path row still needs a screenshot-level pass
  - the title row may still differ if Operit exposes additional context or management affordances

## 2026-04-29 Workspace density tightening pass

- left explorer header padding, row gaps, and quick-path spacing were tightened again toward Operit’s denser file-manager rhythm
- right preview header was also compressed:
  - smaller title/subtitle text rhythm
  - tighter toolbar chip padding
  - smaller chip icon/text sizing
- this round is intentionally low-risk and visual-density only:
  - no workspace state flow changed
  - no file open/save/preview routing changed

## 2026-04-29 Workspace preview-action correction

- after re-checking `Operit` source, the right preview header action area was corrected back toward the original interaction model:
  - removed labeled action chips from the preview header
  - preview/edit toggle is back to a compact icon button
  - save action now only appears when there are unsaved editable changes
  - non-markdown/non-html files no longer show an unnecessary mode-toggle affordance
- this is a fidelity correction, not a new feature:
  - it reduces divergence from `WorkspaceManager` instead of adding more custom UI language

## 2026-04-29 Workspace explorer-header correction

- after re-checking `Operit FileManager`, the left explorer header was corrected away from the custom multi-block layout:
  - removed the visible title/status-summary header line
  - restored the primary structure to `path row + action icons + quick paths`
  - directory breadcrumb is now a secondary row that only appears when not at root
- this is closer to the source behavior because the original file manager header is path-first, not dashboard-first

## 2026-04-29 Workspace quick-path and icon-button correction

- left explorer header action buttons were corrected toward the source `FileManager` rhythm:
  - header action hit area now follows the 36dp button rhythm instead of the smaller shared secondary-icon size
- quick-path row was also corrected toward the source component language:
  - moved from a fully custom surface chip back toward a `FilterChip`-style implementation
  - selected/unselected state still keeps Kiyori’s current palette, but the structural behavior now follows the original component more closely

## 2026-04-29 Workspace tab/action hit-area correction

- top workspace tab close/unsaved area now uses a dedicated 22dp hit target closer to `VSCodeTab` instead of a bare clickable icon
- top tab-bar action buttons and right preview-header action buttons no longer reuse the shared `OperitSecondaryIconButton`
- both action areas now use a workspace-specific 40dp toolbar-button rhythm closer to `WorkspaceManager` icon-button sizing

## 2026-04-29 Workspace wording consistency correction

- workspace root wording is now unified:
  - left path display uses `Workspace / ...`
  - root quick-path label uses `Workspace`
  - breadcrumb root was already using `Workspace`
- this removes a visible `Root` / `Workspace` split that did not exist in the intended Operit-style language

## 2026-04-29 Workspace wording cleanup pass

- quick-path labels were de-productized:
  - kept `Workspace` for the root entry
  - switched secondary entries to actual folder names: `notes / prompts / preview / scratch`
- status summary wording was also pulled closer to Operit file-manager strings:
  - `按名称 / 按大小 / 按修改时间`
  - `显示.文件 / 隐藏.文件`
- this round is purely semantic cleanup; no layout or interaction changed

## 2026-04-29 Workspace dialog wording cleanup

- unsaved-close dialog wording was tightened from descriptive custom phrasing to a shorter file-manager style confirmation
- create dialog wording was also normalized:
  - `创建新文件 / 创建文件夹`
  - `位置：...`
  - `输入文件名 / 输入文件夹名`
- delete dialog title was simplified to `删除`
- read-only preview message was shortened to `此类文件仅支持预览。`

## 2026-04-29 Workspace runtime copy cleanup

- a visible non-source gap was removed from the live workspace runtime:
  - loading copy no longer says `Workspace 宿主`
  - generated seed files no longer describe `replica / operitreplica / WebView 宿主首版 / 当前阶段`
  - preview html and landing html no longer expose `OPERIT WORKSPACE PREVIEW / HOST`, `WebView Host`, `File Manager`, or project-progress text
- this is important for fidelity because entering the first screen should feel like entering a real workspace surface, not opening an internal implementation note
- seed-file write behavior was also hardened:
  - old generated files are now rewritten only when they still contain legacy marker phrases
  - this preserves future user edits while still cleaning previously generated replica-oriented content

## 2026-04-29 Workspace file-list rhythm correction

- after re-checking `Operit FileManager.kt`, the left file list was corrected back toward the source implementation:
  - replaced the manual `Column + verticalScroll` file list with `LazyColumn`
  - removed the custom right-side `size | modified` metadata from each row
  - removed the custom row separator/highlight treatment
  - switched the parent-navigation row icon back toward the source `FolderOpen` style
- this is a fidelity correction:
  - the previous richer row metadata was not actually present in the current `Operit` source
  - for strict replication, source simplicity is more important than adding extra editor-like information density

## 2026-04-29 Workspace preview-header removal

- after re-checking `Operit WorkspaceManager.kt`, the right content pane was corrected away from a duplicate local toolbar:
  - removed the file-specific preview header row
  - removed the separate right-pane breadcrumb bar
  - kept preview/edit and save actions only in the top tabs/action bar, which is where the source project exposes them
- this is another source-alignment correction:
  - the previous extra header made the right pane feel like a second custom tool surface
  - `Operit` is closer to `top tabs/actions -> content directly`, not `top tabs/actions -> file header -> breadcrumb -> content`

## 2026-04-29 Workspace editor-surface correction

- the text editing surface was corrected away from `OutlinedTextField` toward an editor-like host:
  - editable text files now use an `AndroidView`-backed native text pane instead of a form-styled Compose field
  - the editing area is now full-bleed white content rather than an outlined input box
  - a bottom symbol strip was added so the visible structure moves closer to `Operit` `CodeEditor`
- this is still not full editor parity:
  - syntax highlighting, completion, undo/redo, and the exact native editor implementation are still missing
  - but the first-glance structure is now closer to `Operit` than a plain form field

## 2026-04-29 Workspace gutter unification

- the text/code surface was tightened one step further toward `Operit CodeEditor` structure:
  - added a dedicated left gutter for line numbers
  - unified plain-text read-only rendering onto the same editor host instead of a separate `SelectionContainer` layout
  - kept the bottom symbol strip only for editable mode
- this reduces another visible non-source split:
  - text files now read more like one editor family across edit/read-only states instead of two unrelated renderers

## 2026-04-29 Workspace lightweight syntax-highlighting pass

- the current editor host now includes a source-driven lightweight language layer:
  - file names are mapped to language ids using the same extension semantics as `Operit` `LanguageDetector`
  - the editor surface now uses a light editor palette closer to the source editor theme
  - common token categories such as keywords, comments, strings, and numbers now receive basic highlighting for major text/code types
- this is still not full `Operit` editor parity:
  - no completion popup
  - no native canvas renderer
  - no real undo/redo stack parity
  - but the text area now reads more like a code editor and less like a plain note field

## 2026-04-29 Workspace editor action-menu and history pass

- the workspace editor now has a source-aligned editing action entry instead of only passive text updates:
  - added a bottom-right expandable action menu closer to `Operit` `ExpandableFabMenu`
  - current first-pass actions are `撤销 / 重做`
  - menu automatically collapses when the editor is unavailable or the IME is visible
- the editor host itself now keeps a local text history controller:
  - editable text changes are recorded into an undo/redo stack
  - external state sync no longer blindly fights local editor history
  - `replaceAllText(...)` is now available for future format/code-transform actions
- this is still a first-pass fidelity step:
  - menu items are not yet as complete as the source workspace menu
  - format/export/unbind/rename actions are still missing from this replica branch

## 2026-04-29 Workspace editor format-action pass

- the workspace editor action menu now conditionally exposes `格式化` in the same place as `Operit` `ExpandableFabMenu`
- formatter scope is intentionally source-limited:
  - `javascript/js`
  - `css`
  - `html/htm`
- format execution now follows the same state path expected by the source workspace flow:
  - replace visible editor text through `replaceAllText(...)`
  - sync the formatted result back into `editedFileContents`
  - mark the current file as unsaved so the tab/action bar remains truthful
- this is a fidelity correction, not a feature expansion:
  - the formatter is not exposed for unsupported languages
  - the remaining source actions (`Files / Export / Rename / Unbind`) are still pending

## 2026-04-29 Workspace files-menu sheet pass

- the bottom-right workspace menu is no longer limited to edit mode:
  - when the IME is hidden, the menu can now stay available even outside an active editor
  - this is necessary because `Operit` `ExpandableFabMenu` is not an editor-only affordance
- the `Files` branch is now wired back into the replica:
  - tapping `Files` opens a bottom sheet file manager instead of forcing the left pane to be the only explorer entry
  - the sheet reuses the current local explorer actions: quick paths, hidden-files toggle, sort, create, refresh, go-up, open-file, delete
- this is an intermediate source-alignment step, not final parity:
  - the always-visible left explorer still exists and should be retired in a later structural cleanup
  - the goal state remains closer to `WorkspaceManager` where file browsing is entered through the bottom sheet path

## 2026-04-29 Workspace single-content-area pass

- the main workspace surface no longer renders the inline left explorer during runtime
- file browsing is now effectively routed through the `Files` bottom sheet path, which restores a layout rhythm closer to `Operit`:
  - top tabs/action area
  - single main preview/editor surface
  - workspace-level FAB actions
- this is a runtime-structure correction first:
  - the disabled inline explorer code still needs follow-up cleanup
  - but the visible layout is no longer split by a permanent local file pane

## 2026-04-30 Workspace export-action pass

- the workspace FAB menu now exposes `Export` as a workspace-level action alongside `Files`
- current export flow is aligned to the source intent:
  - export the current workspace directory, not just a single open file
  - package the directory as a zip archive
  - hand the archive to the Android system share sheet through `FileProvider`
- this is still partial parity:
  - export currently uses the local replica workspace root directly
  - `Rename / Unbind` remain pending in the FAB menu branch

## 2026-04-30 First-screen fidelity correction

- root-cause confirmed:
  - the current `operitreplica` first screen was still driven by a custom intermediate UI, not by the real `Operit` `ChatScreenHeader + ChatScreenContent + ChatInputBottomBar/AgentChatInputSection` composition
  - this is why the page drifted into explanatory cards, custom status strips, and the wrong input rhythm
- concrete visible mismatches corrected in this pass:
  - removed the extra header arrow and moved the header closer to `Operit` source structure
  - rebuilt the bottom input area into the source-like order: model pill, settings icon, add icon, send/mic button
  - removed the custom reply/status-strip driven first-screen appearance
  - changed first-screen chat rendering so the page stays visually blank until there is a real user message
- important implementation note:
  - current state still contains older seed-message paths in `KiyoriOperitReplicaViewModel`
  - the rendering layer now suppresses those seed messages on first-screen entry so they no longer pollute the default UI
  - a follow-up state cleanup is still required to remove those seed-message branches completely instead of only hiding them at render time
- current acceptance target:
  - treat first-screen fidelity as screenshot parity work, not feature equivalence
  - for this screen, layout rhythm and control placement are higher priority than adding more local placeholder behavior
- verified build:
  - `./gradlew.bat assembleDebug`
  - output directory: `D:\10_Project\kiyori-android\app\build\outputs\apk\debug`

## 2026-04-30 First-screen state cleanup

- first-screen empty state is now established in the state layer, not only in the rendering layer
- `KiyoriOperitReplicaViewModel` no longer seeds default conversations with explanatory assistant/system messages
- new conversations and fallback conversations now start with empty message lists
- the old seed-message helper has been removed from the active path
- this reduces the risk of hidden placeholder copy leaking back into the first screen during later refactors

## 2026-04-30 First source-transplant checkpoint

- the first-screen correction is no longer limited to polishing local replica widgets
- `kiyori` now contains a mirrored `Operit` package component:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatHeader.kt`
- `KiyoriOperitReplicaHeader.kt` now uses the transplanted `ChatHeader` for the left-side top-row structure instead of rendering a handwritten imitation of:
  - history button
  - PiP button
  - avatar + character name switcher
- `kiyori` currently provides the minimum bridge needed for that original component to run:
  - local string resources for history/floating-window labels
  - local avatar resource mapped into an `android.resource://...` uri
  - local click callbacks and lightweight state
- this is the first concrete proof that the first screen can move from “looks similar” to “runs original Operit subtree”
- next transplant priorities remain:
  - `ChatScreenHeader` contract alignment
  - `AgentChatInputSection`
  - `ChatScreenContent` visible shell

## 2026-04-30 Input subcomponent transplant checkpoint

- the input area has now started using original `Operit` subcomponents instead of only local equivalents
- newly mirrored source components:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/AttachmentChip.kt`
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/FullscreenInputDialog.kt`
- local bridge added for those source components:
  - `app/src/main/java/com/ai/assistance/operit/data/model/AttachmentInfo.kt`
  - string resources for attachment insert/remove and fullscreen input actions
  - local `String -> TextFieldValue` fullscreen adapter
- current `KiyoriOperitReplicaInputBar.kt` now renders transplanted `AttachmentChip`
- current fullscreen editor overlay path now enters transplanted `FullscreenInputDialog`
- current input-bar visible shell is no longer directly handwritten inside `KiyoriOperitReplicaInputBar.kt`
- a source-package compatibility shell now hosts the visible workbench structure:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentChatInputWorkbenchBridge.kt`
- this is still not the full `AgentChatInputSection`, but it confirms the input workbench can be migrated in layers without falling back to handwritten replacements
- next input-specific target:
  - move from “local input bar hosting original subcomponents” to “source-faithful `AgentChatInputSection` shell with a kiyori compatibility bridge”

## 2026-04-30 Input shell bridge checkpoint

- `KiyoriOperitReplicaInputBar.kt` is now reduced to a host/adapter role
- the visible input workbench layout has been lifted into the source package bridge:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentChatInputWorkbenchBridge.kt`
- this matters because the next migration step is no longer “rewrite the local input bar again”
- it becomes:
  - keep extending the bridge toward `AgentChatInputSection`
  - then replace the bridge with larger source slices as dependencies are adapted

## 2026-04-30 Input behavior bridge checkpoint

- the input bridge now owns a first-pass behavior layer, not only static structure
- newly aligned visible behaviors:
  - model pill can open and close a selector popup
  - model label can switch between local bridge options
  - sending a prompt now enters a processing state before the assistant reply lands
  - the action button can switch into a cancel state during processing
  - the bridge can show a processing label and ring instead of only a static send/mic button
- this is still lighter than the real `AgentChatInputSection`:
  - no pending queue branch yet
  - no real manager-backed model config list yet
  - no full extra-settings popup system yet
  - no permission/tool/memory/runtime-config wiring yet

## 2026-04-30 Pending queue bridge checkpoint

- the input bridge now includes a first-pass queue workflow
- mirrored source component added:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/common/PendingMessageQueuePanel.kt`
- visible behavior now moves closer to `AgentChatInputSection`:
  - when processing is active and the user continues typing, the action button can switch into queue-add behavior
  - queued prompts are rendered in a dedicated queue panel
  - queued prompts can be edited, deleted, or sent directly
  - when the current processing run completes, the bridge can automatically continue into the next queued prompt
- this is an important fidelity step because the first screen now behaves more like an actual agent workbench instead of a single-shot input bar

## Latest build verification

- verified with `.\gradlew.bat assembleDebug`
- build date: `2026-04-30`
- debug APK path: `D:\10_Project\kiyori-android\app\build\outputs\apk\debug\app-arm64-v8a-debug.apk`

## 2026-04-30 Chat scroll navigator bridge update

- the chat-area control layer is now closer to `Operit` than before, even though the message widgets themselves are still local
- newly added source-package bridge:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatScrollNavigatorBridge.kt`
- `KiyoriOperitReplicaChatContent.kt` now uses source-style anchor-based navigation instead of only relying on “append messages then scroll to bottom”
- current visible improvements:
  - messages inside older hidden pages can now be reopened and jumped to through the locator path
  - the scroll-position chip and locator dialog now belong to the source transplant path, not to a new handwritten feature
  - pagination state and jump logic now follow `Operit` `ChatArea` more closely
- conclusion for fidelity assessment:
  - first-screen mismatch is still large, but the cause has shifted further away from “wrong shell/control hierarchy”
  - the remaining largest chat-area gap is now message item behavior and exact layout rhythm, not missing scroll-navigation architecture

## 2026-04-30 Chat loading indicator update

- the visible “assistant is about to answer” rhythm is now closer to `Operit`
- newly added source component:
  - `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/LoadingDotsIndicator.kt`
- this source animation is now mounted inside the replicated chat stream instead of only being implied by bottom-bar processing state
- effect on audit status:
  - chat-area feedback timing is better aligned
  - the bigger remaining mismatch is still the local message item subtree, especially action surfaces and fine spacing
