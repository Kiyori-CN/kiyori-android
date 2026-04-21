# 仓库目录规范

本文档定义 `kiyori-android` 的目录职责、提交边界和清理原则，目标是让仓库长期保持可构建、可维护、可审计。

## 1. 根目录职责

- `app/`
  Android 应用主模块。只放应用源码、资源、Room schema、构建必须的本地二进制依赖和模块级配置。

- `browser-icons/`
  黑色图标库的规范来源目录。保留在仓库根目录，作为后续 UI 图标选型和复制来源；不要整库复制进 `app/src/main/res/`。

- `docs/`
  面向协作和发布的文档目录。包含 README 引用的截图、设计说明、构建说明和架构分析。

- `tools/`
  维护脚本目录。当前只保留 `generate_launcher_icons.py`，用于从根目录 `icon.png` 生成启动图标。

- `gradle/`、`gradlew`、`gradlew.bat`、`build.gradle`、`settings.gradle`、`gradle.properties`
  Gradle 构建系统文件，属于必须提交内容。

- `local.properties.example`
  本地配置模板，允许提交。

- `local.properties`
  本机私有配置，禁止提交。

- `icon.png`
  启动图标源图，供 `tools/generate_launcher_icons.py` 使用，属于应保留的设计源文件。

## 2. 提交边界

以下内容应提交到 GitHub：

- 可直接参与构建的源码、资源、配置和脚本。
- `app/libs/` 中构建必须的 AAR/JAR。
- `docs/` 中被 README 或技术文档引用的截图和说明。
- 当前命名空间下的 Room schema 历史文件。

以下内容不应提交到 GitHub：

- `build/`、`.gradle/`、`.cxx/` 等生成产物和缓存。
- IDE 工作区文件、临时导出物、补丁残留、日志和本机脚本草稿。
- 签名文件、密钥、令牌、真实 `local.properties` 和其他个人凭证。
- 没有被 README、文档或构建流程使用的临时媒体文件。

## 3. Room Schema 规则

- 统一保留在 `app/schemas/com.android.kiyori.database.VideoDatabase/`。
- 数据库版本升级时，继续在当前命名空间下追加对应版本的 JSON 文件。
- 旧包名或旧命名空间目录在迁移完成后应移除，避免历史残留继续污染仓库。

## 4. 图标与设计资产规则

- `browser-icons/` 是规范来源，不轻易移动或重命名。
- Android UI 优先从 `browser-icons/android-xml/` 复制实际需要的图标。
- 复制进应用模块的图标必须按 `ic_kiyori_*` 命名，避免与现有资源冲突。
- 只有 README 或文档明确引用的截图才应留在 `docs/screenshots/`。

## 5. 大文件与二进制管理

- `app/libs/` 中只保留构建不可缺少的二进制依赖。
- 如果新增大体积文件，提交前必须先判断它是否满足以下任一条件：
  - 参与构建且无法通过依赖仓库拉取。
  - 是 README 或文档明确引用的必要媒体资源。
  - 是后续设计协作必须保留的规范源文件。

不满足以上条件的大文件不应进入仓库。

## 6. 推送策略

- 只向 `origin` 推送。
- 只推送 `main` 分支。
- 其他远端只允许取回参考信息，不作为发布目标。

## 7. 提交前检查清单

提交或推送前至少检查以下事项：

1. `git status --short` 只包含本次需要的变更。
2. 没有把 `local.properties`、签名文件、缓存目录或临时文件带入提交。
3. 新增文档资源都被 README 或对应文档实际引用。
4. 变更后仍可执行 `./gradlew assembleDebug` 或 `gradlew.bat assembleDebug`。
