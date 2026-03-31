# WebDAV 使用说明

## 快速开始

### 1. 进入 WebDAV 管理

主页点击**云朵图标** → 进入 WebDAV 管理界面

### 2. 添加账户

点击**添加账户**，填写以下信息：
- **服务器地址**：如 `https://dav.jianguoyun.com/dav/`
- **显示名称**：自定义名称
- **登录模式**：选择账号登录或匿名访问
- **账号/密码**：WebDAV 凭证（坚果云需使用应用密码）

点击**测试连接** → 成功后**保存**

### 3. 浏览与播放

点击已保存的账户 → 浏览文件 → 点击视频即可播放

---

## 技术实现

### 核心组件

- **Sardine (OkHttpSardine)**：WebDAV 客户端库
- **EncryptedSharedPreferences**：凭证加密存储
- **Jetpack Compose**：现代化 UI 实现
- **OkHttp**：HTTP 请求处理

### 代码架构

```
webdav/
├── WebDavAccountManager.kt    # 多账户管理
├── WebDavClient.kt             # 客户端封装
├── WebDavComposeActivity.kt    # 账户管理页面
├── WebDavBrowserComposeActivity.kt  # 文件浏览
└── WebDavScreen.kt             # UI 组件
```

---

## 常见问题

**Q：连接失败？**  
检查：服务器地址格式、账号密码、网络连接

**Q：坚果云密码？**  
使用第三方应用密码，非登录密码
