# Notix — 架构与代码库概览

> 对应 **v8.57.0**（versionCode 203）`com.enlpot.notix`。与源码冲突时以源码为准。  
> 旧版全文（v8.15.2）见 [archive/v8.15.2/ARCHITECTURE.md](archive/v8.15.2/ARCHITECTURE.md)。

## 1. 项目概览

单模块 Android 应用：监听系统通知，按用户规则评估，按**有序动作链**处理（消除 / 打开 / 点击按钮 / 复制 / TTS / 等待）。  
**v8.57.0 起移除词频词云与 HanLP 分词插件，`INTERNET` 权限已删除，核心路径完全离线。**

| 属性 | 值 |
|---|---|
| 包名 | `com.enlpot.notix` |
| UI | Jetpack Compose + Material 3 |
| min / target / compile | 24 / 36 / 36 |
| 当前版本 | 8.57.0（203） |
| 协议 | MIT（DoNotNotify 分支） |

## 2. 模块与目录

```
app/
  src/main/java/com/enlpot/notix/
    NotixApp.kt                 # Application：诊断日志 + 崩溃日志 + 健康检查
    MainActivity.kt             # UI 根：状态、广播刷新、横竖屏导航
    NotificationBlockerService.kt # 监听引擎：保活 + 通知流水线 + ActionFlow 宿主
    BlockerRule.kt              # 规则模型 + 动作/条件枚举
    RuleMatcher.kt              # 纯 JVM 决策
    ActionFlowExecutor.kt       # 动作链串行执行
    RuleStorage.kt / RuleIds / RuleMutations / RuleImport / RuleWizardSupport
    SimpleNotification / NotificationHistoryEntry
    TtsSpeaker / RemoteViewsTextExtractor / NotificationColorEngine
    CrashLogManager / DebugLogManager / StatsStorage / …
    health/ setup/
    data/                       # Room：notix.db（分组 + 变更）
    ui/                         # screens / components / theme
  src/test / androidTest
```

## 3. 运行时入口

- **`NotixApp`**：`DebugLogManager` → `CrashLogManager` → 健康检查渠道 + `HealthCheckWorker`（6h）。
- **`MainActivity`**：无 Jetpack Navigation；未完成引导 → 主界面三/四 Tab（历史 / 规则 / 统计 / 设置）。  
  收到 `ACTION_HISTORY_UPDATED` 后 400ms 去抖刷新。  
  **注意**：横竖屏各有一套内容树，改页面时两处需同步（见 AGENTS.md）。
- **`NotificationBlockerService`**：`NotificationListenerService`；`specialUse` 前台保活；断线 `requestRebind`。

## 4. 数据模型（规则）

`BlockerRule`：多来源 App + 关键字 `RuleCondition` + `ExtraCondition`（屏幕/充电/勿扰/蓝牙/时段）+ `actions: List<ActionSpec>`。

`RuleAction`：`DISMISS` / `CLICK_BUTTON` / `OPEN_NOTIFICATION` / `COPY` / `TTS` / `STRONG_REMIND` / `DELAY` / `POSTPONE`。

| 动作 | 执行现状 |
|---|---|
| DISMISS / CLICK_BUTTON / OPEN / COPY / TTS / DELAY | 已实现 |
| STRONG_REMIND / POSTPONE | **执行层未实现**，Flow 记 `FAILED` 后继续 |
| MatchMode.ADVANCED | 匹配恒 false（UI 占位） |

决策：`RuleMatcher.planNotificationDecision` — **首条命中优先**。

## 5. 通知流水线（Service）

```
onNotificationPosted
  → 暂停 / 自包名守卫
  → 提取 title/text（必要时 RemoteViews，v8.53 起无文字通知总是尝试）
  → EnvironmentSnapshot（约 10s 缓存）
  → RuleMatcher
       Pass → 历史（blocked=false，跳过未监控 App）
       Apply → Action Flow（同 key ~3s 防抖）+ 历史 blocked=true + hitCount
  → 广播 ACTION_HISTORY_UPDATED
```

Action Flow：每次独立 `FlowExecution`；同步动作失败继续；TTS/DELAY 异步推进；宿主销毁不推进。

## 6. 存储

| 存储 | 用途 |
|---|---|
| `rules.json`（AtomicFile） | 规则；id-keyed 变更 + 全局锁 |
| Room `notix.db` | 通知聚合组 + 变更明细（主历史） |
| `app_info.db` | 包名 → 名称/图标 |
| SharedPreferences | 未监控、统计、冻结 snooze、设置 |
| `NotificationHistoryStorage`（JSON） | **死代码**，历史已迁 Room；文档归档见 archive |
| `BlockedNotificationHistoryStorage` | 仅启动时一次性迁移 |

Room version **7**：`word_frequency` 在 6→7 已 `DROP`（词云功能移除）。

## 7. UI

- 全 Compose + M3；主题令牌 `ui/theme/NotixColorScheme.kt`。
- 通知卡动态色：`NotificationColorEngine`（图标聚类 → 背景 + WCAG 文字）。
- 历史折叠：同 App 连续段 ≥4 折叠，`FoldToggleCard` 用品牌色半透明底。

## 8. 隐私与权限

**已声明权限**：通知监听、`POST_NOTIFICATIONS`、电池优化豁免、前台服务 specialUse。  
**无 `INTERNET`**（8.57 起）。不枚举全部已安装应用；规则 App 仅来自历史/规则。

## 9. 测试

- JVM：Action Flow 引擎 / 规则模型 / 导入导出 / 向导纯函数（覆盖较好）。
- 仪器化：Action Flow 与规则向导 Compose 流程。
- **缺口**：RuleMatcher、Room 仓储、迁移、API&lt;29 路径等。

## 10. 已知占位

- `STRONG_REMIND` / `POSTPONE` 可配置但执行失败。
- `repostNotification` / `RULE_REPOST_CHANNEL_ID`：旧 SILENT 模型残留。
- `ADVANCED` 匹配不可用。

---

更多开发者说明见 [DEVELOPER_GUIDE.zh-CN.md](DEVELOPER_GUIDE.zh-CN.md)。
