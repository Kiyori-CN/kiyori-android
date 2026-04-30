# Kiyori 项目审计报告（2026-04-30）

本文档记录本轮对 `D:\10_Project\kiyori-android` 的结构、配置、依赖、业务链路、历史残留和文档状态的检查结果。结论基于当前仓库源码、Gradle 配置、Manifest、文档目录，以及本地参考项目 `D:\10_Project\Operit`、`D:\10_Project\hikerView` 的结构对照。

## 1. 总体结论

Kiyori 当前是单模块 Android 工程，核心方向已经从“小喵播放器”式本地播放器演变为“浏览器优先的全能工具应用”。主入口集中在 `MainActivity` + `HomeScreen`，浏览器、远程播放、WebDAV、下载、Bilibili、播放器、设置和 Operit 正一屏复刻都在 `:app` 内按功能包组织。

当前架构可以继续支撑快速迭代，但已经进入“单模块大包需要强边界约束”的阶段。最需要优先治理的是：权限面过宽、若干核心 Compose/Activity 文件过大、下载持久化和断点续传仍未闭环、部分播放器时期命名继续外溢到全局主题与资源命名。

## 2. 当前模块划分

- `app/`：唯一 Android 应用模块。
- `com.android.kiyori.app`：启动、权限协调、主页容器。
- `com.android.kiyori.app.ui.HomeScreen`：底部五入口、主页三页横滑、负一屏、AI 正一屏挂载、文件/小程序入口编排。
- `com.android.kiyori.browser`：浏览器数据、领域模型、WebView 控制、安全策略、X5 扩展预留。
- `com.android.kiyori.remote`、`sniffer`、`webdav`：远程播放、网页媒体嗅探、WebDAV 文件浏览。
- `com.android.kiyori.player`、`media`、`history`、`subtitle`、`danmaku`、`dandanplay`：播放器与媒体能力层。
- `com.android.kiyori.download`、`bilibili`：下载与外部平台集成。
- `com.android.kiyori.settings`：设置 Activity 与 Compose 设置页面。
- `com.android.kiyori.operitreplica`：Kiyori 自有状态和页面装配。
- `com.ai.assistance.operit.*Bridge`：从 Operit 迁入或适配的 UI 桥接组件，只服务正一屏复刻，不是完整 Operit 运行时。

## 3. 与参考项目的关系

### 3.1 Operit

本地 Operit 是完整 AI 助手应用，包含 `AIChatScreen`、`ChatViewModel`、历史、工具、工作区、浮窗等运行时体系。Kiyori 没有直接接入完整运行时，而是引入一组 `Bridge` 组件和 `operitreplica` 本地状态，目标是复刻首屏视觉、历史面板、输入区和工作区观感。

这种策略是合理的：直接引入 Operit 的完整 chat runtime 会把模型、工具系统、工作区和配置体系一起拖入 Kiyori，耦合过高。当前缺点是桥接包仍使用 `com.ai.assistance.operit` 命名，容易让后续维护者误判为完整依赖。

### 3.2 hikerView

hikerView 是多模块、大量能力拆分的参考项目，包含解析、主页列表、广告拦截、下载、DLNA、播放器悬浮控制等成熟能力。Kiyori 当前仍是单模块，已经吸收了“浏览器 + 嗅探 + 播放 + 下载”的产品方向，但没有照搬 hikerView 的多模块 Gradle 结构。

现阶段不建议立刻拆成 hikerView 式多模块。更稳妥的路径是先在 `:app` 内保持 `browser`、`remote`、`sniffer`、`player`、`download`、`webdav` 等功能包边界，等某条业务稳定后再拆 Gradle module。

## 4. 问题分级

### 致命

本轮未发现会立即导致项目无法构建的致命结构问题。

### 严重

1. Manifest 权限面已经收缩，但仍需要持续按需化  
   位置：`app/src/main/AndroidManifest.xml`  
   描述：本轮已移除电话、短信、活动识别、写系统设置、未知来源安装、查询全部应用等无明确业务入口的高敏权限，并新增 `docs/security/permissions.md` 作为权限矩阵。定位、附近 Wi-Fi、相机、录音、全文件访问和通知都已退出启动时批量索权；定位由网页 geolocation 请求链按需触发，相机/录音由 WebView `PermissionRequest` 映射到 Android 运行时权限后按需触发，全文件访问由本地媒体页按需引导。  
   影响：权限面已经明显收窄，启动阶段目前只保留媒体读取等核心文件/媒体流所需运行时权限。  
   方案：继续评估 Android 13+ `READ_MEDIA_VIDEO` 是否也能从启动阶段延迟到本地媒体入口；全文件访问继续评估是否可降级到 MediaStore 或系统文件选择器。

2. 核心文件过大，修改风险集中  
   位置：`HomeScreen.kt`、`BrowserScreen.kt`、`VideoPlayerActivity.kt`、`SettingsScreen.kt`、`KiyoriOperitReplicaWorkspaceHost.kt`  
   描述：多个核心文件超过千行，`HomeScreen` 同时负责底部导航、三页横滑、负一屏、主页、文件/小程序入口和 AI 页挂载；设置页也把路由、页面、行组件、占位页混在单文件里。  
   影响：后续 UI 改动容易产生跨区域回归，代码审查成本高。  
   方案：按页面切片拆分文件，优先拆 `HomeScreen` 的 `MinusOne`、`HomeLanding`、`Apps`、`Files`、`BottomBar`，再拆 `SettingsScreen` 的 root、browser、player、placeholder。

3. 下载业务闭环未完成  
   位置：`BilibiliDownloadViewModel.kt`、`docs/architecture/project-overview.md`  
   描述：源码中仍有“从数据库加载下载项”“实现断点续传”的 TODO。  
   影响：下载中心状态恢复和中断恢复能力不可靠，和“全能浏览器”的下载器目标不匹配。  
   方案：先让普通下载任务进入 Room 持久化，再补 range 校验、临时文件恢复和失败重试。

### 一般

1. 全局主题仍使用 `Theme.VideoPlayer` 命名  
   位置：`app/src/main/res/values/themes.xml`、`AndroidManifest.xml`、`ThemeManager.kt`  
   描述：主题命名延续播放器项目时期，和当前浏览器优先定位不一致。  
   影响：不会造成功能错误，但会误导新维护者。  
   方案：新增 `Theme.Kiyori.*` 别名并逐步迁移引用，最后再清理旧名。

2. Operit 桥接包命名可能误导  
   位置：`app/src/main/java/com/ai/assistance/operit/**`  
   描述：Kiyori 只使用部分桥接 UI 组件，但包名看起来像完整 Operit 源码。  
   影响：后续开发者可能在错误包内继续扩展业务状态。  
   方案：短期在 AGENTS 和 docs 中明确“桥接只服务 UI 复刻”；长期迁移到 `com.android.kiyori.operitbridge` 或类似命名。

3. `manager/` 与 `utils/` 仍是历史共享区  
   位置：`app/src/main/java/com/android/kiyori/manager`、`utils`  
   描述：目录内既有播放器管理器，也有跨业务工具，边界不够清晰。  
   影响：新增功能容易继续堆进历史目录。  
   方案：新增代码默认进入具体功能包；只有明确跨功能复用才进入 `utils`。

4. `local.properties` 存在于工作区  
   位置：`local.properties`  
   描述：该文件被 `.gitignore` 忽略，当前不是提交风险，但仍是本机私有配置。  
   影响：复制/打包仓库时可能误带。  
   方案：继续只提交 `local.properties.example`，不要把真实配置纳入版本控制。

### 建议

1. 为主页与设置页补轻量 UI 路由说明  
   位置：`docs/project/codebase-map.md`、`docs/audit/`  
   方案：把 `MainActivity.TAB_AI -> HomeTab.homePagerPage("ai") -> page 2 -> KiyoriOperitReplicaScreen` 作为固定链路记录。

2. 为权限和外部 Intent 建立单独文档  
   位置：建议新增 `docs/security/permissions.md`  
   方案：按权限、触发功能、请求时机、是否必要、是否可移除维护表格。

3. 给 Operit 复刻链路补契约测试或截图验证  
   位置：`operitreplica/`  
   方案：短期用 debug 构建保证可编译，长期用 Compose screenshot 或 Playwright/设备截图流程验证布局。

## 5. 本轮清理

已删除：

- `.tmp_search_icons/`：9 个搜索引擎图标临时导出副本。对应文件已经存在于 `app/src/main/res/drawable/ic_kiyori_search_engine_*.png`，且源码引用的是 `R.drawable.ic_kiyori_search_engine_*`。
- `.tmp_search_icons_raw/`：3 个原始 `.ico` 临时下载文件，未被源码或文档引用。
- `docs/planning/logs/overnight/20260429-160404/`：两个 0 字节本机运行日志文件。

已补充 `.gitignore`：

- `.tmp_*/`，避免后续临时导出目录再次进入版本控制。

暂不删除：

- `app/src/main/res/layout/activity_video_player*.xml`：播放器 Activity 仍使用。
- `app/src/main/res/values/mpv_legacy_*.xml`：播放器/mpv 能力仍在构建与运行链路中。
- `com.ai.assistance.operit/**Bridge`：当前正一屏复刻仍引用。
- `docs/planning/operit-*`：仍是 Operit 复刻的阶段性依据，未确认过时前保留。

## 6. 后续优化路线

1. 继续权限按需化：评估 Android 13+ `READ_MEDIA_VIDEO` 是否也能从启动阶段延迟到本地媒体入口。
2. 再拆大文件：`HomeScreen` 和 `SettingsScreen` 优先，因为它们是后续 UI 迭代最高频入口。
3. 完成下载持久化与断点续传。
4. 逐步把 `Theme.VideoPlayer` 迁移到 `Theme.Kiyori`，保持别名过渡。
5. 给 Operit 桥接包建立明确边界：只作为 UI bridge，不承载 Kiyori 业务状态。
