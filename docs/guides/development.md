# 开发指南

本文档提供 Kiyori 项目的构建、配置和开发说明。

---

## 环境要求

### 必需工具

- **Android Studio**：Arctic Fox (2020.3.1) 或更高版本，推荐使用最新稳定版
- **JDK**：JDK 17
- **Gradle**：项目使用 Gradle Wrapper，无需手动安装
- **Android SDK**：
  - 最低 SDK：26 (Android 8.0)
  - 目标 SDK：35 (Android 15)
  - 编译 SDK：35
- **Android NDK**：r25c 或更高版本（用于 libmpv 原生库）

版本维护约定：

- Gradle / AGP / Kotlin / AndroidX 版本统一维护在 `gradle/libs.versions.toml`
- 模块构建逻辑维护在 `app/build.gradle`
- Compose 编译器跟随 Kotlin Compose Gradle 插件维护，不再单独写死 `kotlinCompilerExtensionVersion`
- 本地敏感配置继续放在 `local.properties`

### 硬件建议

- **内存**：至少 8GB RAM（推荐 16GB+）
- **存储**：至少 10GB 可用空间（用于 SDK、NDK、依赖库和构建输出）
- **测试设备**：Android 12+ 的真机或模拟器

---

## 🔧 构建项目

### 1. 克隆仓库

```bash
git clone https://github.com/Kiyori-CN/kiyori-android.git kiyori-android
cd kiyori-android
```

### 2. 安装 Android Studio 和 SDK

1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio，通过 SDK Manager 安装必需组件：
   - Android SDK Platform 35
   - Android SDK Build-Tools 35.x
   - Android SDK Platform-Tools
   - Android Emulator（如果需要使用模拟器）

### 3. 配置 Android NDK

项目使用 libmpv 原生库，需要安装 NDK：

1. 在 Android Studio 中打开 SDK Manager
2. 切换到 "SDK Tools" 标签页
3. 勾选 "NDK (Side by side)"
4. 勾选 "CMake"（用于编译原生代码）
5. 点击 "Apply" 安装

或者在项目的 `local.properties` 文件中手动指定 NDK 路径：

```properties
ndk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk\\ndk\\25.2.9519653
```

### 4. 配置 DanDanPlay API 凭证

首先复制示例配置文件：

```bash
# Windows
copy local.properties.example local.properties

# Linux/macOS
cp local.properties.example local.properties
```

然后编辑项目根目录的 `local.properties` 文件，填入你的 DanDanPlay API 凭证：

```properties
# SDK 路径（Android Studio 自动生成）
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

# DanDanPlay API 凭证（手动填入）
dandanplay.appId=your_app_id_here
dandanplay.appSecret=your_app_secret_here

# NDK 路径（可选，如果自动检测失败）
# ndk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk\\ndk\\25.2.9519653
```

#### 💡 如何获取 DanDanPlay 凭证？

1. 前往 [DanDanPlay 开放平台](https://www.dandanplay.com/)
2. 注册账号并申请开发者权限
3. 创建应用并获取 AppId 和 AppSecret
4. 将凭证填入 `local.properties` 文件

> **注意**：`local.properties` 文件已被添加到 `.gitignore`，不会被提交到版本控制系统。

### 5. 打开项目

1. 启动 Android Studio
2. 选择 "Open" 或 "Open an Existing Project"
3. 导航到克隆的项目目录，选择根目录打开
4. Android Studio 会自动识别 Gradle 项目并开始同步

### 6. 同步 Gradle 依赖

首次打开项目时，Android Studio 会自动触发 Gradle 同步：

1. 等待 Gradle 下载依赖（可能需要几分钟）
2. 如果同步失败，点击工具栏的 "Sync Project with Gradle Files" 重试
3. 检查 "Build" 窗口查看同步日志

常见同步问题：

- **网络问题**：配置 Gradle 使用国内镜像（阿里云、腾讯云等）
- **NDK 未找到**：检查 NDK 是否正确安装，路径是否配置正确
- **JDK 版本不兼容**：确保使用 JDK 11 或更高版本

### 7. 构建项目

#### 通过 Android Studio 构建

1. 选择菜单 `Build` → `Make Project` 或按 `Ctrl+F9`（Windows/Linux）/ `Cmd+F9`（macOS）
2. 等待构建完成，查看 "Build" 窗口的输出信息
3. 如果构建成功，会显示 "BUILD SUCCESSFUL"

#### 通过命令行构建

```bash
# Windows
gradlew.bat assembleDebug

# Linux/macOS
./gradlew assembleDebug
```

构建产物位置：`app/build/outputs/apk/debug/app-debug.apk`

#### 构建 Release 版本

```bash
# Windows
gradlew.bat assembleRelease

# Linux/macOS
./gradlew assembleRelease
```

Release 版本需要签名配置，参考下面的"签名配置"部分。

### 8. 运行项目

#### 在真机上运行

1. 在手机上启用"开发者选项"和"USB 调试"
2. 用 USB 数据线连接手机到电脑
3. 在 Android Studio 中选择设备：工具栏的设备下拉列表
4. 点击 "Run" 按钮（绿色三角形）或按 `Shift+F10`

#### 在模拟器上运行

1. 打开 AVD Manager：`Tools` → `AVD Manager`
2. 创建新的虚拟设备（推荐 Pixel 系列，API 31+）
3. 启动模拟器
4. 在 Android Studio 中选择模拟器设备
5. 点击 "Run" 按钮

> **性能提示**：由于 Anime4K 超分算法对性能要求较高，建议在真机上测试，模拟器性能可能不足。

---

## 签名配置（可选）

如果需要发布 Release 版本，需要配置签名：

### 1. 创建密钥库

```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

### 2. 配置签名信息

在 `local.properties` 中添加：

```properties
storeFile=../my-release-key.jks
storePassword=your_store_password
keyAlias=my-key-alias
keyPassword=your_key_password
```

或者在 `app/build.gradle` 中硬编码（不推荐）：

```groovy
android {
    signingConfigs {
        release {
            storeFile file('../my-release-key.jks')
            storePassword 'your_store_password'
            keyAlias 'my-key-alias'
            keyPassword 'your_key_password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

---

## 常见构建问题

### 问题 1：NDK 未找到

**错误信息**：
```
NDK is not configured. Download it with SDK manager.
```

**解决方案**：
1. 通过 SDK Manager 安装 NDK
2. 在 `local.properties` 中指定 NDK 路径

### 问题 2：DanDanPlay API 凭证未配置

**错误信息**：
```
Missing dandanplay.appId or dandanplay.appSecret in local.properties
```

**解决方案**：
按照步骤 4 配置 `local.properties` 文件。

### 问题 3：Gradle 同步失败

**错误信息**：
```
Could not resolve all dependencies...
```

**解决方案**：
1. 检查网络连接
2. 配置 Gradle 使用镜像源（修改 `build.gradle`）
3. 清除 Gradle 缓存：`gradlew clean`

### 问题 4：内存不足

**错误信息**：
```
OutOfMemoryError: Java heap space
```

**解决方案**：
在 `gradle.properties` 中增加堆内存：

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError
```

---

## 调试技巧

### 使用 Logcat

1. 在 Android Studio 中打开 "Logcat" 窗口
2. 过滤日志标签：输入 `tag:MPV` 或 `tag:Danmaku` 等
3. 查看应用运行时的日志输出

### 使用调试器

1. 在代码中设置断点（点击行号旁边的空白区域）
2. 点击 "Debug" 按钮（绿色虫子图标）或按 `Shift+F9`
3. 应用会在断点处暂停，可以查看变量值和调用栈

### 性能分析

1. 使用 Android Studio 的 Profiler：`View` → `Tool Windows` → `Profiler`
2. 监控 CPU、内存、网络和能耗使用情况
3. 分析 Anime4K 超分渲染的性能瓶颈



