# Kiyori Icons

`icons/` 是 Kiyori 的统一图标资产目录，包含浏览器图标库和应用品牌图标源文件。

## 目录结构

```text
icons/
├── app/
│   └── kiyori-app-icon-1024.png
├── android-xml/
├── png/
├── svg/
├── manifest.json
├── preview.html
└── preview-grid.png
```

## 用途约定

- `app/`
  应用品牌图标源文件。`kiyori-app-icon-1024.png` 是启动图标生成脚本的输入文件。

- `android-xml/`
  Android Vector Drawable 版本，适合直接复制到 `app/src/main/res/drawable/`。

- `png/`
  多密度位图导出，主要用于兼容场景或非 Android Vector Drawable 用途。

- `svg/`
  原始矢量文件，是最适合继续编辑和复用的源格式。

- `manifest.json`
  图标元数据清单，便于脚本或 AI 工具检索图标名称、分类和关键词。

- `preview.html` / `preview-grid.png`
  图标预览文件。

## 推荐使用方式

Android UI 优先使用 `android-xml/`。

只有在 Vector Drawable 不满足需求时，才回退使用 `png/` 或 `svg/`。

应用模块里只复制实际使用的图标，并统一采用 `ic_kiyori_*` 命名，避免资源冲突。
