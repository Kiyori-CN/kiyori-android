# Codex CLI 过夜持续工作方案

## 结论

可以做接近 `24h` 的无人值守持续推进，但不是靠当前聊天会话无限挂起，而是靠本地循环编排：

1. 第一轮启动 `codex exec`
2. 后续轮次用 `codex exec resume --last`
3. 每轮读取规划文档
4. 每轮完成一个连贯工程增量
5. 每轮构建并记录输出
6. 脚本自动进入下一轮

本仓库已经提供：

- 启动脚本：`tools/codex-overnight-operit.ps1`
- 轮次 prompt：`docs/planning/codex-overnight-operit-exec-prompt.md`

## 为什么不能只靠当前对话一直不结束

当前交互式会话适合持续推进，但不适合当作真正的后台守护进程。

原因很直接：

- 会话本身不是系统服务
- 单轮上下文和执行时间不是为无限时长设计的
- 一旦需要明确收口，本轮就会结束

所以正确做法不是“逼当前会话永不结束”，而是“把明确工程拆成一轮一轮的自动续跑”。

## 脚本行为

`tools/codex-overnight-operit.ps1` 会：

- 在指定工作目录启动第一轮 `codex exec`
- 成功后自动切到 `codex exec resume --last`
- 按 `Hours` 和 `MaxIterations` 控制总运行时长
- 把每轮的 `stdout / stderr / last message` 写到：
  - `docs/planning/logs/overnight/<timestamp>/`
- 最后生成 `run-summary.txt`

## 默认执行方式

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\codex-overnight-operit.ps1
```

默认参数：

- `Hours=24`
- `CooldownSeconds=10`
- `MaxIterations=200`
- 默认使用 `--full-auto`

## 更激进的无人值守方式

如果你明确接受更高风险，可以启用：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\codex-overnight-operit.ps1 -DangerouslyBypassSandbox
```

这会让脚本在每轮调用时使用：

```text
--dangerously-bypass-approvals-and-sandbox
```

只建议在你确认运行环境本身已经外部隔离时使用。

## 指定模型

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\codex-overnight-operit.ps1 -Model gpt-5.5
```

## 常用变体

跑 8 小时：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\codex-overnight-operit.ps1 -Hours 8
```

把轮次间隔改成 30 秒：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\codex-overnight-operit.ps1 -CooldownSeconds 30
```

限制最多 40 轮：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\codex-overnight-operit.ps1 -MaxIterations 40
```

## 当前限制

- `resume --last` 默认按当前工作目录取最近会话
- 如果你同时在同一目录手工跑别的 `codex` 会话，可能会干扰续跑目标
- 这不是绝对意义的“永不停止”，而是“在设定时间内尽可能自动续跑”

## 建议

如果你的目标就是“我睡觉时继续做 Operit 复刻”，那正确用法就是：

1. 白天先把规划文档和优先级校准好
2. 晚上运行 `tools/codex-overnight-operit.ps1`
3. 第二天查看 `docs/planning/logs/overnight/` 和最新 APK

这比强依赖当前单轮会话更稳，也更接近真正的持续工程推进。
