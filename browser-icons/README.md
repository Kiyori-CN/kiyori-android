# 浏览器图标库 · Browser Icons

一套为浏览器 APK 开发定制的专业图标库，**共 174 个图标**，严格基于用户提供的 9 张截图梳理，覆盖所有截图中出现的图标，并补充常用的浏览器功能图标。

## ✨ 核心特性

- **完整覆盖**：9 张截图中每一个图标都已包含，并额外补充常用图标
- **无重复**：所有图标唯一命名，严格按功能分类
- **AI 友好**：每个图标附带中文名、关键词、分类元数据，便于 AI 编程助手查找
- **多格式**：SVG、Android Vector Drawable XML、多密度 PNG 三种格式
- **主题化**：使用 `currentColor` / `?attr/colorControlNormal`，自动跟随主题色

## 📐 设计规范

| 项目 | 规范 |
|------|------|
| 画布 | 24 × 24 |
| 描边宽度 | 1.75px |
| 端点样式 | round（圆头）|
| 连接样式 | round（圆角）|
| 默认颜色 | `currentColor`（可被父元素颜色继承）|
| 风格 | 线条（outline），部分徽章使用填充 |

## 📦 目录结构

```
browser-icons-v2/
├── icons/                  ← 174 个原始 SVG 文件（最推荐）
├── android-xml/            ← 174 个 Android Vector Drawable
├── png/
│   ├── drawable-mdpi/      ← 24×24 px
│   ├── drawable-hdpi/      ← 36×36 px
│   ├── drawable-xhdpi/     ← 48×48 px
│   ├── drawable-xxhdpi/    ← 72×72 px
│   └── drawable-xxxhdpi/   ← 96×96 px
├── manifest.json           ← ⭐ AI 使用的元数据清单
├── preview.html            ← 交互预览页（搜索/换色/复制）
├── preview-grid.png        ← 全部图标总览图
└── README.md
```

## 🚀 使用方式

### Android 原生开发（推荐 Vector Drawable）

直接把 `android-xml/` 下的 XML 复制到 `app/src/main/res/drawable/`：

```xml
<ImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    android:src="@drawable/ic_search"
    android:tint="?attr/colorControlNormal" />
```

Vector Drawable 是单文件多密度，无需 PNG 多份，APK 体积更小。

### PNG 方式（兼容性）

把 `png/drawable-*/` 5 个文件夹直接复制到 `res/` 目录，Android 会自动选合适尺寸。

### WebView/H5/Flutter/RN

直接使用 `icons/` 下的 SVG 文件：

```html
<img src="icons/search.svg" width="24" style="color:#2563eb">
```

## 🤖 给 AI 编程助手的说明

`manifest.json` 是为 AI 工具准备的元数据清单，结构如下：

```json
{
  "icons": [
    {
      "name": "search",              // 英文名（用于文件查找）
      "cn": "搜索",                   // 中文名
      "category": "nav",              // 分类
      "keywords": ["search", "查找", "搜索"],  // 关键词
      "source_screenshots": ["image7", "image2"],  // 来源
      "file": "icons/search.svg"     // 相对路径
    }
  ]
}
```

**当 AI 需要为浏览器界面选图标时：**
1. 先读 `manifest.json` 了解有哪些图标
2. 根据功能在 `keywords`/`cn`/`name` 中匹配
3. 使用 `file` 字段指定的路径

## 📋 完整图标清单

> 全部图标按功能分 19 个类别，以下是每个分类的图标清单（中文名 / 英文名）

### 🧭 核心导航 (nav) · 22 个
搜索 / `search`、首页 / `home`、首页(五边形) / `home-pentagon`、首页(Q标识) / `home-q`、后退 / `back`、前进 / `forward`、刷新 / `refresh`、刷新(圆盘) / `refresh-disc`、刷新(顺时针) / `refresh-cw`、关闭 / `close`、关闭(圆圈) / `close-circle`、菜单 / `menu`、更多(横) / `more-horizontal`、更多(纵) / `more-vertical`、向上/下/左/右箭头 / `arrow-up/down/left/right`、尖角向上/下/左/右 / `chevron-up/down/left/right`

### 📑 标签页/窗口 (tab) · 8 个
标签页 / `tab`、多标签 / `tab-multi`、新建标签 / `tab-new`、关闭标签 / `tab-close`、画中画 / `picture-in-picture`、全屏 / `fullscreen`、全屏模式 / `fullscreen-corners`、退出全屏 / `fullscreen-exit`

### 🔖 书签 (bookmark) · 8 个
书签 / `bookmark`、加书签 / `bookmark-add`、书签实心 / `bookmark-filled`、书签双层 / `bookmarks-stacked`、书签收藏方形 / `bookmark-square`、星标 / `star`、星标实心 / `star-filled`、收藏该网页 / `star-add`

### ⏱️ 历史 (history) · 3 个
历史记录 / `history`、历史(实心) / `history-filled`、时钟 / `clock`

### ⬇️ 下载 (download) · 4 个
下载 / `download`、下载方框 / `download-box`、下载管理(实心) / `download-filled`、上传 / `upload`

### 👤 用户/账户 (user) · 8 个
用户 / `user`、用户实心 / `user-filled`、用户占位 / `user-placeholder`、登录 / `login`、退出登录 / `logout`、微笑脸 / `smile`、铃铛 / `bell`、关闭通知 / `bell-off`

### ⚙️ 设置/工具 (settings) · 5 个
设置六边形 / `settings`、设置齿轮 / `settings-gear`、工具箱公文包 / `toolbox`、工具箱包裹盒 / `toolbox-package`、插件 / `plugin`

### 🎬 媒体/影视 (media) · 10 个
相机 / `camera`、摄像机 / `video`、热搜好剧 / `video-dot`、播放 / `play`、播放圆圈 / `play-circle`、观影模式VR / `cinema-vr`、耳机 / `headphones`、音乐 / `music`、影视追剧 / `follow-drama`、AI搜影视 / `ai-video-search`

### 🤖 AI 相关 (ai) · 7 个
AI搜索 / `ai-search`、AI总结 / `ai-summary`、智能排序带星 / `sort-sparkle`、星火 / `sparkle`、机器人 / `robot`、QBot / `qbot`、聊天气泡 / `chat-bubble`

### 🔧 浏览器工具 (browser-tool) · 28 个
资源嗅探 / `resource-sniffer`、悬浮嗅探 / `float-sniffer`、屏蔽广告 / `ad-block`、标记广告层叠 / `ad-layers`、无图模式 / `no-image`、图片 / `image`、访问电脑版 / `desktop-mode`、移动端模式 / `mobile-mode`、平板模式 / `tablet-mode`、小说模式 / `novel-mode`、阅读模式 / `reader-mode`、阅读单页 / `reader-page`、无痕幽灵 / `incognito`、无痕文档款 / `incognito-doc`、UA标识苹果 / `ua-tag`、网站配置 / `site-config`、查看源码 / `view-source`、代码 / `code`、终端 / `terminal`、网络日志沙漏 / `network-log`、翻译 / `translate`、网页翻译中A / `translate-box`、网页朗读 / `read-aloud`、网页缩放 / `page-zoom`、页内查找 / `page-find`、定时刷新 / `timed-refresh`、放大 / `zoom-in`、缩小 / `zoom-out`

### 📁 文件/文档 (file) · 15 个
文件 / `file`、文档带内容 / `file-text`、保存为PDF / `file-pdf`、保存网页 / `save-page`、打印 / `print`、添加至桌面 / `add-desktop`、文件夹 / `folder`、文件夹加 / `folder-add`、文件工具 / `folder-arrow`、文件底栏导航 / `folder-download`、文档管理 / `folder-document`、打开的书 / `book-open`、小说 / `novel`、学习工具 / `study-tool`、扫描工具 / `scan-tool`

### 🔒 安全/密码 (security) · 6 个
钥匙横 / `key`、密码管理竖钥匙 / `key-vertical`、锁 / `lock`、解锁 / `unlock`、盾牌 / `shield`、盾牌保护 / `shield-check`

### 🎨 主题模式 (theme) · 5 个
太阳日间 / `sun`、月亮夜间 / `moon`、主题切换 / `theme`、壁纸T恤款 / `wallpaper-tshirt`、壁纸图片 / `wallpaper`

### ⚡ 通用操作 (action) · 25 个
加号圆圈 / `add-circle`、加号方框 / `add-square`、加号 / `plus`、减号 / `minus`、减号圆圈 / `minus-circle`、对勾 / `check`、对勾圆圈 / `check-circle`、编辑 / `edit`、编辑带框 / `edit-box`、删除 / `trash`、复制 / `copy`、分享三点 / `share`、分享箭头 / `share-arrow`、分享外链 / `share-box`、喜欢 / `heart`、显示 / `eye`、隐藏 / `eye-off`、信息 / `info`、警告 / `warning`、帮助 / `help`、筛选 / `filter`、排序 / `sort`、列表 / `list`、网格 / `grid`、推荐 / `recommend`

### 🌐 网络/云 (network) · 7 个
语音广播三弧 / `voice`、麦克风 / `mic`、WiFi / `wifi`、云 / `cloud`、云下载 / `cloud-download`、云上传 / `cloud-upload`、地球 / `globe`

### 📷 扫描 (scan) · 2 个
扫描 / `scan`、二维码 / `qr-code`

### 🎮 游戏 (game) · 2 个
游戏手柄 / `game`、游戏福利 / `game-x`

### 🔌 电源/存储 (power) · 6 个
电源 / `power`、关机 / `power-off`、存储 / `storage`、空间清理 / `storage-clean`、数据库 / `database`、沙漏 / `hourglass`

### 🔥 趋势/热门 (trending) · 3 个
趋势上升 / `trending`、火焰 / `fire`、热门带内火 / `hot`

## 🎯 版本对比 v1 → v2

- ✅ 补充了遗漏的图标：`voice`(语音)、`add-circle`(圆圈加号)、`add-square`、`view-source`(查看源码)、`reader-page`(单页阅读)、`home-pentagon`(五边形首页)、`home-q`(Q首页)、`folder-download`(文件底栏)、`star-add`(收藏该网页)、`user-placeholder` 等
- ✅ 去除重复和模糊命名
- ✅ 每个图标附带来源截图标注，便于追溯
- ✅ manifest.json 元数据清单，给 AI 使用

## 📝 许可

自由使用、修改、商用。
