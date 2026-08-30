# Notix 项目 Agent 工作规则

## 指令6：修改代码前必须先评估影响范围
执行规则：任何代码修改前，必须先排查该改动可能波及的其他页面、组件、数据流、共享状态，避免改一处坏多处。
- 涉及公共组件（如 PagerScreenContent、RuleCard、底部导航）时，必须检查所有调用方。
- 涉及 MainActivity 结构变更时，注意竖屏分支（else 分支）有独立的 Box 结构，不调用 screenContent()，新增/修改页面必须两处同步。
- 涉及版本号、release 配置时，同步检查 RELEASE_NOTES.md 和 .github/workflows/release.yml。
- 评估结果应在修改前简要说明，修改后验证受影响功能。

## 指令7：Edit 工具频繁失败，优先用 PowerShell 改文件
执行规则：本环境下 Edit 工具和 Read 工具频繁返回 "Native execution failed"，不要先尝试它们。
- 修改文件：直接用 PowerShell 的 `Get-Content -Encoding UTF8 -Raw` 读取 + 字符串替换 + `Set-Content -Encoding UTF8` 写入。
- 行号精确修改：用 `$lines = Get-Content` + `$lines[N] = "..."` + `$lines | Set-Content`。
- 只有 PowerShell 方式也失败时，才考虑其他工具。
- 读取文件同理，优先用 `Get-Content`，不用 Read 工具。
