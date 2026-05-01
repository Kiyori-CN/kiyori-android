# Kiyori 源码目录地图

本文档只描述当前仓库已经形成的目录边界，不引入新的功能设计。目标是让后续继续做浏览器、播放器、下载、WebDAV、Bilibili 等扩展时，先知道代码应该落在哪里。

## 1. 当前工程形态

- 当前是单模块 Android 工程，只包含 `:app`。
- 这一点和 `D:\10_Project\hikerView` 的多模块工程不同。`hikerView` 已经拆成大量 Gradle module，而 `kiyori-android` 现阶段更适合先把单模块内部的职责边界整理清楚，再决定是否拆模块。
- 当前真正需要稳定的是“功能分包边界”和“仓库层级清晰度”，而不是为了形式先做大规模 module 拆分。

## 2. 根目录职责

- `app/`：唯一应用模块，承载源码、资源、Room schema、本地依赖。
- `docs/`：技术文档，已按主题拆到 `architecture`、`guides`、`integrations`、`planning`、`product`、`project`、`security`。
- `icons/`：规范图标库与启动图标源文件。
- `tools/`：维护脚本。当前已开始按主题拆分，现有图标脚本位于 `tools/icons/`。

## 3. `app/src/main/java/com/android/kiyori/` 包地图

### 3.1 应用启动与入口

- `app/`：应用级入口、常量、基础 Activity、首页入口。
- `settings/`：设置页 Activity 与设置相关 Compose 页面。
- `ui/`：真正跨功能复用的 Compose UI 基础组件与主题色。

`MainActivity` 负责首启协议门禁、主题初始化和挂载 `HomeScreen`。主入口不再批量请求运行时权限；浏览器网页权限由 `browser/ui/BrowserPermissionCoordinator.kt` 按需处理，本地媒体读取权限由 `LocalMediaBrowserActivity` 按需处理。主页路由中需要特别注意 `TAB_AI = "ai"`：它不是底部独立 tab，而是映射到 `HomeTab.Home` 的 pager 第 2 页，也就是正一屏 AI 对话页。

### 3.2 浏览器主线

- `browser/`：内置浏览器主体，内部已拆出 `data`、`domain`、`playback`、`security`、`ui`、`web`、`x5`；其中网页权限映射策略在 `browser/security/BrowserWebPermissionPolicy.kt`，Activity Result 请求编排在 `browser/ui/BrowserPermissionCoordinator.kt`。
- `remote/`：远程 URL 播放输入、历史与请求解析。
- `sniffer/`：网页视频嗅探、URL 探测与媒体识别。
- `webdav/`：WebDAV 账户管理、客户端、文件浏览与 Compose 页面。WebDAV 播放通过 `RemotePlaybackRequest.headers` 传递认证头，不应把账号密码嵌入 URL。

这四块共同组成当前项目的“浏览器优先”主业务。

### 3.3 播放器与媒体主线

- `player/`：播放器核心控制、手势、缩略图、Anime4K、对话框与 Activity。
- `media/`：本地媒体浏览、分页、缓存实体与本地视频列表页面。
- `history/`：浏览历史、播放历史与对应抽屉/页面。
- `subtitle/`：字幕搜索、下载与模型。
- `danmaku/`：弹幕配置、弹幕渲染、下载与 Compose 页面。
- `dandanplay/`：弹弹play 接口与模型。

这几块构成“媒体能力层”，由 `player/` 作为核心中枢。

### 3.4 集成能力

- `bilibili/`：Bilibili 登录、模型与播放入口。
- `download/`：内置下载器、下载设置、下载状态与 Bilibili 下载能力。通用下载记录使用 `internal_downloads`，Bilibili 下载记录使用 `bilibili_downloads`，数据库入口统一在 `VideoDatabase`。

### 3.5 Operit 正一屏复刻

- `operitreplica/`：Kiyori 自有的 AI 对话复刻状态、模型、页面装配和组件适配。
- `com/ai/assistance/operit/**Bridge`：从 Operit 参考项目迁入或重写的 UI 桥接组件，当前服务 `operitreplica`，不是完整 Operit 运行时。
- 正一屏入口链路：`MainActivity.TAB_AI` -> `HomeTab.homePagerPage("ai")` -> `HomeScreen` pager page `2` -> `AiConversationPage()` -> `KiyoriOperitReplicaScreen()`。

### 3.6 数据与基础设施

- `database/`：Room 数据库入口。
- `worker/`：后台任务。
- `manager/`：历史遗留的共享管理器与播放器叠加层 Compose 组件。
- `utils/`：历史遗留通用工具。

`manager/` 与 `utils/` 当前仍有必要，但它们已经足够大。后续新增代码不要默认继续往这两个目录塞。

## 4. 资源目录地图

- `app/src/main/assets/shaders/`：Anime4K 着色器资源。
- `app/src/main/res/layout/`：仍在使用的 XML 页面与对话框布局，主要集中在播放器相关场景。
- `app/src/main/res/drawable/`：通用 shape、位图和实际落库使用的图标资源。
- `app/src/main/res/mipmap-*`：应用启动图标导出结果。
- `app/src/main/res/values*`：主题、颜色、字符串、样式。

## 5. 后续新增功能的落位规则

- 新浏览器能力：优先进入 `browser/`、`remote/`、`sniffer/`、`webdav/` 之一。
- 新播放器能力：优先进入 `player/`、`media/`、`subtitle/`、`danmaku/`。
- 新外部平台接入：优先单独建功能包，参考 `bilibili/`、`dandanplay/`。
- 新页面专属状态与模型：优先放在对应功能包内部，不要先放到 `utils/`。
- 只有明确跨多个功能复用的代码，才进入 `ui/`、`manager/`、`utils/`。

## 6. 如果未来要继续向 `hikerView` 靠拢

当前更适合先维持单模块，但如果后续代码量继续膨胀，最自然的候选拆分方向会是：

- 浏览器核心：`browser` + `remote` + `sniffer`
- 播放器核心：`player` + `subtitle` + `danmaku`
- 媒体数据：`media` + `history` + `database` + `worker`
- 外部集成：`bilibili` + `download` + `dandanplay` + `webdav`

在做到这一步之前，优先保证单模块内的目录边界清楚，比仓促拆 module 更重要。
