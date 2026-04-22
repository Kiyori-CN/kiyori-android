# Kiyori

**[中文版本](README.md) | [English Version](README_EN.md)**

> [!IMPORTANT]
> 当前主仓库：<https://github.com/Kiyori-CN/kiyori-android>

Kiyori 是一个面向 Android 的浏览器与媒体工具应用。

当前项目方向以浏览器体验为主，围绕内置浏览器、网页视频嗅探、远程播放、WebDAV、Bilibili 相关能力和本地媒体浏览构建；底层仍保留 `libmpv` 播放能力，用于承接网页嗅探、本地文件和远程媒体链接的统一播放入口。

## 声明

本项目遵守 [GPL-3.0-or-later](LICENSE) 开源协议，免费开源。

任何形式的二次分发都必须继续开源、遵守相同协议，并保留原作者及版权信息。

本项目仅用于技术学习、产品验证和个人测试。请勿将其用于侵权、盗版、绕过付费机制或其他违法用途。

## 项目定位

Kiyori 不是单一的“本地播放器”项目，而是一个浏览器优先的混合型 Android 应用：

- 内置浏览器作为主入口，支持网页访问、搜索、历史记录、书签和 UA 切换。
- 网页视频嗅探用于识别页面中的媒体资源，并转为统一播放请求。
- 远程播放支持手动输入 URL、WebDAV 和网页来源链接。
- 本地媒体模块继续保留，用于文件浏览、历史记录、字幕、弹幕和播放增强能力。
- Bilibili 相关登录、解析、下载与弹幕能力仍作为扩展模块存在。

## 核心能力

- 浏览器主页、地址栏搜索、历史记录、书签、无痕模式
- 网页视频嗅探与远程媒体链接播放
- WebDAV 远程文件浏览与打开
- 本地文件扫描、播放历史、字幕与弹幕支持
- Bilibili 登录、番剧解析、下载与相关媒体能力
- Anime4K、截图、播放进度记忆等媒体增强能力

完整功能说明见 [docs/product/features.md](docs/product/features.md)。

## 技术架构

- 浏览器层：Android WebView，预留 X5 扩展能力
- 媒体播放层：`libmpv`
- 语言：Kotlin + Java
- 数据存储：Room + SharedPreferences
- 网络层：OkHttp
- 远程文件：WebDAV
- 最低 SDK：Android 8.0+（`minSdk 26`）
- 编译 / 目标 SDK：34

## 技术文档

- [文档导航](docs/README.md)
- [项目构建引导](docs/guides/development.md)
- [仓库目录规范](docs/project/repository_layout.md)
- [源码目录地图](docs/project/codebase-map.md)
- [功能特性详细说明](docs/product/features.md)
- [内置浏览器详细设计](docs/architecture/browser.md)
- [项目架构分析](docs/architecture/project-overview.md)
- [远程 URL 播放设计](docs/architecture/remote-playback.md)
- [第三方 API 使用说明](docs/integrations/third-party-api.md)
- [数据安全文档](docs/security/data-security.md)
- [WebDAV 使用说明](docs/guides/webdav.md)
- [.nomedia 支持说明](docs/guides/nomedia.md)

## 依赖与参考

项目当前仍依赖或参考以下核心开源能力：

- [mpv-player/mpv](https://github.com/mpv-player/mpv)
- [mpv-android/mpv-android](https://github.com/mpv-android/mpv-android)
- [bloc97/Anime4K](https://github.com/bloc97/Anime4K)
- [bilibili/DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster)
- [SocialSisterYi/bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect)

第三方依赖和版权信息以 [LICENSE](LICENSE) 为准。

## 反馈

- 问题反馈与功能建议：<https://github.com/Kiyori-CN/kiyori-android/issues>
- 项目主页：<https://github.com/Kiyori-CN/kiyori-android>
- 组织主页：<https://github.com/Kiyori-CN>
