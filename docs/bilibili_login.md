# 哔哩哔哩登录实现

## 登录流程

### 1. 获取二维码

```
请求: https://passport.bilibili.com/x/passport-login/web/qrcode/generate
返回:
  - url: 二维码链接
  - qrcode_key: 用于轮询登录状态
```

### 2. 显示二维码

使用 ZXing 库将 url 转换为二维码图片并显示

```kotlin
val qrCodeWriter = QRCodeWriter()
val bitMatrix = qrCodeWriter.encode(
    url,
    BarcodeFormat.QR_CODE,
    size,
    size
)
```

### 3. 轮询登录状态

每3秒查询一次登录状态

```
请求: https://passport.bilibili.com/x/passport-login/web/qrcode/poll
参数: qrcode_key

状态码:
  86101 - 未扫码
  86090 - 已扫码，等待确认
  0 - 登录成功
  86038 - 二维码过期
```

```kotlin
while (isActive) {
    val result = authManager.pollQRCodeStatus(qrcodeKey)
    when (result.code) {
        0 -> break // 登录成功
        86101 -> {} // 继续等待
        86090 -> {} // 提示用户确认
        86038 -> break // 二维码过期
    }
    delay(3000)
}
```

### 4. 保存登录凭证

登录成功后提取关键 Cookie 字段并加密存储

```
关键字段:
  - SESSDATA
  - bili_jct
  - DedeUserID
  - DedeUserID__ckMd5
  - buvid3

加密方式: AES-256
存储位置: EncryptedSharedPreferences
```

---

## 技术实现

### 加密存储

```kotlin
// 使用 Android Jetpack Security 库
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "bilibili_auth",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### 凭证使用

在调用 B站 API 时，将保存的 Cookie 添加到请求头

```kotlin
val cookies = "SESSDATA=$sessdata; bili_jct=$biliJct; ..."
httpClient.newCall(
    Request.Builder()
        .url(apiUrl)
        .addHeader("Cookie", cookies)
        .build()
)
```

---

## 常见问题

**Q: 凭证有效期？**  
约一个月，过期后需重新登录

**Q: 扫码后需要等待确认？**  
应用通过轮询获取状态，建议等待界面显示"已扫码"后再确认
