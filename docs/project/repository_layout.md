# 仓库目录规范

本文档定义 `kiyori-android` 的目录职责、提交边界和清理原则，目标是让仓库长期保持可构建、可维护、可审计。

## 1. 根目录职责

- `app/`
  Android 应用主模块。只放应用源码、资源、Room schema、构建必须的本地二进制依赖和模块级配置。

- `icons/`
  图标与图形资产目录。保留在仓库根目录，作为后续 UI 图标选型、复制和应用图标维护的规范来源；当前结构以“每个图标一个同名目录”为主，另含总预览图、说明文档和应用启动图标源文件；不要整库复制进 `app/src/main/res/`。

- `docs/`
  面向协作和发布的文档目录。当前已按主题分层，便于后续继续扩充文档而不再平铺堆积：
  - `docs/architecture/`：架构分析、浏览器设计、远程播放设计
  - `docs/guides/`：开发、WebDAV、.nomedia 等使用与维护指南
  - `docs/integrations/`：第三方 API 说明与 Bilibili 专项文档
  - `docs/planning/`：重构计划与阶段性方案
  - `docs/product/`：功能说明
  - `docs/project/`：仓库规范、源码目录地图
  - `docs/security/`：数据安全与隐私相关说明

- `tools/`
  维护脚本目录。当前按主题开始分层，`tools/icons/generate_launcher_icons.py` 用于从 `icons/软件图标/kiyori-app-icon-1024.png` 生成启动图标；后续新增脚本应优先放入对应主题子目录，而不是继续平铺在 `tools/` 根下。

- `gradle/`、`gradlew`、`gradlew.bat`、`build.gradle`、`settings.gradle`、`gradle.properties`
  Gradle 构建系统文件，属于必须提交内容。依赖与 SDK / 插件版本统一放在 `gradle/libs.versions.toml`，不要再分散到多个脚本里维护。

- `local.properties.example`
  本地配置模板，允许提交。

- `local.properties`
  本机私有配置，禁止提交。

- `icons/软件图标/kiyori-app-icon-1024.png`
  启动图标源图，供 `tools/icons/generate_launcher_icons.py` 使用，属于应保留的设计源文件。

## 2. 提交边界

以下内容应提交到 GitHub：

- 可直接参与构建的源码、资源、配置和脚本。
- `app/libs/` 中构建必须的 AAR/JAR。
- `docs/` 中实际维护使用的技术文档和说明。
- 当前命名空间下的 Room schema 历史文件。

以下内容不应提交到 GitHub：

- `build/`、`.gradle/`、`.cxx/` 等生成产物和缓存。
- IDE 工作区文件、临时导出物、补丁残留、日志和本机脚本草稿。
- 签名文件、密钥、令牌、真实 `local.properties` 和其他个人凭证。
- 没有被 README、文档或构建流程使用的临时媒体文件。
- `.tmp_*/` 这类临时导出目录。图标临时副本确认复制到 `app/src/main/res/drawable/` 后，应删除临时目录，只保留实际引用的资源。

## 3. Room Schema 规则

- 统一保留在 `app/schemas/com.android.kiyori.database.VideoDatabase/`。
- 数据库版本升级时，继续在当前命名空间下追加对应版本的 JSON 文件。
- 旧包名或旧命名空间目录在迁移完成后应移除，避免历史残留继续污染仓库。

## 4. 图标与设计资产规则

- `icons/` 是规范来源，不轻易移动或重命名。
- `icons/` 根目录当前包含 `README.md`、`_全部图标预览.png`、275 个标准图标目录，以及 `软件图标/` 目录。
- 标准图标目录采用“一个图标一个目录”的结构；每个目录内保留同名 `svg`、多尺寸 `png`、`webp`、`ico` 和白底 `png`。
- Android UI 从目标图标自己的目录中挑选实际需要的资源，不再依赖旧的 `icons/android-xml/`、`icons/png/`、`icons/svg/` 分层。
- 复制进应用模块的图标必须按 `ic_kiyori_*` 命名，避免与现有资源冲突。
- `icons/软件图标/` 单独存放应用品牌图标源文件，不与标准功能图标混放。
- 只复制实际使用到的图标进入应用模块，避免把整套图标资源带入 APK 或源码树。

## 5. 源码目录边界

- 当前项目仍保持单模块 `:app`，暂不照搬 `hikerView` 的多模块拆分；现阶段更适合先在单模块内部维持清晰的功能分包边界。
- 新增浏览器相关功能优先落在 `browser/`、`remote/`、`sniffer/`、`webdav/` 等业务包，而不是继续堆到 `utils/`。
- 新增播放器相关逻辑优先落在 `player/`、`danmaku/`、`subtitle/`、`media/`，避免把页面控制代码重新塞回 Activity。
- `manager/` 和 `utils/` 视为历史共享区。后续新增代码只有在明确跨功能复用时才进入这两个目录；否则优先归属到具体功能包。
- 当某个功能包规模继续增大时，先在包内细分 `data/`、`ui/`、`domain/`、`playback/` 等子目录，再评估是否值得独立 Gradle module。
## 6. 大文件与二进制管理

- `app/libs/` 中只保留构建不可缺少的二进制依赖。
- 如果新增大体积文件，提交前必须先判断它是否满足以下任一条件：
  - 参与构建且无法通过依赖仓库拉取。
  - 是 README 或文档明确引用的必要媒体资源。
  - 是后续设计协作必须保留的规范源文件。

不满足以上条件的大文件不应进入仓库。

## 7. 推送策略

- 只向 `origin` 推送。
- 只推送 `main` 分支。
- 其他远端只允许取回参考信息，不作为发布目标。

## 8. 提交前检查清单

提交或推送前至少检查以下事项：

1. `git status --short` 只包含本次需要的变更。
2. 没有把 `local.properties`、签名文件、缓存目录或临时文件带入提交。
3. 新增文档资源都被 README 或对应文档实际引用。
4. 变更后仍可执行 `./gradlew assembleDebug` 或 `gradlew.bat assembleDebug`。
