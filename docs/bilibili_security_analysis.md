# B站登录数据安全性说明

## 概述

B站登录信息（Cookie）通过 AES256-GCM 加密存储在本地，密钥保存在 Android KeyStore 中。

安全特性：
- 登录信息加密存储，其他应用无法读取
- 应用卸载后数据自动销毁
- 不上传到任何服务器，仅保存在本地设备

## 技术实现

### 1. 加密存储方案

使用 EncryptedSharedPreferences：
- 加密算法：AES256-GCM
- 密钥存储：Android KeyStore
- 密钥方案：AES256_SIV (键加密) + AES256_GCM (值加密)

### 2. 存储的数据内容

- Cookie：包含用户会话信息（SESSDATA、bili_jct、DedeUserID等）
- 用户信息：UID、用户名、头像链接等
- 刷新令牌：用于保持长期登录状态

### 3. 安全特性

已实现的保护措施：

1. 数据加密：所有 Cookie 和用户信息经过 AES256 加密
2. 硬件保护：密钥存储在 Android KeyStore，密钥受硬件级别保护
3. 应用隔离：Android 沙箱机制确保其他应用无法访问加密数据
4. 防篡改：GCM 模式提供认证加密，检测并拒绝被篡改的数据

## 潜在风险

### 1. Root 设备风险

拥有 Root 权限的用户理论上可以尝试访问 KeyStore。建议避免在 Root 设备上使用敏感账号，或定期退出登录。

### 2. 应用逆向分析

攻击者可能反编译应用分析代码逻辑。应用使用 ProGuard 代码混淆增加逆向难度。即使知道加密方式，没有密钥也无法解密。

### 3. 内存攻击

应用运行时，数据会短暂存在内存中（明文）。需要 Root 权限和专业内存调试工具才能实施攻击，且只能获取当前运行时的会话。

## 已实现的安全措施

- 使用 EncryptedSharedPreferences 加密存储
- 启用 ProGuard 代码混淆
- 提供退出登录功能
- 应用卸载时自动清除所有数据

## 安全建议

基本安全习惯：
1. 定期退出登录：长期不用时建议退出账号
2. 避免 Root 设备：Root 会降低整体安全性
3. 及时更新应用：安装最新版本获得安全修复
4. 启用屏幕锁：手机设置密码/指纹锁定

## 技术实现细节

### EncryptedSharedPreferences 工作原理

```
用户登录
   ↓
获取Cookie（明文）
   ↓
AES256-GCM加密 ← Android KeyStore提供密钥
   ↓
加密数据存储在手机
   ↓
需要使用时
   ↓
从KeyStore获取密钥 → 解密 → 使用（仅在内存中）
```

### 密钥管理机制

密钥生成：
```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()
```

特点：
- 密钥由 Android 系统自动生成
- 密钥永远不离开 Android KeyStore
- 应用卸载后，密钥自动销毁
- 重装应用会生成新密钥，旧数据无法解密

### 为什么选择这个方案

对比自己实现加密：
- 容易出现安全漏洞（如密钥硬编码）
- 密钥管理困难
- 容易被逆向破解

使用 Android 官方方案：
- 由 Google 安全团队维护
- 经过大量实际应用验证
- 自动处理密钥管理
- 持续更新和修复漏洞

## 总结

本应用使用 EncryptedSharedPreferences 加密方案，适用于视频播放器场景：
- 保护用户隐私
- 符合 Android 安全最佳实践
- 防止常见攻击手段
- 性能开销极小
