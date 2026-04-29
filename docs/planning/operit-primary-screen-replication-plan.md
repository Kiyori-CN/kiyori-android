# 正一屏完全复刻 Operit 方案

## 目标

- 目标页面：`kiyori-android` 首页右滑进入的“正一屏”
- 最终效果：用户进入正一屏时，感知上等同于进入 `D:\10_Project\Operit` 的主聊天页，而不是“参考 Operit 画了一个相似页面”
- 复刻范围不仅是视觉，还包括：
  - 页面层级
  - 顶栏/历史侧板/输入区/浮层关系
  - 交互节奏
  - 状态组织方式
  - 占位能力入口（工作区、角色切换、统计环、附件、历史、电脑模式入口等）

## 现状结论

当前 `kiyori` 已经存在一版“Operit 风格”页面，但本质是本地假数据 UI，不是真正的首页迁移。

- 当前入口：
  - `app/src/main/java/com/android/kiyori/app/MainActivity.kt`
  - `app/src/main/java/com/android/kiyori/app/ui/HomeScreen.kt`
- 当前正一屏实现：
  - `app/src/main/java/com/android/kiyori/app/ui/HomeScreen.kt:1808` `AiConversationPage()`
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/KiyoriOperitReplicaScreen.kt`
- 当前问题：
  - 运行入口虽然已迁出，但 `KiyoriOperitReplicaScreen.kt` 仍是单文件大实现
  - 会话、消息、统计、附件、侧板均为本地伪状态
  - 没有真实对齐 `Operit` 的组件树
  - 没有真实对齐 `Operit` 的状态入口、浮层关系和后续扩展点
  - 如果继续在这版大文件假复刻上追加功能，后面只会越来越难替换

结论：当前独立模块只能作为过渡骨架，不能作为最终架构继续无约束扩建。

## 当前重构里程碑

已完成：

- `AiConversationPage()` 已改为只负责挂载 `KiyoriOperitReplicaScreen(...)`
- 原先残留在 `HomeScreen.kt` 的旧 `OperitReplica*` 页面实现已经移除
- `HomeScreen.kt` 现在重新退回“宿主 pager 页面”职责，不再同时维护两套正一屏源码
- 当前运行中的正一屏源码入口已经唯一收敛到：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/KiyoriOperitReplicaScreen.kt`
- 已建立首个 `KiyoriOperitReplicaViewModel`
- 会话列表 / 当前会话 / 消息映射 / 历史搜索词 已从页面局部状态迁入 `state/`
- Header 已迁出为独立组件文件：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaHeader.kt`
- HistoryPanel 已迁出为独立组件文件：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaHistoryPanel.kt`
- InputBar 已迁出为独立组件文件，并且运行时入口已经切换到组件文件：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaInputBar.kt`
  - `KiyoriOperitReplicaScreen.kt` 当前只负责挂载 InputBar，不再继续把输入区实现扩写回主屏
- Overlay 面板文件已建立，FeaturePanel / AttachmentPanel 运行时入口已切换到组件文件：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaOverlays.kt`
  - `KiyoriOperitReplicaScreen.kt` 当前已改为挂载独立的 FeaturePanel / AttachmentPanel 组件
- SwipeHint / FullscreenEditor / HistorySettingsDialog 的运行时入口也已切换到 overlay 文件：
  - `KiyoriOperitReplicaScreen.kt` 当前已改为挂载独立的提示条与对话框组件
- `KiyoriOperitReplicaScreen.kt` 中对应的旧输入区 / 旧浮层死代码与假分支已清理，主屏当前只保留真实运行版装配逻辑
- Input / overlay / toggle / attachment 状态已迁入 `KiyoriOperitReplicaViewModel`
  - `KiyoriOperitReplicaScreen.kt` 当前主要负责从 `uiState` 读状态并装配组件
- Header / Input 直接消费的派生状态已迁入 `UiState`
  - 已包含：`latestUserMessageText`、`statusStripText`、`inputTokenCount`、`outputTokenCount`、`currentWindowSize`、`contextUsagePercentage`
- 当前会话派生与历史分组已开始从 `Screen` 下沉到 `ViewModel`
  - `activeConversation`
  - `activeMessages`
  - `groupedConversations`
  - 发送时间戳也已改为由 `ViewModel` 在提交时生成
- Overlay host 已开始按 `AIChatScreen` 的覆盖层顺序建出来
  - `KiyoriOperitReplicaOverlayHost`
  - `CharacterSelectorPanel` 对位层
  - `Workspace` 保活覆盖层
  - `Computer` 全屏独立覆盖层
  - `WorkspacePreparing` 进入态遮罩
- 角色和历史已开始建立 Operit 式联动
  - 会话现在带 `characterId`
  - 角色切换可选联动切会话
  - 会话切换可选联动角色
  - 历史设置已接入真实状态：`BY_CHARACTER_CARD / BY_FOLDER / CURRENT_CHARACTER_ONLY`
  - 角色面板已接入排序状态
- 消息区主内容已迁出为独立组件文件：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaChatContent.kt`
  - `KiyoriOperitReplicaScreen.kt` 当前不再直接展开 header + message list 的布局实现
- 消息渲染子组件目录已建立并接管运行时渲染：
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/message/KiyoriOperitReplicaMessageBubble.kt`
  - `app/src/main/java/com/android/kiyori/operitreplica/ui/components/message/KiyoriOperitReplicaSystemMessage.kt`

当前仍未完成：

- `KiyoriOperitReplicaScreen.kt` 仍是过大的单文件实现
- 当前 `ViewModel` 已接管会话、输入区、主要 overlay 状态以及 header / input 的核心派生数据，但 overlay host 和内容层装配还没有继续切成更接近 `Operit` 的状态切片
- 还没有按 `Operit` 的真实组件树完整拆出 content / input / overlays
- `Screen` 中虽然已经去掉历史分组、当前会话兜底和提交时间逻辑，但页面装配层仍然偏厚，后续还要继续向 `AIChatScreen` 的装配方式靠拢
- workspace / computer / character selector 已有首轮结构等价层，但目前仍是占位内容，后续还要替换成真实能力容器
- 历史显示模式与角色排序虽然已接通状态，但还没有完全对齐 Operit 的真实数据源与分组细节

## Operit 真首页基线

### 启动链路

`Operit` 的首页不是单一 Composable，而是完整启动流程的一部分：

1. `app/src/main/java/com/ai/assistance/operit/ui/main/MainActivity.kt`
2. 协议/权限/插件加载层
3. `OperitApp(...)`
4. `PhoneLayout(...)` / `TabletLayout(...)`
5. `AppContent(...)`
6. `Screen.AiChat`
7. `AIChatScreen(...)`

### 首页核心组件树

`AIChatScreen` 的真实主干：

- `AIChatScreen.kt:876` `CustomScaffold`
- `AIChatScreen.kt:935` `ChatScreenContent`
- `AIChatScreen.kt:1091` `ChatInputBottomBar`
- `AIChatScreen.kt:1131` `CharacterSelectorPanel`
- `AIChatScreen.kt:1172` `ChatHistorySelectorPanel`
- `AIChatScreen.kt:1222` `WorkspaceScreen`
- `AIChatScreen.kt:1262` `ComputerScreen`

`ChatScreenContent` 内部继续拆为：

- `ChatScreenHeader.kt:57` `ChatScreenHeader`
- `ChatHeader.kt:35` `ChatHeader`
- `ChatArea.kt:291` `ChatArea`
- `ChatScreenContent.kt:996` `ChatHistorySelectorPanel`

输入区核心入口：

- `AIChatScreen.kt:1472` `ChatInputBottomBar`
- `style/input/agent/AgentChatInputSection.kt:155` `AgentChatInputSection`

### 这意味着什么

`Operit` 首页不是“顶栏 + 消息列表 + 输入框”这么简单，而是：

- 一个聊天工作台
- 一个会话历史系统
- 一个角色/配置切换系统
- 一个工作区/网页工作面板系统
- 一个输入工具编排系统
- 一个顶部统计与模式切换系统

如果只抄视觉，不抄结构，最后不会像 `Operit`。

## kiyori 与 Operit 的关键差距

### 1. 入口层差距

`Operit`：

- 首页是应用主导航默认页
- 上层带协议、权限、插件加载和 app shell

`kiyori`：

- 正一屏只是 `HomeScreen` 中 `HorizontalPager` 的第 3 页
- 处于首页 pager 的局部区域

影响：

- `kiyori` 不能简单照搬 `OperitApp`
- 必须做“嵌入式复刻”：保留 `kiyori` 首页宿主，但把正一屏内部做成独立 Operit 风格子系统

### 2. 组件结构差距

`Operit`：

- 多文件拆分
- 结构清晰，组件职责稳定

`kiyori` 当前：

- 大量复刻逻辑堆在 `HomeScreen.kt`

影响：

- 第一优先级不是继续补 UI，而是先拆文件

### 3. 状态模型差距

`Operit`：

- `ChatViewModel` 驱动
- 多个 `StateFlow` 管理聊天、历史、输入、角色、工作区、统计

`kiyori` 当前：

- `rememberSaveable` 本地状态硬编码
- 无独立 ViewModel

影响：

- 想做到“完全复刻”，必须先建立 `KiyoriOperitReplicaViewModel`

### 4. 能力入口差距

`Operit` 首页包含真实入口：

- 历史侧板
- 角色切换
- 附件系统
- 工作区
- 电脑模式
- 上下文统计
- thinking/tools/max-context 等开关

`kiyori` 当前：

- 多数为展示性占位

影响：

- 需要先把入口和层级做真，再决定每个入口接真实能力还是接兼容占位

## 推荐实施策略

### 总策略

推荐走“结构先行，视觉对齐，状态落地，能力逐层接入”的路线，不建议继续在 `HomeScreen.kt` 中直接补细节。

### 不推荐方案

#### 方案 A：继续补当前 `OperitReplicaChatPage`

问题：

- 文件过大
- 状态不可维护
- 后续替换成本更高

#### 方案 B：整包搬运 `Operit` 的 `AIChatScreen`

问题：

- 依赖链过深
- 主题、导航、偏好、数据层全部耦合
- 实际落地成本远超“复刻首页”

### 推荐方案 C：Kiyori 内建立独立 Operit 首页模块

建议新建模块目录，逐步替换当前假复刻：

- `app/src/main/java/com/android/kiyori/operitreplica/`
  - `ui/`
  - `state/`
  - `model/`
  - `components/`

目标不是复制包名，而是复制结构和体验。

## 分阶段执行

### 阶段 0：冻结现状

目标：

- 把当前正一屏定义为“过渡假复刻”
- 停止继续往 `HomeScreen.kt` 堆新逻辑

动作：

- 文档确认当前参考文件
- 后续新工作全部迁出 `HomeScreen.kt`

### 阶段 1：建立独立页面骨架

目标：

- 从 `HomeScreen.kt` 中抽出正一屏页面

建议拆分：

- `KiyoriOperitScreen.kt`
- `KiyoriOperitHeader.kt`
- `KiyoriOperitMessageList.kt`
- `KiyoriOperitInputBar.kt`
- `KiyoriOperitHistoryPanel.kt`
- `KiyoriOperitOverlays.kt`
- `KiyoriOperitViewModel.kt`

完成标准：

- `HomeScreen.kt` 只保留 pager 挂载
- 正一屏内部不再继续定义一大串本地 UI 组件

### 阶段 2：先对齐真实层级，再对齐样式

必须先做成和 `Operit` 接近的层次：

1. 顶栏
2. 消息区
3. 底部输入区
4. 历史遮罩层 + 左滑面板
5. 角色切换面板
6. 工作区覆盖层
7. 电脑模式覆盖层

验收标准：

- 所有浮层关系正确
- 历史面板不是弹窗假层，而是侧滑面板
- 输入区和消息区的遮挡关系正确

### 阶段 3：对齐 Header

需要对齐：

- 左侧历史按钮
- PiP/模式按钮占位
- 中间角色头像 + 名称 + 下拉箭头
- 右侧上下文统计圆环 + 下拉菜单

对齐来源：

- `ChatScreenHeader.kt`
- `ChatHeader.kt`

### 阶段 4：对齐消息区

需要对齐：

- assistant / user / system 三类消息呈现
- assistant 头像、标签、气泡节奏
- 顶部 padding 与 header overlay 关系
- 滚动到底按钮
- 多消息操作的布局预留

对齐来源：

- `ChatArea.kt`
- `ChatScreenContent.kt`

### 阶段 5：对齐输入区

需要对齐：

- 状态条
- 附件 chip
- 多行输入
- 左侧附件/功能入口
- 右侧发送按钮
- thinking/tools/max-context/workspace 等开关区

对齐来源：

- `AIChatScreen.kt`
- `AgentChatInputSection.kt`

### 阶段 6：对齐历史侧板

需要对齐：

- 遮罩层
- 左滑进入
- 搜索框
- 分组会话列表
- 新建会话
- 会话切换/删除/重命名的结构预留

对齐来源：

- `ChatHistorySelectorPanel`
- `ChatHistorySelector`

### 阶段 7：对齐覆盖层

需要对齐：

- 角色选择面板
- 工作区层
- 电脑模式层

这里建议：

- 第一轮先保留“结构真、能力假”
- 第二轮再接入真实网页工作区或本地文件工作区

### 阶段 8：状态落地

建立独立状态模型：

- `conversation list`
- `active conversation`
- `message list`
- `header stats`
- `feature toggles`
- `attachment list`
- `overlay visibility`

当前落地进度：

- 已完成 `conversation list / active conversation / message list / header stats / attachment list / overlay visibility` 的首轮收敛
- 下一步重点不再是继续往 `Screen` 塞派生逻辑，而是把这些字段进一步整理成更接近 `Operit delegate` 的子状态切片

建议不要直接复制 `Operit` 的 ViewModel，而是做适配版：

- `KiyoriOperitReplicaViewModel`
- 只保留当前正一屏真的需要的数据

### 阶段 9：动效与手势修正

重点：

- 历史面板开合动效
- 底部输入区显隐/上移节奏
- 统计菜单弹出节奏
- pager 进入正一屏时的过渡

### 阶段 10：验收

只有同时满足以下条件，才算“完全复刻”：

- 静态视觉接近
- 页面层级一致
- 浮层关系一致
- 交互节奏一致
- 正一屏不再依赖假数据结构硬编码在 `HomeScreen.kt`

## 建议的目录重构

推荐新增：

- `app/src/main/java/com/android/kiyori/operitreplica/ui/KiyoriOperitReplicaScreen.kt`
- `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitHeader.kt`
- `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitChatContent.kt`
- `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitInputBar.kt`
- `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitHistoryPanel.kt`
- `app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitOverlayPanels.kt`
- `app/src/main/java/com/android/kiyori/operitreplica/ui/components/message/...`
- `app/src/main/java/com/android/kiyori/operitreplica/state/KiyoriOperitReplicaViewModel.kt`
- `app/src/main/java/com/android/kiyori/operitreplica/model/...`
- `app/src/main/java/com/android/kiyori/operitreplica/state/...`

推荐保留：

- `HomeScreen.kt` 只做宿主和 pager 切页

推荐下一轮拆分顺序：

1. 从 `KiyoriOperitReplicaScreen.kt` 抽出 `model/` 中的会话、消息、统计数据结构
2. 抽出 `ui/components/KiyoriOperitHeader.kt`
3. 抽出 `ui/components/KiyoriOperitHistoryPanel.kt`
4. 抽出 `ui/components/KiyoriOperitInputBar.kt`
5. 抽出 `ui/components/KiyoriOperitOverlayPanels.kt`
6. 最后再引入 `KiyoriOperitReplicaViewModel` 接管页面状态

## 关键参考文件清单

### Operit

- `app/src/main/java/com/ai/assistance/operit/ui/main/MainActivity.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/main/OperitApp.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/main/layout/PhoneLayout.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/main/components/AppContent.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/main/screens/OperitScreens.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/features/chat/screens/AIChatScreen.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatScreenHeader.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatHeader.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatScreenContent.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatArea.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/CharacterSelectorPanel.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/features/chat/webview/workspace/WorkspaceScreen.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/features/chat/webview/computer/ComputerScreen.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentChatInputSection.kt`

### kiyori 当前相关

- `app/src/main/java/com/android/kiyori/app/MainActivity.kt`
- `app/src/main/java/com/android/kiyori/app/ui/HomeScreen.kt`
- `app/src/main/java/com/android/kiyori/operitreplica/ui/KiyoriOperitReplicaScreen.kt`

## 近期执行顺序

建议下一轮按下面顺序做：

1. 保持 `HomeScreen.kt` 只做挂载，不再回填任何 Operit 细节
2. 继续把 `KiyoriOperitReplicaScreen.kt` 按 `Operit` 组件树拆成多文件
3. 扩展 `KiyoriOperitReplicaViewModel`，继续把 chat/history/header/input 派生状态整理为更稳定的子切片
4. 再逐项替换当前假数据 UI
5. 最后再接真实能力入口

## 一个明确判断

如果目标是“完全照抄，进入正一屏就像进入 Operit”，那么后续工作的核心不是继续润色当前页面，而是把当前页面从“仿制草图”升级为“可持续维护的 Operit 子系统”。
