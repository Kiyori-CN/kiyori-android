# 哔哩哔哩番剧解析播放

## 解析流程

### 1. 提取 ID

从输入链接中提取 Season ID 或 Episode ID

```
支持格式:
https://www.bilibili.com/bangumi/play/ss12345  -> season_id: 12345
https://www.bilibili.com/bangumi/play/ep67890  -> ep_id: 67890
https://b23.tv/xxxxx                           -> 需先重定向获取完整链接
```

```kotlin
val ssPattern = "ss(\\d+)".toRegex()
val epPattern = "ep(\\d+)".toRegex()
val seasonId = ssPattern.find(url)?.groupValues?.get(1)
val episodeId = epPattern.find(url)?.groupValues?.get(1)
```

### 2. 获取番剧信息

```
API: https://api.bilibili.com/pgc/view/web/season
参数: season_id 或 ep_id
请求头:
  User-Agent: 浏览器标识
  Referer: https://www.bilibili.com
  Cookie: 登录凭证（可选）
```

返回关键字段:

```json
{
  "result": {
    "season_id": 12345,
    "title": "番剧名称",
    "episodes": [
      {
        "id": 67890,
        "aid": 11111,  // AV号
        "cid": 22222,  // CID号
        "title": "第1话"
      }
    ]
  }
}
```

### 3. 获取播放地址

```
API: https://api.bilibili.com/pgc/player/web/playurl
参数:
  avid: 视频AV号
  cid: 视频CID号
  qn: 画质码（64=720P, 80=1080P, 112=1080P+, 116=1080P60）
  fnval: 格式（1=MP4, 16=DASH）
请求头:
  Cookie: 登录凭证（会员内容需要）
  User-Agent: 浏览器标识
  Referer: https://www.bilibili.com
```

**MP4 格式返回:**

```json
{
  "data": {
    "durl": [
      {"url": "https://..."}  // 直接播放地址
    ]
  }
}
```

**DASH 格式返回:**

```json
{
  "data": {
    "dash": {
      "video": [{"base_url": "https://..."}],  // 视频流
      "audio": [{"base_url": "https://..."}]   // 音频流
    }
  }
}
```

### 4. 播放视频

使用 libmpv 播放器

```kotlin
// DASH 格式（视频音频分离）
mpv.command(arrayOf("loadfile", videoUrl))
mpv.command(arrayOf("audio-add", audioUrl))
mpv.setOptionString("http-header-fields", 
    "Referer: https://www.bilibili.com,User-Agent: ...")

// MP4 格式（单文件）
mpv.command(arrayOf("loadfile", videoUrl))
mpv.setOptionString("http-header-fields", 
    "Referer: https://www.bilibili.com,User-Agent: ...")
```

---

## 技术要点

### 请求头要求

必须设置以下请求头，否则返回 403:

- `Referer: https://www.bilibili.com`
- `User-Agent: Mozilla/5.0 ...`

### 画质与会员

```
画质码对应关系:
16  = 360P
32  = 480P
64  = 720P
80  = 1080P
112 = 1080P+
116 = 1080P60
120 = 4K

权限要求:
未登录    -> 最高 480P
普通用户  -> 最高 720P
大会员    -> 1080P, 1080P+, 1080P60
年度大会员 -> 4K（视频支持时）
```

### DASH vs MP4

**DASH:**
- 视频音频分离
- 支持高画质（1080P+）
- 需要播放器合并流

**MP4:**
- 单文件直链
- 最高 720P
- 兼容性好

---

## 常见问题

**Q: 播放失败？**  
检查：会员权限、地区限制、Cookie 有效性

**Q: 画质不高？**  
原因：未登录、非会员、视频源限制

**Q: 播放卡顿？**  
降低画质或检查网络连接
