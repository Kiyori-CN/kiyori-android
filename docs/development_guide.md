# 开发指南

本文档提供项目的构建、配置和开发说明。

---

## 🔧 构建项目

### 1. 克隆仓库

```bash
git clone https://github.com/azxcvn/mpv-android-anime4k.git
cd mpv-android-anime4k
```

### 2. 配置 DanDanPlay API 凭证

首先复制示例配置文件：

```bash
# 在 Windows 上
copy local.properties.example local.properties


然后编辑项目根目录的 `local.properties` 文件，填入你的 DanDanPlay API 凭证：

```properties
dandanplay.appId=your_app_id_here
dandanplay.appSecret=your_app_secret_here
```

#### 💡 如何获取凭证？

1. 前往 [DanDanPlay 开放平台](https://www.dandanplay.com/)
2. 注册账号并申请开发者权限
3. 创建应用并获取 AppId 和 AppSecret
4. 将凭证填入 `local.properties` 文件

> **注意**：`local.properties` 文件已被添加到 `.gitignore`，不会被提交到版本控制系统，你的凭证信息是安全的。



