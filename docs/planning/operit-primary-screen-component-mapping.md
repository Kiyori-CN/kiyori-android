# 正一屏 Operit 组件映射

## 目的

这份文档只解决一件事：后续复刻时，`Operit` 的哪一块源码应当落到 `kiyori` 的哪个目标文件里。

原则：

- 先对齐结构，再对齐样式细节
- 先做独立文件，再决定是否接真实能力
- 不再把任何新的 Operit 页面逻辑塞回 `HomeScreen.kt`

## 当前宿主边界

- `app/src/main/java/com/android/kiyori/app/ui/HomeScreen.kt`
  - 仅负责 pager 中的 `AiConversationPage()`
  - `AiConversationPage()` 仅负责挂载 `KiyoriOperitReplicaScreen(...)`
- `app/src/main/java/com/android/kiyori/operitreplica/ui/KiyoriOperitReplicaScreen.kt`
  - 当前唯一的正一屏运行入口
  - 后续拆分的源头文件

## Operit 到 Kiyori 的目标映射

### 启动与宿主层

- `Operit` `ui/main/MainActivity.kt`
  - `kiyori` 不照搬
  - 原因：`kiyori` 正一屏是首页 pager 子页，不是应用根入口

- `Operit` `ui/main/OperitApp.kt`
  - `kiyori` 不照搬
  - 只借鉴页面层级和状态组织，不迁移整套 app shell

### 页面总装层

- `Operit` `ui/main/layout/PhoneLayout.kt`
- `Operit` `ui/main/components/AppContent.kt`
- `Operit` `ui/main/screens/OperitScreens.kt`
  - `kiyori` 目标：
    - `app/src/main/java/com/android/kiyori/operitreplica/ui/KiyoriOperitReplicaScreen.kt`
  - 说明：
    - 当前先保留总装入口
    - 后续它只负责拼装 header / content / input / overlays

### 聊天主屏

- `Operit` `ui/features/chat/screens/AIChatScreen.kt`
  - `kiyori` 目标：
    - `app/src/main/java/com/android/kiyori/operitreplica/ui/KiyoriOperitReplicaScreen.kt`
    - `app/src/main/java/com/android/kiyori/operitreplica/state/KiyoriOperitReplicaViewModel.kt`
  - 说明：
    - UI 外壳和层级留在 screen
    - 页面状态逐步迁入 view model
    - `Screen` 只保留页面装配、动画和宿主事件桥接，不再本地推导会话内容与历史分组

### Header 区

- `Operit` `ui/features/chat/components/ChatScreenHeader.kt`
- `Operit` `ui/features/chat/components/ChatHeader.kt`
  - `kiyori` 目标：
    - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitHeader.kt`
  - 需要对齐：
    - 左侧历史入口
    - 中间角色头像/标题/下拉
    - 右侧上下文统计环与菜单

当前进度：

- 已有首个独立落点：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaHeader.kt`
- Header 所需的核心派生状态已进入 `KiyoriOperitReplicaViewModel`
  - `contextUsagePercentage`
  - `currentWindowSize`
  - `inputTokenCount`
  - `outputTokenCount`
- 后续应继续把命名和内部结构收敛到更接近 `Operit` 的 `Header` / `ScreenHeader` 分层

### 消息区

- `Operit` `ui/features/chat/components/ChatScreenContent.kt`
- `Operit` `ui/features/chat/components/ChatArea.kt`
  - `kiyori` 目标：
    - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaChatContent.kt`
    - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/message/KiyoriOperitReplicaMessageBubble.kt`
    - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/message/KiyoriOperitReplicaSystemMessage.kt`
  - 需要对齐：
    - assistant / user / system 三类消息
    - 顶部与 header 的遮挡关系
    - 滚动节奏和留白

当前进度：

- 已新增：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaChatContent.kt`
- 当前 `Screen` 已改为挂载独立的 `ChatContent`
- `activeMessages` 当前也已由 `uiState` 直接提供，`Screen` 不再本地兜底消息列表
- 已新增：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/message/KiyoriOperitReplicaMessageBubble.kt`
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/message/KiyoriOperitReplicaSystemMessage.kt`
- `ChatContent` 当前已改为挂载独立的 `message/` 子组件

### 输入区

- `Operit` `ui/features/chat/components/style/input/agent/AgentChatInputSection.kt`
- `Operit` `ui/features/chat/screens/AIChatScreen.kt` 中输入底栏相关部分
  - `kiyori` 目标：
    - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitInputBar.kt`
    - `app/src/main/java/com/android/kiyori/operitreplica/state/KiyoriOperitReplicaViewModel.kt`
  - 需要对齐：
    - 状态条
    - 附件 chips
    - 多行输入框
    - 左右工具入口
    - thinking / tools / workspace 等开关布局

当前进度：

- 输入文本 / reply preview / 附件列表 / capability toggles 已迁入：
  - `app/src/main/java/com/android/kiyori/operitreplica/state/KiyoriOperitReplicaViewModel.kt`
- 状态条与回复预览依赖的派生状态也已迁入：
  - `latestUserMessageText`
  - `statusStripText`
- `KiyoriOperitReplicaScreen.kt` 当前主要负责挂载 `InputBar`，不再自己持有这些输入区局部状态
- 消息提交时间戳已改为由 `ViewModel` 在发送动作内部生成

### 历史面板

- `Operit` `ChatHistorySelectorPanel`
  - `kiyori` 目标：
    - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitHistoryPanel.kt`
  - 需要对齐：
    - 左滑面板
    - 搜索
    - 分组会话列表
    - 新建会话入口

当前进度：

- 已有首个独立落点：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaHistoryPanel.kt`
- 历史搜索后的会话可见集与分组区段已开始由 `uiState` 直接提供：
  - `groupedConversations`
  - `activeConversation`
- 后续应继续把面板生命周期、搜索模式和操作入口进一步对齐到 `Operit` 的 `ChatHistorySelectorPanel`

### 角色与其他浮层

- `Operit` `CharacterSelectorPanel.kt`
- `Operit` `WorkspaceScreen.kt`
- `Operit` `ComputerScreen.kt`
  - `kiyori` 目标：
    - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaOverlays.kt`
  - 第二阶段再细分为：
    - `KiyoriOperitCharacterPanel.kt`
    - `KiyoriOperitWorkspacePanel.kt`
    - `KiyoriOperitComputerPanel.kt`

当前进度：

- 已新增：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaOverlays.kt`
- 其中已经接管运行时入口的浮层：
  - `FeaturePanel`
  - `AttachmentPanel`
- 其中已经接管运行时入口的提示与对话框：
  - `SwipeHint`
  - `FullscreenEditorDialog`
  - `HistorySettingsDialog`
- 本轮新增对位结构：
  - `CharacterSelector` 顶部浮层
  - `Workspace` 全屏保活层
  - `Computer` 全屏独立层
  - `WorkspacePreparing` 进入态遮罩
- 进一步补齐的 Operit 细节：
  - `CharacterSelector` 已接入排序状态
  - `HistorySettingsDialog` 已接入显示模式与联动开关状态
  - 会话与角色的绑定关系已进入 `ViewModel`
- 当前补充完成：
  - `KiyoriOperitReplicaScreen.kt` 中对应的大部分旧浮层残留已移入 `KiyoriOperitReplicaOverlayHost`
- overlay 可见性状态当前也已迁入：
  - `app/src/main/java/com/android/kiyori/operitreplica/state/KiyoriOperitReplicaViewModel.kt`

## 推荐拆分顺序

1. `model/`：先抽离会话、消息、菜单项、统计项数据结构
2. `KiyoriOperitHeader.kt`
3. `KiyoriOperitChatContent.kt`
4. `KiyoriOperitInputBar.kt`
5. `KiyoriOperitHistoryPanel.kt`
6. `KiyoriOperitOverlayPanels.kt`
7. `KiyoriOperitReplicaViewModel.kt`

## 明确禁止

- 不再把 Operit 页面实现写回 `HomeScreen.kt`
- 不以“看起来差不多”为目标直接改样式
- 不在未拆结构前继续叠加新的本地假功能
