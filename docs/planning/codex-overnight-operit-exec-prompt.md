你正在 `D:\10_Project\kiyori-android` 持续推进一个明确工程：

目标：把正一屏尽可能 1:1 完美复刻 `D:\10_Project\Operit`，重点是 `AIChatScreen` 及其下游聊天子系统，不允许擅自改版，不允许为了省事做视觉近似替代。

每一轮执行时都必须先做这几件事：

1. 读取 `AGENTS.md`
2. 读取以下规划文档：
   - `docs/planning/operit-primary-screen-replication-plan.md`
   - `docs/planning/operit-primary-screen-component-mapping.md`
   - `docs/planning/operit-primary-screen-architecture-blueprint.md`
   - `docs/planning/operit-primary-screen-history-architecture.md`
   - `docs/planning/operit-primary-screen-progress-audit.md`
3. 对照 `D:\10_Project\Operit\app\src\main\java\com\ai\assistance\operit\ui\features\chat` 相关源码确认当前最高优先级差距

执行原则：

- 不要停在“分析完成”
- 不要停在“构建成功”
- 每一轮都要完成一个连贯的工程增量，再退出给下一轮继续
- 优先处理最影响“完全一样”的差距
- 不要回退或覆盖用户已有更改
- 只用 `apply_patch` 做文件编辑
- 每次代码改动后都要运行 `.\gradlew.bat assembleDebug`
- APK 输出只认 `D:\10_Project\kiyori-android\app\build\outputs\apk\debug`

当前优先级顺序：

1. `Workspace` 真实宿主化
2. `Computer` 真实宿主化
3. `ChatContent` 向 `ChatScreenContent + ChatArea` 靠拢
4. `InputBar` 向 `AgentChatInputSection` 靠拢
5. `History` 向原项目滑动/拖拽交互靠拢

每一轮结束前必须做：

1. 更新相关文档，尤其是 `docs/planning/operit-primary-screen-progress-audit.md`
2. 明确写出这轮完成了什么、还差什么、下一轮最该做什么
3. 运行 `.\gradlew.bat assembleDebug`
4. 在最终输出中写出 APK 路径和时间戳

如果遇到阻塞：

- 先自己排查和解决
- 只有在确实无法安全推进时，才在文档里写清楚 blocker，然后结束本轮
