# 第三方 API 使用说明

本应用使用以下第三方API服务。

---

## 哔哩哔哩 (Bilibili)

用于登录、番剧播放、弹幕下载、视频下载等功能。

### API 列表

- **登录API**: `https://passport.bilibili.com/x/passport-login/web/qrcode/*`
- **番剧信息API**: `https://api.bilibili.com/pgc/view/web/season`
- **番剧播放API**: `https://api.bilibili.com/pgc/player/web/playurl`
- **弹幕下载API**: `https://api.bilibili.com/x/v1/dm/list.so`
- **视频信息API**: `https://api.bilibili.com/x/web-interface/view`
- **视频下载API**: `https://api.bilibili.com/x/player/playurl`
- **番剧下载API**: `https://api.bilibili.com/pgc/player/web/playurl`

### 声明

本应用与哔哩哔哩无任何官方关联，仅使用其公开API。

---

## 弹弹play (DanDanPlay)

用于本地视频弹幕匹配和下载功能。

### API 列表

- **弹幕匹配API**: `https://api.dandanplay.net/api/v2/match`
- **弹幕搜索API**: `https://api.dandanplay.net/api/v2/search/episodes`
- **弹幕下载API**: `https://api.dandanplay.net/api/v2/comment/*`

### 配置说明

使用弹弹play API需要在 `local.properties` 中配置AppId和AppSecret。

获取凭证步骤：
1. 前往 [DanDanPlay 开放平台](https://www.dandanplay.com/)
2. 注册并申请获取 AppId 和 AppSecret
3. 将凭证填入项目根目录的 `local.properties` 文件

详见主文档的[开发说明](../README.md#开发说明)章节。

---

## 数据隐私

- ✅ 所有API调用均在**本地设备**上进行
- ✅ 不会将API返回的数据上传到第三方服务器
- ✅ 登录凭证使用 **AES-256 加密**存储在本地
- ✅ 用户可随时清除所有数据

更多隐私信息请参阅主文档的[隐私与第三方服务](../README.md#隐私与第三方服务)章节。
