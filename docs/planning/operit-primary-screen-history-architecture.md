# 正一屏 Operit 历史面板架构说明

## 本轮目标

把 `kiyori-android` 正一屏历史侧栏从“单层 section 列表”升级为更接近 `Operit` `ChatHistorySelector` 的层级骨架，后续继续补长按管理、拖拽、删除、重命名时不再返工。

## Operit 的真实结构要点

`Operit` 历史面板不是简单的：

- 一个标题
- 一个搜索框
- 一堆会话行

而是更接近下面这棵树：

```text
HistoryPanel
  BindingBucket / CharacterBucket
    GroupHeader
      ConversationRow
      ConversationRow
```

其中：

- `BindingBucket` 在 `BY_CHARACTER_CARD` 模式下对应角色卡或角色组绑定桶
- `GroupHeader` 对应同一绑定桶下的文件夹/分组
- `ConversationRow` 才是实际会话项

## Kiyori 当前对齐方案

本轮已经在 `operitreplica` 模块内落下以下结构：

- `OperitReplicaConversation.groupName`
  - 用来表达 Operit 历史里的文件夹/分组概念
  - 替代之前用“今天 / 昨天 / 更早”硬分组的过渡实现

- `OperitReplicaConversation.characterGroupName`
  - 预留 Operit 里的“角色组绑定”语义
  - 后续可以直接把角色组数据源接到历史桶上，而不用再改会话模型

- `OperitReplicaConversationGroup`
  - 让历史分组本身成为独立实体
  - 支持“先建分组，后放会话”
  - 解决只有会话存在时才能显示分组的问题

- `OperitReplicaConversationSectionType`
  - `CHARACTER`
  - `GROUP`

- `OperitReplicaConversationBindingKind`
  - `CHARACTER_CARD`
  - `CHARACTER_GROUP`
  - `UNBOUND`
  - `GROUP_ONLY`

- `OperitReplicaConversationSection`
  - `key`
  - `title`
  - `subtitle`
  - `bindingKind`
  - `type`
  - `accent`
  - `characterId`
  - `groupName`
  - `childSections`
  - `conversations`

## 当前分组规则

### `BY_CHARACTER_CARD`

按下面顺序构建：

1. 先按 `conversation.characterId` 切成角色桶
2. 每个角色桶内部再按 `conversation.groupName` 切成组
3. 组内会话按 `sortKey` 倒序

也就是：

```text
Character
  Group
    Conversations(desc)
```

其中角色桶现在会直接携带语义标签：

- 正常角色卡：显示角色名 + 角色说明
- 角色组绑定：显示 `角色组绑定: xxx`
- 未绑定：显示 `未绑定角色卡`

### `BY_FOLDER`

按 `groupName` 直接分组：

```text
Group
  Conversations(desc)
```

### `CURRENT_CHARACTER_ONLY`

先筛出当前角色卡的会话，再按 `groupName` 分组。

当前实现还会把“当前角色卡名”作为组头补充说明直接带到 section 上，方便后面继续对齐 Operit 在该模式下的辅助提示。

空分组现在也能显示，因为 section 不再只从 `conversations.groupBy(...)` 推导，而是会合并 `conversationGroups` 实体。

## 为什么必须这样做

如果继续保留旧的一层 `section -> conversations` 结构，会直接卡住后续这些能力：

- 角色桶折叠
- 分组折叠
- 组级长按管理
- 角色卡切换与历史联动
- 更接近 `Operit` 的连接线与层级视觉

所以本轮不是“润色 UI”，而是把历史面板的数据骨架改到正确方向。

## 当前 UI 落点

`app/src/main/java/com/android/kiyori/operitreplica/ui/components/KiyoriOperitReplicaHistoryPanel.kt`

这份文件现在已经按两层头部渲染：

- `CharacterHeader`
- `GroupHeader`
- `ConversationRow`

并且已经有：

- 角色桶折叠
- 分组折叠
- 搜索空状态
- 嵌套连接线风格占位
- header subtitle / binding badge 的结构落点
- 分组长按管理入口
- 会话长按动作入口

## 当前已接通的动作链

这轮已经把 `Operit` 历史面板里最关键的“管理入口骨架”接成可运行状态：

- 分组长按
  - 重命名分组
  - 移出分组
  - 删除组内会话

- 会话长按
  - 重命名会话
  - 上移
  - 下移
  - 删除

- 新建分组
  - 已有独立入口
  - 已接 `ViewModel.createConversationGroup(...)`
  - 新建后即使组内没有会话，也会在历史面板结构中保留

当前这些动作已经接到了 `KiyoriOperitReplicaViewModel`，不是纯 UI 壳。

## 仍未完成的 Operit 对齐项

这轮只是把骨架拉正，下面这些还要继续补：

- 绑定桶里的“角色组绑定 / 未绑定角色卡”真实语义
- 组级长按菜单
- 会话项滑动操作
- 拖拽重排
- 真正的历史持久化数据源
- `CharacterSelectorPanel` 与历史绑定桶的完整联动

## 后续执行顺序

建议按下面顺序继续：

1. 把 `groupName` 从演示数据扩展到真实可编辑字段
2. 在历史面板补组级操作入口
3. 把会话项操作补成 `Operit` 式滑动/长按
4. 最后再接真实历史仓库和排序持久化

这样可以保证“结构先正确，再逐步逼近交互细节”，不会再次出现页面越来越像假复刻的问题。
