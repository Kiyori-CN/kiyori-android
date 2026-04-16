# 内置浏览器详细设计

## 1. 文档目的

本文档用于冻结播放器内置浏览器功能的首期架构、交互、开发流程与安全边界，作为后续逐步开发的基线文档。

本次开发遵循以下原则：

- 先设计，后编码
- 先架构，后功能
- 先安全边界，后能力扩展
- 一次只完成一个阶段，阶段完成后再进入下一步
- 不直接移植 `hikerView` 浏览器整套实现，只参考其中适合当前项目的能力点

## 2. 项目背景

当前工程已经具备部分浏览器与远程播放基础能力：

- `app/src/main/java/com/fam4k007/videoplayer/tv/TVBrowserActivity.kt`
- `app/src/main/java/com/fam4k007/videoplayer/sniffer/VideoSnifferManager.kt`
- `app/src/main/java/com/fam4k007/videoplayer/sniffer/UrlDetector.kt`
- `app/src/main/java/com/fam4k007/videoplayer/remote/RemotePlaybackLauncher.kt`
- `app/src/main/java/com/fam4k007/videoplayer/remote/RemotePlaybackHeaders.kt`
- `app/src/main/java/com/fam4k007/videoplayer/remote/RemotePlaybackResolver.kt`

现状问题：

- 浏览器能力集中在 `TVBrowserActivity`，结构偏原型化
- UI、WebView 配置、嗅探、播放跳转耦合较高
- 适配 TV 的页面不适合作为手机浏览器长期演进基线
- 嗅探逻辑可继续增强，但目前缺少清晰的模块边界
- 缺少统一的浏览器安全策略和后续扩展接口

## 3. 总体目标

### 3.1 首期目标

实现一个面向手机的视频播放器内置浏览器，具备以下能力：

- 打开网页
- 输入和跳转 URL
- 后退、前进、刷新
- 显示加载进度与页面标题
- 对网页中的视频资源进行被动嗅探
- 将嗅探结果安全转换为播放器可消费的播放请求
- 在现有主题体系下提供一致的手机端 UI

### 3.2 首期不做

- 不引入 X5 内核
- 不引入 `hikerView` 的 Rhino 规则引擎
- 不引入任意 JS 注入系统
- 不引入自定义 `JavascriptInterface` 桥接
- 不做多标签页
- 不做广告拦截
- 不做通用下载器
- 不做站点规则市场

### 3.3 二期可选目标

- 浏览记录和收藏
- 桌面版 User-Agent
- 轻量页面解析器
- 广告拦截
- 站点级规则
- 多标签页
- 下载面板

## 4. 设计原则

### 4.1 架构原则

- UI 层不直接处理播放参数拼装
- WebView 生命周期与 Compose 页面解耦
- 嗅探能力与浏览器视图分离
- 安全策略统一收口，不散落在多个 Activity 中
- 所有浏览器状态以明确数据结构表达

### 4.2 安全原则

- 默认不开启任何 `JavascriptInterface`
- 默认不开放任意本地文件访问能力
- 仅允许 `http` 与 `https` 作为默认可直接加载 scheme
- 外部 scheme 必须走显式确认和白名单策略
- 播放请求头字段继续使用白名单模式
- Cookie、Authorization 等敏感数据日志必须脱敏
- 页面资源嗅探只做被动分析，不允许页面主动触发播放器执行

### 4.3 开发原则

- 每个阶段先完成设计边界，再写代码
- 每个阶段保留可回退点
- 每个阶段都必须有验收标准
- 每个阶段结束后进行一次人工回归

## 5. 模块架构

建议新增浏览器域目录：

```text
app/src/main/java/com/fam4k007/videoplayer/browser/
  data/
  domain/
  playback/
  security/
  sniffer/
  ui/
  web/
```

### 5.1 `browser/ui`

职责：

- 浏览器界面呈现
- 地址栏、工具栏、面板、错误态展示
- 将用户交互转换为浏览器命令

建议文件：

- `BrowserActivity.kt`
- `BrowserScreen.kt`
- `BrowserTopBar.kt`
- `BrowserBottomBar.kt`
- `BrowserPanels.kt`
- `BrowserMenuSheet.kt`
- `BrowserModels.kt`

### 5.2 `browser/web`

职责：

- 负责 `WebView` 的创建、配置、回调封装与生命周期
- 将 `WebViewClient` / `WebChromeClient` 事件转为结构化事件

建议文件：

- `BrowserWebViewController.kt`
- `BrowserWebViewFactory.kt`
- `BrowserWebViewClient.kt`
- `BrowserWebChromeClient.kt`
- `BrowserWebViewSettings.kt`

### 5.3 `browser/domain`

职责：

- 抽象浏览器状态
- 定义浏览器命令与页面状态
- 降低 UI 层和 WebView 层的耦合

建议文件：

- `BrowserPageState.kt`
- `BrowserNavigationState.kt`
- `BrowserCommand.kt`
- `BrowserSession.kt`

### 5.4 `browser/sniffer`

职责：

- 协调页面请求拦截
- 管理候选媒体结果
- 对结果去重、评分、排序

建议文件：

- `BrowserSnifferCoordinator.kt`
- `BrowserDetectedMedia.kt`
- `BrowserCandidateSelector.kt`
- `BrowserSnifferMapper.kt`

已有代码复用：

- `sniffer/VideoSnifferManager.kt`
- `sniffer/UrlDetector.kt`

### 5.5 `browser/playback`

职责：

- 将嗅探结果转换为 `RemotePlaybackRequest`
- 负责调用远程预解析与启动播放器

建议文件：

- `BrowserPlaybackInteractor.kt`
- `BrowserPlaybackMapper.kt`

复用：

- `remote/RemotePlaybackLauncher.kt`
- `remote/RemotePlaybackHeaders.kt`
- `remote/RemotePlaybackResolver.kt`

### 5.6 `browser/security`

职责：

- 收口浏览器安全策略
- 管理允许的 URL scheme、Header 白名单、调试策略

建议文件：

- `BrowserSecurityPolicy.kt`
- `BrowserExternalSchemeHandler.kt`
- `BrowserHeaderPolicy.kt`

### 5.7 `browser/data`

职责：

- 保存浏览器偏好
- 保存首页 URL、最近输入、桌面 UA 开关等

建议文件：

- `BrowserPreferencesRepository.kt`

## 6. 核心数据结构设计

### 6.1 页面状态 `BrowserPageState`

建议字段：

- `inputUrl: String`
- `currentUrl: String`
- `title: String`
- `isLoading: Boolean`
- `progress: Int`
- `canGoBack: Boolean`
- `canGoForward: Boolean`
- `isDesktopMode: Boolean`
- `errorState: BrowserErrorState?`
- `snifferCount: Int`
- `bestCandidateId: String?`
- `showUrlBar: Boolean`
- `showMenuSheet: Boolean`
- `showSnifferSheet: Boolean`

### 6.2 媒体候选 `BrowserDetectedMedia`

建议字段：

- `id: String`
- `url: String`
- `pageUrl: String`
- `title: String`
- `headers: LinkedHashMap<String, String>`
- `format: String`
- `detectedContentType: String?`
- `score: Int`
- `source: BrowserMediaSource`
- `timestamp: Long`

### 6.3 浏览器命令 `BrowserCommand`

建议命令：

- `LoadUrl`
- `GoBack`
- `GoForward`
- `Reload`
- `StopLoading`
- `ToggleDesktopMode`
- `PlayCandidate`
- `OpenExternal`
- `CopyCurrentUrl`
- `ClearCurrentSiteCookies`

## 7. 页面交互设计

## 7.1 手机端页面结构

浏览器页面采用三段式布局：

- 顶部：导航栏与地址栏
- 中部：网页区域
- 底部：工具栏

附加层：

- 顶部细进度条
- 底部嗅探面板
- 更多菜单底部弹层

### 7.2 顶部栏设计

左侧：

- 返回按钮

中间：

- 默认显示页面标题
- 点击地址区域切换为可编辑地址栏

右侧：

- 刷新 / 停止加载按钮
- 更多菜单按钮

行为规则：

- 加载中时右侧显示停止按钮
- 非加载中时显示刷新按钮
- 当前 URL 为空时默认展开地址栏

### 7.3 地址栏设计

输入框规则：

- 支持输入完整 URL
- 支持输入裸域名后自动补 `https://`
- 支持粘贴后直接前往
- 首期不支持搜索引擎混输

地址栏按钮：

- 清空输入
- 前往

### 7.4 底部工具栏设计

按钮从左到右：

- 后退
- 前进
- 首页
- 嗅探结果
- 播放最佳结果
- 更多

状态规则：

- 后退不可用时禁用
- 前进不可用时禁用
- 嗅探结果为空时显示禁用态
- 播放按钮仅在有最佳候选时高亮

### 7.5 嗅探结果面板设计

面板形式：

- 底部抽屉

内容结构：

- 当前页面标题
- 当前页面 URL
- 媒体候选列表

每个候选项显示：

- 格式标签
- URL 简要摘要
- 来源页信息
- 请求头状态
- 评分标识

每项操作：

- 播放
- 复制链接
- 查看请求头摘要

### 7.6 更多菜单设计

首期菜单项：

- 刷新页面
- 复制当前链接
- 外部浏览器打开
- 切换桌面版 UA
- 清除当前站点 Cookie
- 查看页面信息

延后菜单项：

- 收藏
- 历史记录
- 下载
- 调试信息

## 8. 状态流转设计

### 8.1 页面加载流

```text
输入 URL
  -> 归一化 URL
  -> BrowserWebViewController.loadUrl()
  -> onPageStarted
  -> 清理上一页嗅探结果
  -> 请求拦截中持续采集候选资源
  -> onProgressChanged
  -> onPageFinished
  -> 更新标题 / 可后退前进状态
```

### 8.2 嗅探流

```text
WebView shouldInterceptRequest
  -> BrowserSnifferCoordinator.onRequest()
  -> UrlDetector 判断是否像媒体资源
  -> 去重 / 合并 headers
  -> 评分排序
  -> 更新 UI 嗅探数量
  -> 选择最佳候选
```

### 8.3 播放流

```text
用户点击播放
  -> BrowserPlaybackInteractor.play(candidate)
  -> 生成 RemotePlaybackRequest
  -> RemotePlaybackResolver.resolve()
  -> RemotePlaybackLauncher.start()
  -> 打开 VideoPlayerActivity
```

### 8.4 失败流

```text
远程预解析失败
  -> 保留原始 URL
  -> 展示原因摘要
  -> 允许继续尝试播放
  -> 允许复制候选链接进行人工排查
```

## 9. WebView 安全配置策略

首期配置要求：

- `javaScriptEnabled = true`
  - 网页兼容所需，但仅开启脚本执行，不开放桥接
- `domStorageEnabled = true`
- `databaseEnabled = true`
- `mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW`
  - 当前项目已有远程视频场景，首期保持兼容，但后续需评估是否按站点策略收紧
- `allowFileAccess = false`
- `allowContentAccess = true`
- `allowFileAccessFromFileURLs = false`
- `allowUniversalAccessFromFileURLs = false`
- 不注册 `addJavascriptInterface`

外部 scheme 处理：

- `http` / `https`：允许直接加载
- `intent` / `market` / 第三方 App scheme：弹确认框后外部打开
- 未识别 scheme：默认拦截并提示

Cookie 策略：

- 首期使用系统 WebView 默认 Cookie
- 清除 Cookie 操作必须显式触发
- 日志输出中 Cookie 必须脱敏

## 10. 与现有代码的集成策略

### 10.1 保留现有入口

首期开发时保留当前：

- `TVBrowserActivity`

新建：

- `BrowserActivity`

后续策略：

- 手机端入口逐步切换到 `BrowserActivity`
- `TVBrowserActivity` 保留为 TV 定制页面或后续复用统一浏览器控制器

### 10.2 现有类复用策略

直接复用：

- `RemotePlaybackHeaders`
- `RemotePlaybackResolver`
- `RemotePlaybackLauncher`

部分复用并增强：

- `VideoSnifferManager`
- `UrlDetector`

不复用 `hikerView` 的直接代码实现：

- `X5`
- `JSEngine`
- `AdblockHolder`
- `WebViewActivity`

## 11. 阶段拆分

## 阶段 0：设计冻结

目标：

- 完成详细设计文档
- 冻结首期范围、边界、安全策略与 UI 结构

输出：

- 本文档

验收：

- 模块边界明确
- UI 按钮和交互定义明确
- 安全策略明确
- 开发顺序明确

## 阶段 1：架构骨架

目标：

- 建立浏览器模块目录与基础数据结构
- 抽离 `WebView` 控制器
- 不做复杂功能增强

输出：

- `browser/ui` 基础文件
- `browser/web` 基础文件
- `browser/domain` 基础数据结构

验收：

- 新浏览器页能打开 URL
- 顶部标题、地址、进度能联动
- 后退、前进、刷新正常

## 阶段 2：手机 UI 完整化

目标：

- 完成手机端浏览器完整 UI
- 实现顶部栏、底部栏、面板与菜单

输出：

- 手机浏览器完整交互页

验收：

- 竖屏 UI 完整可用
- 按钮布局稳定
- 交互逻辑闭环

## 阶段 3：嗅探能力整合

目标：

- 把当前嗅探逻辑整合进新浏览器模块
- 增强结果去重、排序、评分

输出：

- 嗅探面板
- 最佳结果播放入口

验收：

- 常见媒体资源可识别
- 页面切换不串结果
- headers 合并正确

## 阶段 4：播放链路标准化

目标：

- 将浏览器播放逻辑封装为独立交互器

输出：

- `BrowserPlaybackInteractor`

验收：

- 播放请求转换稳定
- 远程预解析与回退逻辑清晰

## 阶段 5：安全加固与回归

目标：

- 清理临时实现
- 收口所有安全策略
- 做阶段回归

验收：

- 无高风险桥接
- 无敏感日志泄露
- 外部 scheme 跳转可控

## 12. 验证与测试策略

### 12.1 每阶段验证

- 编译通过
- 浏览器基础跳转验证
- 页面返回栈验证
- 页面加载进度验证
- 嗅探结果验证
- 播放器跳转验证

### 12.2 关键手工测试场景

- 打开普通网页
- 打开含 `m3u8` 资源页面
- 打开含 `mp4` 资源页面
- 页面内重定向
- 有 Referer 限制的资源
- 播放器返回浏览器
- 地址栏手动输入错误 URL
- 外部 scheme 链接点击

### 12.3 用户协助测试节点

需要用户参与测试的阶段：

- 阶段 2：手机 UI 易用性与按钮布局
- 阶段 3：不同站点嗅探命中率
- 阶段 4：带请求头资源的实际播放成功率
- 阶段 5：真实设备回归

## 13. 风险与应对

风险 1：WebView 兼容性差异导致部分站点加载异常

应对：

- 首期只基于系统 WebView
- 配置保持保守兼容
- 复杂站点问题放入二期专项优化

风险 2：嗅探误判过多

应对：

- 增强 `UrlDetector`
- 引入评分和排除规则
- 在 UI 层区分“候选资源”和“最佳候选”

风险 3：请求头丢失导致播放器无法访问资源

应对：

- 统一由 `BrowserPlaybackInteractor` 进行转换
- 继续复用 `RemotePlaybackHeaders` 白名单逻辑
- 支持面板查看请求头摘要

风险 4：后续功能堆积导致浏览器 Activity 重新膨胀

应对：

- 严格按模块目录组织
- UI、Web、嗅探、播放分层

## 14. 当前结论

本项目的正确实施路径不是移植 `hikerView` 浏览器，而是：

- 复用当前项目已有远程播放和嗅探能力
- 新建统一浏览器模块
- 优先实现手机端可用的基础浏览器
- 在保证安全边界的前提下逐步增强媒体识别能力

## 15. 下一步

下一阶段进入“阶段 1：架构骨架”开发，改动原则如下：

- 新增浏览器模块目录
- 新建 `BrowserActivity`
- 提取 `BrowserPageState`
- 提取 `BrowserWebViewController`
- 先打通基本导航，不在该阶段引入嗅探增强
