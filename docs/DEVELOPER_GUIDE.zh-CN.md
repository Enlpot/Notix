# Notix 开发者指南

> 对应 **v8.57.0**。旧版（v8.15.2，含已废弃 JSON 历史主存储等描述）见 [archive/v8.15.2/DEVELOPER_GUIDE.zh-CN.md](archive/v8.15.2/DEVELOPER_GUIDE.zh-CN.md)。

## 环境

- JDK 17（本机常用 Adoptium 路径见根目录 `AGENTS.md`）
- Android SDK：`compileSdk 36`，模拟器示例 `Pixel_6_API_36`
- 代理：若拉依赖慢，见 `AGENTS.md` 中 Gradle 代理说明

```bash
./gradlew assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew installDebug
```

签名凭据：`local.properties` 或环境变量 `KEYSTORE_NOTIX_*`，勿入库。

## 改代码前

1. 读 [AGENTS.md](../AGENTS.md)（影响范围评估、Edit 工具偏好 PowerShell 等）。
2. 涉及 **MainActivity** 时：横屏用 `screenContent()`，竖屏 `else` 分支是另一套 `PagerScreenContent`，**两处同步**。
3. 涉及公共组件（`RuleCard`、底部导航等）时 grep 全部调用方。
4. 升版本时同步：`app/build.gradle.kts`、`RELEASE_NOTES.md`、`.github/workflows/release.yml`（push main 自动发 Release）。

## 核心代码地图

| 关注点 | 文件 |
|---|---|
| 通知主链路 | `NotificationBlockerService.kt` |
| 匹配 | `RuleMatcher.kt` |
| 动作链 | `ActionFlowExecutor.kt` |
| 规则持久化 | `RuleStorage.kt` + `RuleMutations.kt` + `RuleIds.kt` |
| 历史（Room） | `data/repository/NotificationHistoryRepository.kt` |
| 动态卡片配色 | `NotificationColorEngine.kt` |
| 历史折叠 UI | `ui/screens/HistoryScreen.kt`（`FoldToggleCard` / `foldSegments`） |

## 测试

- 纯逻辑优先放 `app/src/test`（Action Flow 已有较完整 Fake Host/Runner）。
- Service / Compose 流程放 `app/src/androidTest`。
- 注意：词频/插件相关测试与实现已在 8.57 移除。

## 构建与 CI

- `release.yml`：push 到 `main` 且 `versionName` 尚无对应 GitHub Release 时，构建签名 APK 并创建 `v{version}`。
- 插件模块与 `plugin-hanlp.yml` 已删除（8.57）。

## 文档维护

- 架构大改后更新 [ARCHITECTURE.zh-CN.md](ARCHITECTURE.zh-CN.md)，过时全文挪到 `docs/archive/`。
用户可见变更写入 [RELEASE_NOTES.md](../RELEASE_NOTES.md)。
- 根目录不再维护 `CHANGELOG.md`（旧清单在 `docs/archive/CHANGELOG.md`）；长文版本史同步 `VERSION_HISTORY*.md`。
