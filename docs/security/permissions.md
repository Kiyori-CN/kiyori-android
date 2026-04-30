# Android 权限矩阵

本文档记录当前 Kiyori 仍声明或启动时请求的权限，以及保留原因。后续新增功能必须先更新这里，再修改 `AndroidManifest.xml` 或 `AppPermissionCoordinator`。

## 当前保留权限

| 权限 | 触发位置 | 保留原因 |
| --- | --- | --- |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Manifest | 浏览器、下载、WebDAV、远程播放、Bilibili 接口。 |
| `READ_MEDIA_VIDEO` | Manifest / `AppPermissionCoordinator` | Android 13+ 本地媒体读取。 |
| `READ_EXTERNAL_STORAGE` | Manifest / `AppPermissionCoordinator` | Android 12 及以下本地媒体读取。 |
| `WRITE_EXTERNAL_STORAGE` | Manifest / `AppPermissionCoordinator` | Android 10 及以下下载和旧文件写入兼容。 |
| `MANAGE_EXTERNAL_STORAGE` | Manifest / 本地媒体页 | 本地媒体浏览需要跨目录扫描；不在 App 启动时请求，用户进入本地媒体浏览并触发扫描时由 `LocalMediaBrowserActivity` 按需引导。后续应继续评估是否能降级到系统文件选择器或 MediaStore。 |
| `CAMERA` | Manifest / `BrowserActivity` WebView 权限请求链 | 浏览器网页拍摄、扫码、上传入口的运行基础；不在 App 启动时批量索取。 |
| `RECORD_AUDIO` | Manifest / `BrowserActivity` WebView 权限请求链 | 浏览器网页录音、语音输入入口的运行基础；不在 App 启动时批量索取。 |
| `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` | Manifest / `BrowserActivity` | WebView geolocation 按设置项控制，并在网页请求时二次确认，不在 App 启动时批量索取。 |
| `POST_NOTIFICATIONS` | Manifest | 预留给后续应用自有通知；当前代码没有 `NotificationManager` / channel，启动阶段不请求。系统下载器通知由系统 `DownloadManager` 处理。 |
| `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` / `NEARBY_WIFI_DEVICES` | Manifest | 预留给局域网、WebDAV、投屏/局域网发现类能力；当前不在 App 启动时批量索取。 |

## 已移除权限

本轮移除以下权限声明和启动请求，因为当前代码中没有明确业务入口：

- `CALL_PHONE`
- `ANSWER_PHONE_CALLS`
- `READ_PHONE_STATE`
- `READ_PHONE_NUMBERS`
- `SEND_SMS`
- `RECEIVE_SMS`
- `READ_SMS`
- `RECEIVE_MMS`
- `RECEIVE_WAP_PUSH`
- `ACTIVITY_RECOGNITION`
- `WRITE_SETTINGS`
- `REQUEST_INSTALL_PACKAGES`
- `QUERY_ALL_PACKAGES`

以下权限仍在 Manifest 中声明，但已经移出启动批量索权：

- `CAMERA`
- `RECORD_AUDIO`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_FINE_LOCATION`
- `NEARBY_WIFI_DEVICES`
- `MANAGE_EXTERNAL_STORAGE`
- `POST_NOTIFICATIONS`

如果后续确实需要电话、短信、安装包管理或应用枚举能力，必须先补业务入口、风险说明、用户可见解释和按需请求流程，不能恢复为启动时批量索权。

## WebView 权限策略

- `BrowserWebViewController` 只负责接收 X5/WebView 的 `PermissionRequest`，不直接无条件授权网页资源。
- `BrowserActivity` 负责把网页资源映射为 Android 运行时权限：
  - `RESOURCE_VIDEO_CAPTURE` -> `CAMERA`
  - `RESOURCE_AUDIO_CAPTURE` -> `RECORD_AUDIO`
- 如果 Android 权限已经授予，则只授予对应的网页资源。
- 如果权限缺失，则通过 Activity Result API 按需请求，结果返回后只授予用户实际允许的资源。
- 未识别的网页资源默认拒绝。
