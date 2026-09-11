# Notix — 架构与代码库文档

> 本文档对应 **v8.15.2**（versionCode 131）的 `com.enlpot.notix`。文中所有文件、类、方法名均与 `app/src/main/java/com/enlpot/notix/` 下的实际源码一一对应；如与旧版本文档冲突，以本文件为准。

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 项目结构](#2-项目结构)
- [3. 构建系统与配置](#3-构建系统与配置)
- [4. 运行时入口](#4-运行时入口)
- [5. 数据模型](#5-数据模型)
- [6. 规则决策引擎（RuleMatcher）](#6-规则决策引擎rulematcher)
- [7. 动作流执行引擎（ActionFlowExecutor）](#7-动作流执行引擎actionflowexecutor)
- [8. 通知处理流水线（NotificationBlockerService）](#8-通知处理流水线notificationblockerservice)
- [9. 存储层](#9-存储层)
- [10. 支撑子系统](#10-支撑子系统)
- [11. UI 层](#11-ui-层)
- [12. 并发与一致性模型](#12-并发与一致性模型)
- [13. 隐私与离线设计](#13-隐私与离线设计)
- [14. 测试](#14-测试)
- [15. Android Manifest 与权限](#15-android-manifest-与权限)
- [16. 已知占位与限制](#16-已知占位与限制)

---

## 1. 项目概览

Notix 是一个**单模块** Android 应用：监听系统通知，按用户规则评估，并按规则的**动作链**对通知执行消除/点击/打开/复制/TTS 播报/延时等操作。完全离线运行，不声明任何网络权限。

| 属性 | 值 |
|---|---|
| 包名 / applicationId | `com.enlpot.notix` |
| 语言 | Kotlin（100%） |
| UI 框架 | Jetpack Compose + Material 3 |
| 最小 SDK | 24（Android 7.0） |
| 目标 / 编译 SDK | 36 |
| Java 目标 | 11 |
| 当前版本 | 8.15.2（versionCode 131） |
| 协议 | MIT |
| AGP / Kotlin | 8.13.2 / 2.0.21 |

### 核心依赖（`gradle/libs.versions.toml`）

| 依赖 | 版本 | 用途 |
|---|---|---|
| Jetpack Compose BOM | 2024.09.00 | UI 框架 |
| Material 3 | 1.4.0 | 设计体系 |
| Material Icons Extended | 1.7.8 | 图标库 |
| Gson | 2.13.2 | JSON 序列化 |
| Accompanist System UI Controller | 0.36.0 | 系统栏样式 |
| WorkManager (`work-runtime-ktx`) | 2.9.1 | 周期健康检查 |
| reorderable | 2.4.3 | 动作链拖拽排序 |
| core-ktx / activity-compose / lifecycle-runtime-ktx | 1.10.1 / 1.8.0 / 2.6.1 | AndroidX 基础 |

---

## 2. 项目结构

```
Notix/
├── .github/workflows/release.yml     # CI：自动构建并发布 GitHub Release（凭据走 Secrets）
├── app/
│   ├── build.gradle.kts              # 应用构建配置（签名、R8、Compose）
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/enlpot/notix/
│       │   │   ├── NotixApp.kt                  # Application：崩溃日志 + 健康渠道 + 健康检查
│       │   │   ├── MainActivity.kt              # UI 根：状态持有、广播刷新、导航
│       │   │   ├── NotificationBlockerService.kt# 监听引擎：前台保活 + 通知流水线
│       │   │   ├── BlockerRule.kt               # 规则模型 + 枚举 + 各动作参数
│       │   │   ├── RuleMatcher.kt               # 纯 JVM 决策引擎 + EnvironmentSnapshot
│       │   │   ├── ActionFlowExecutor.kt        # 动作链串行执行引擎 + 执行体接口
│       │   │   ├── RuleStorage.kt               # 规则持久化（AtomicFile + 锁）
│       │   │   ├── RuleIds.kt                   # 规则稳定 id 规范化
│       │   │   ├── RuleMutations.kt             # 纯函数规则变更辅助
│       │   │   ├── RuleImport.kt                # v4 envelope 导入/导出 + 净化
│       │   │   ├── RuleWizardSupport.kt         # 向导摘要/已知 App 合并等纯辅助
│       │   │   ├── SimpleNotification.kt        # 通知快照模型
│       │   │   ├── NotificationHistoryEntry.kt  # 聚合历史条目模型
│       │   │   ├── NotificationHistoryStorage.kt# 统一历史（聚合 JSON）
│       │   │   ├── BlockedNotificationHistoryStorage.kt # 旧版被拦历史（仅迁移用）
│       │   │   ├── StatsStorage.kt              # 拦截计数 + 按日通知量
│       │   │   ├── AppInfoStorage.kt            # App 图标/名称（SQLite）
│       │   │   ├── UnmonitoredAppsStorage.kt    # 未监控 App（SharedPreferences）
│       │   │   ├── NotificationActionRepository.kt # PendingIntent 内存缓存
│       │   │   ├── TtsSpeaker.kt                # TTS 播报 + 防抖
│       │   │   ├── RemoteViewsTextExtractor.kt  # 反射提取正文（默认关）
│       │   │   ├── NotificationColorEngine.kt   # App 图标取色 + hash 兜底
│       │   │   ├── CrashLogManager.kt           # 崩溃日志收集/查看
│       │   │   ├── ExternalLinks.kt             # 外部链接
│       │   │   ├── health/HealthCheckWorker.kt  # 周期监听健康检查
│       │   │   ├── setup/OemAutostart.kt        # 各厂商自启动设置页
│       │   │   ├── setup/SetupState.kt          # 引导向导步骤状态
│       │   │   └── ui/
│       │   │       ├── components/              # 可复用 Compose 组件
│       │   │       ├── screens/                 # 六个主屏面
│       │   │       └── theme/                   # 主题 + 语义令牌
│       │   └── res/                             # 资源、多语言 strings
│       ├── test/                                # JVM 单元测试
│       └── androidTest/                         # 仪器化测试
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml                    # 版本目录
├── gradle/wrapper/…
├── docs/                                       # ARCHITECTURE / API_REFERENCE / DEVELOPER_GUIDE
├── README.md / README.zh-CN.md
├── CHANGELOG.md / RELEASE_NOTES.md / VERSION_HISTORY*.md
└── LICENSE
```

> 注意：本仓库**没有** `assets/`、`fastlane/` 目录，也没有任何预置规则/预置规则仓库类。旧文档中提到的 `PrebuiltRulesRepository`、`StackedNotificationManager`、`BlockedScreen`、`Dialogs.kt`、`AboutDialog.kt` 等均已不存在。

---

## 3. 构建系统与配置

### 根构建（`build.gradle.kts`）

仅声明插件别名，不直接应用：

- `com.android.application`（AGP 8.13.2）
- `org.jetbrains.kotlin.android`（Kotlin 2.0.21）
- `org.jetbrains.kotlin.plugin.compose`

### 应用构建（`app/build.gradle.kts`）

**应用插件**：`android.application`、`kotlin.android`、`kotlin.compose`、`kotlin-parcelize`。

**签名（release）**：通过 `signingConfigs.create("notix")` 从 `local.properties` 或环境变量读取 `KEYSTORE_NOTIX_FILE / KEYSTORE_NOTIX_PASSWORD / KEYSTORE_NOTIX_ALIAS / KEYSTORE_NOTIX_KEYPASSWORD`；文件存在时才配置。**密钥库与密码不入库**（见 `.gitignore`）。

**构建类型**：
- `debug` — 默认调试配置
- `release` — `isMinifyEnabled = true`（R8，`proguard-android-optimize.txt` + `proguard-rules.pro`），使用 `notix` 签名配置

**其他**：`compileOptions`/`kotlinOptions` Java 11；`buildFeatures { compose = true; buildConfig = true }`；`testOptions.unitTests.isReturnDefaultValues = true`（JVM 单测 stub `android.util.Log`）。

### ProGuard（`proguard-rules.pro`）

规则极少。数据类依赖 `@Keep` 注解保证 R8 保留（Gson 反射反序列化需要）。

---

## 4. 运行时入口

### 4.1 `NotixApp`（`NotixApp.kt`）

`Application` 子类，`onCreate()` 依次：
1. `CrashLogManager.install(this)` — 安装全局崩溃日志（写 `crash_logs.txt`）。
2. `createHealthChannel()` — 创建健康检查通知渠道（`IMPORTANCE_HIGH`）。
3. `HealthCheckWorker.enqueue(this)` — 排队周期健康检查。

### 4.2 `MainActivity`（`MainActivity.kt`）

`ComponentActivity`，应用 UI 根与状态协调者：

- **初始化**：`enableEdgeToEdge()`；读本地数据（规则、历史、未监控 App、设置、统计）；启动时执行**旧版被拦历史一次性迁移**；注册 `ACTION_HISTORY_UPDATED` 广播接收器。
- **状态**：`setupDone`（未完成则进引导向导）、`selectedTab`（历史/规则/设置）、`rules`、`notifications`（聚合历史）、`unmonitoredApps` 等；用 `mutableStateOf` 驱动 Compose。
- **导航**：**无 Jetpack Navigation**，纯状态切换：引导向导（未完成时）→ 主界面（三 Tab）；设置、规则向导、存储占用、崩溃日志等为全屏覆盖层（bool/可空状态控制）。
- **广播刷新**：收到 `ACTION_HISTORY_UPDATED` 后 `scheduleHistoryRefresh`（400ms 去抖，IO 线程读盘）刷新历史/规则/统计。
- 提供 `triggerNotificationAction(...)`（历史详情里触发缓存 PendingIntent）、`restoreNotificationToShade(...)` 等与 Service 通信的入口。

### 4.3 `NotificationBlockerService`（`NotificationBlockerService.kt`）

`NotificationListenerService` 子类，应用的核心引擎（详见第 8 节）。要点：

- **前台保活**：`onListenerConnected()` 时 `startKeepAliveForeground()`（`specialUse` 类型前台服务）+ 每 1h heartbeat 更新 `last_listener_connected_ms`。
- **断线重连**：`onListenerDisconnected()` 在非暂停状态下 `requestRebind()`。
- **`onStartCommand` 动作**：
  - `ACTION_APPLY_RULE` — 按规则 id 回溯应用到当前活跃通知；
  - `ACTION_RESCAN_ALL` — 全量重扫当前活跃通知；
  - `ACTION_RESTORE_SNOOZED` — 恢复被规则冻结的常驻通知。
- **自包名守卫**：跳过 `BuildConfig.APPLICATION_ID` 与 `RULE_REPOST_CHANNEL_ID` 渠道的通知，防止递归处理（`RULE_REPOST_CHANNEL_ID` 渠道仍有定义，但重发函数 `repostNotification()` 为旧 SILENT 模型残留的死代码，见第 16 节）。
- **销毁**：`onDestroy()` 取消进行中的 Action Flow、5 秒延迟关闭线程池。

---

## 5. 数据模型

### 5.1 `BlockerRule`（`BlockerRule.kt`）

v7.37 起规则重构为「多来源 App + 关键字条件 + 手机状态条件 + **顺序动作链**」结构：

```kotlin
data class BlockerRule(
    val id: String = "",                  // 稳定 id（RuleIds 规范化，禁止改键）
    val description: String? = null,      // 可选规则名
    val isEnabled: Boolean = true,
    val hitCount: Int = 0,                // 命中次数
    val sourcePackages: List<SourceApp> = emptyList(),  // 多来源 App
    val condition: RuleCondition = RuleCondition(),     // 关键字匹配
    val extraCondition: ExtraCondition = ExtraCondition(), // 手机状态条件
    val actions: List<ActionSpec> = emptyList(),  // 动作链（顺序即执行顺序）
    val createdAt: Long = 0L,
) {
    val isValid: Boolean   // 至少 1 个来源 App + 动作链非空且全部有效
}
```

> 旧模型（`action`/`actionParams` 单动作字段）不再兼容：缺 `actions` 的规则被 `isValid=false` 判为无效，加载时过滤清空。

### 5.2 来源 App 与关键字条件

```kotlin
data class SourceApp(val packageName: String, val appName: String? = null)

data class RuleCondition(
    val mode: MatchMode = MatchMode.CONTAINS_ANY,
    val includeKeywords: List<String> = emptyList(),
    val excludeKeywords: List<String> = emptyList(),  // 仅 MIXED 使用
)
```

`MatchMode` 六态：

| 枚举 | 语义 |
|---|---|
| `CONTAINS_ANY` | 包含任一（A 组任一命中） |
| `CONTAINS_ALL` | 包含全部（A 组全部命中） |
| `NOT_CONTAINS_ANY` | 不包含任一（A 组全不命中） |
| `NOT_CONTAINS_ALL` | 不包含全部（任一不命中即中） |
| `MIXED` | 包含 A 且不包含 B |
| `ADVANCED` | 高级匹配（仅 UI 展示，暂不可用，匹配恒为 false） |

### 5.3 手机状态额外条件

```kotlin
data class ExtraCondition(
    val screenState: ScreenState = ScreenState.ANY,      // ANY / SCREEN_ON / SCREEN_OFF
    val chargingState: ChargingState = ChargingState.ANY,// ANY / WIRED / WIRELESS / BATTERY
    val dndState: DndState = DndState.ANY,               // ANY / ON / OFF
    val bluetoothState: BluetoothState = BluetoothState.ANY, // ANY / CONNECTED / DISCONNECTED
    val bluetoothDeviceNames: List<String> = emptyList(),    // 指定设备名，任一命中
    val time: TimeCondition = TimeCondition(),
)

data class TimeCondition(
    val enabled: Boolean = false,
    val startHour: Int = 0, val startMinute: Int = 0,
    val endHour: Int = 23, val endMinute: Int = 59,
    val weekdays: List<Int> = emptyList(),  // 1=周一 … 7=周日；空=每天
)
```

### 5.4 动作链：`ActionSpec` + `RuleAction`

```kotlin
data class ActionSpec(
    val type: RuleAction,
    val params: JsonObject? = null,   // Gson 原生对象，规避 sealed 反序列化难题
)
```

`RuleAction` 八种动作，按列表顺序**严格串行执行**：

| 动作 | 参数 | 执行现状 |
|---|---|---|
| `DISMISS` | `DismissParams(includeOngoing, snoozeDurationMs)` | ✅ 可清除通知 `cancel`；常驻通知 + `includeOngoing=true` 走 `snoozeNotification` 冻结 |
| `CLICK_BUTTON` | `ClickButtonParams(buttonLabel)` | ✅ 精确/包含匹配按钮 label → `actionIntent.send()` |
| `OPEN_NOTIFICATION` | 无 | ✅ `contentIntent.send()` |
| `COPY` | `CopyParams(mode: TITLE/TEXT/TITLE_AND_TEXT)` | ✅ 写入系统剪贴板 |
| `TTS` | `TtsParams(template)` | ✅ 模板占位符 `{app}/{title}/{text}` + 播报 |
| `STRONG_REMIND` | `StrongRemindParams` | ⚠️ **执行层 TODO**（v8.10 新增，未接入 heads-up/响铃/震动） |
| `DELAY` | `DelayParams(durationMs)` | ✅ `Handler.postDelayed` 等待后继续 |
| `POSTPONE` | `PostponeParams(delayMs)` | ⚠️ **执行层 TODO**（稍后重发通知未实现） |

辅助类型：`CopyMode`、`TtsParams`、`CopyParams`、`DelayParams`、`DismissParams`、`StrongRemindParams`、`PostponeParams`、`ClickButtonParams`；`ActionSpec.isValid` 对 CLICK_BUTTON（label 非空）、DELAY（durationMs>0 且 Long 范围安全校验）、POSTPONE（delayMs>0）做合法性判定。`SnoozeDurations` 提供冻结时长档位（1 小时/1 天/7 天/30 天/1 年，默认 7 天）。

### 5.5 `SimpleNotification`（`SimpleNotification.kt`）

通知快照模型：`appLabel / packageName / title / text / timestamp / wasOngoing / id`（`@Keep @Parcelize`）。

### 5.6 `NotificationHistoryEntry`（`NotificationHistoryEntry.kt`）

聚合历史条目（同 pkg + 同标题连续通知合并）：

```kotlin
@Keep data class NotificationHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String? = null,
    val appLabel: String? = null,
    val title: String? = null,
    val count: Int = 1,                 // 真实聚合次数（无 9+ 上限）
    val firstTimestamp: Long = 0L,
    val lastTimestamp: Long = 0L,
    val blocked: Boolean = false,       // 被规则过滤的组与普通通知不合并
    val changes: List<SimpleNotification> = emptyList(),  // 按时间倒序，[0] 为最新
)
```

---

## 6. 规则决策引擎（RuleMatcher）

`RuleMatcher`（`RuleMatcher.kt`）是**纯 JVM 单例 object**，无 Android 依赖，负责规则评估。决策模型：

```kotlin
data class EnvironmentSnapshot(
    val screenOn: Boolean = true,
    val charging: ChargingState = ChargingState.ANY,
    val dndOn: Boolean = false,
    val bluetoothDeviceNames: List<String> = emptyList(),  // 免权限读取 A2DP/SCO 设备名
    val now: Long = System.currentTimeMillis(),
)

sealed interface RuleDecision {
    data object Pass : RuleDecision          // 无规则命中 → 正常放行
    data class Apply(val rule: BlockerRule) : RuleDecision  // 命中 → 按 actions 执行
}
```

核心方法：

- **`evaluate(rule, packageName, title, text, env): Boolean`** — 单条规则完整判定：`isValid && isEnabled` → 来源 App 过滤 → `matchesCondition` → `matchesExtra`。
- **`matchesCondition(condition, title, text): Boolean`** — 按 `MatchMode` 对标题/正文任一字段做大小写不敏感的关键字匹配；无条件（A、B 均空）= 全部命中。
- **`matchesExtra(extra, env): Boolean`** — 屏幕/充电/勿扰/蓝牙状态/指定设备/时间段（含跨天）判断；支持 `orEmpty` 兜底旧 JSON 空字段。
- **`isTimeInRange(hour, minute, startH, startM, endH, endM): Boolean`** — 支持跨天区间（如 22:00–06:00）。
- **`planNotificationDecision(rules, packageName, title, text, env): RuleDecision`** — **决策入口**：按来源 App → 关键字 → 额外条件顺序扫描启用规则，**第一条命中即返回 `Apply`**，否则 `Pass`。

> 决策是「首条命中优先」模型，无 allowlist/denylist/stack 优先级矩阵（旧模型已删除）。

---

## 7. 动作流执行引擎（ActionFlowExecutor）

`ActionFlowExecutor`（`ActionFlowExecutor.kt`）将 `rule.actions` 按 List 顺序**严格串行执行**，负责 Flow 生命周期、失败继续、异步完成信号与 at-most-once 防重复推进。

### 7.1 关键设计

- **每次 `execute()` 创建独立 `FlowExecution`**：`currentIndex / failedActions / cancelled` 均为该 Flow 私有，多通知并发 Flow 互不污染；禁止全局 currentIndex。
- **同步动作**（DISMISS/CLICK_BUTTON/OPEN_NOTIFICATION/COPY）执行完立即推进；**抛异常 → 记录 FAILED → 继续下一个**，不终止 Flow。
- **异步动作**（TTS/DELAY）必须等 `onDone/onError` 或 `postDelayed` 到期后才推进。
- **at-most-once**：重复回调（onDone+onError / onDone+onDone）一律忽略。
- **宿主存活检查**：注入 Service 的 `!isDestroyed`，宿主销毁后挂起的 DELAY/TTS 回调不再推进。
- **线程模型**：引擎不持有 worker 线程池，调用方负责把 `execute()` 放到合适线程（Service 内用自己的执行器）；`FlowExecution` 内 `synchronized(this)` 保证 TTS/DELAY 回调线程与同步执行线程互斥。

### 7.2 运行时上下文与接口

```kotlin
class ActionContext(
    ruleId, packageName, appName, title, text,
    notificationKey, postTime,
    includeOngoing, snoozeDurationMs,
    sbn, notificationActions, contentIntent,   // 运行时对象，仅内存，绝不落 JSON
)

interface ActionFlowHost {            // 由 NotificationBlockerService 实现
    fun cancelNotificationCompat(key: String)
    fun snoozeNotificationCompat(key: String, ruleId: String?, durationMs: Long)
    fun copyToClipboard(text: String)
    fun buildTtsText(template, app, title, text, postTime): String
    fun speakTts(ctx, text, onDone: (Boolean) -> Unit)
}

interface SyncActionRunner  { fun dismiss/clickButton/openNotification/copy(...) }
interface AsyncActionRunner { fun runTts(...); fun runDelay(delayMs, onComplete) }
```

生产实现：`RealSyncActionRunner`（DISMISS 分派可清除/常驻、按钮匹配、contentIntent.send、剪贴板）、`RealAsyncRunner`（主线程 `Handler.postDelayed`、TTS 桥接 host）。JVM 单测注入 Fake 验证引擎逻辑。

### 7.3 Flow 终态

`FlowStatus`：`SUCCESS` / `PARTIAL_FAILURE`（有失败但走完）/ `EMPTY`（空链）/ `CANCELLED`（Service destroy 或外部取消）。结果携带 `failedActions: List<ActionFailure(index, type, reason)>` 与 `executedCount`。

---

## 8. 通知处理流水线（NotificationBlockerService）

`onNotificationPosted(sbn)` 主链路（全程有顶层 try-catch，异常写崩溃日志不崩溃）：

```
Android 投递通知 (StatusBarNotification)
  │
  ├─ 1. 暂停检查：listener_paused 为真 → 直接返回
  ├─ 2. 自包名守卫：packageName == BuildConfig.APPLICATION_ID
  │       或 channelId == RULE_REPOST_CHANNEL_ID → 跳过（防递归）
  ├─ 3. 提取 title/text（extras: android.title / android.text）
  │       ├─ 双空且开关 extract_remoteviews_text 开启（默认关）
  │       │    → RemoteViewsTextExtractor 反射提取
  │       └─ 仍空 → 忽略该通知
  ├─ 4. 解析 App 名 + AppInfoStorage 缓存图标/名称
  ├─ 5. 收集 EnvironmentSnapshot（屏幕/充电/勿扰/蓝牙/时间）
  ├─ 6. RuleStorage.getRules() + RuleMatcher.planNotificationDecision(...)
  │       ├─ Pass   → 走历史记录（未监控 App 跳过）
  │       └─ Apply  → executeActionFlow(rule)
  │
  ├─ 7. Apply 分支：Action-Flow 3s 防抖（同 sbn.key）
  │       → actionExecutor.execute(rule.actions, ctx)
  │       （单线程串行；STRONG_REMIND/POSTPONE 记日志跳过）
  │
  ├─ 8. 历史写入（独立 historyExecutor 单线程）：
  │       ├─ 命中规则 → hitCount+1 → saveNotification(blocked=true) + blocked 计数
  │       └─ 未命中   → 非未监控 → saveNotification(blocked=false)
  │
  ├─ 9. 广播 ACTION_HISTORY_UPDATED（刷新 UI）
  └─ 10. 清理过期防抖登记
```

**常量**：`ACTION_HISTORY_UPDATED = "com.enlpot.notix.HISTORY_UPDATED"`、`RULE_REPOST_CHANNEL_ID = "rule_repost"`、各类防抖/心跳常量。

**去重/防抖层次**（服务层）：
- `recentlyBlocked`（sbn.key，3s 窗口）— 同 key 短时间重复处理抑制；
- Action-Flow 3s 防抖 — 同 sbn.key 3s 内只执行一次动作链；
- TTS 5s 防抖（`TtsSpeaker`）— 同通知 5s 内单次播报；
- 存储层同条去重 + 头部聚合。

---

## 9. 存储层

| 存储类 | 载体 | 内容/要点 |
|---|---|---|
| `RuleStorage` | `rules.json`（AtomicFile） | 规则持久化；进程级缓存；**所有变更 id-keyed + 全局锁**，防「监听器 bump hitCount」与「UI 编辑」竞态互相覆盖；加载前过滤无效规则并备份 `rules.json.bak`，损坏文件保留为 `.corrupt.<ts>` |
| `NotificationHistoryStorage` | `notification_history.json` | 统一历史，**聚合模型**（`NotificationHistoryEntry`）；进程级缓存；旧格式自动迁移；按 `historyDays`（默认 5 天）裁剪；`deleteNotification` / `deleteNotificationsFromPackage` / `updateAppLabelForPackage` / `clearHistory` |
| `BlockedNotificationHistoryStorage` | `blocked_notification_history.json` | **仅一次性迁移**：启动时并入统一历史后清空；Service 不再写入 |
| `AppInfoStorage` | SQLite `app_info.db` | 包名 → 名称 + PNG 图标 BLOB（`CONFLICT_REPLACE`） |
| `UnmonitoredAppsStorage` | SharedPreferences `unmonitored_apps_prefs` | 未监控 App 集合（Gson 序列化 Set） |
| `StatsStorage` | SharedPreferences `stats` | 拦截总数 `blocked_count` + 按日通知量（400 天上限） |
| `NotificationActionRepository` | 内存 `ConcurrentHashMap` | PendingIntent 缓存（进程死亡即失） |
| `CrashLogManager` | `crash_logs.txt` | 崩溃日志（默认开启，写应用私有目录） |

> 存储一致性：JSON 存储类均**整文件替换写**（AtomicFile 保证原子性），配合单写者执行器与锁；本应用数据规模下足够（规则通常 <100、历史 <数千条）。

---

## 10. 支撑子系统

- **`TtsSpeaker`** — TTS 初始化/播报/停止，内置 5s 防抖；模板构建与清洗由 Service 的 `buildTtsText` 完成。
- **`RemoteViewsTextExtractor`** — 对双空通知用反射提取 `RemoteViews` 文本；**默认关闭**（隐私考虑）。
- **`NotificationColorEngine`** — 从 App 图标提取主色用于 UI 强调色；取不到时用包名 hash 兜底（v8.15.2 新增）。
- **`HealthCheckWorker`**（WorkManager 周期 6h）— 检查 `last_listener_connected_ms` 是否超 24h 且监听已授权；失效则弹高优通知引导修复（24h 节流）。
- **`setup/OemAutostart`** — 按厂商（小米/华为/OPPO/一加/vivo/三星等 10 家）跳转对应自启动设置页。
- **`setup/SetupState`** — 引导向导步骤状态（Welcome → Listener → PostNotif → Battery → OEM → Done）。
- **`RuleIds`** — 规则 id 规范化（缺失/冲突时重键；正常更新禁止改键，避免通知渠道孤儿化）。
- **`RuleMutations`** — 纯函数规则变更辅助（启用/禁用、改名、重置命中数等），保证更新不重键 id。
- **`RuleImport`** — v4 envelope 导入/导出（`@Keep` 字段、净化丢弃非法规则 `droppedCount`）。
- **`RuleWizardSupport`** — 向导用纯辅助：动作链摘要、已知 App 合并（历史+规则+AppInfo，**不读已安装列表**）。

---

## 11. UI 层

全 Compose + Material 3，无 XML 布局。底部三 Tab：**历史 / 规则 / 设置**（图标-only，长按气泡标签；竖屏底部导航，横屏左图右内容）。

### 屏面（`ui/screens/`）

| 屏面 | 职责 |
|---|---|
| `HistoryScreen` | 历史 Tab：聚合卡片（按时间/按应用/被规则处理 子 Tab）、图表面板、搜索、折叠分段、详情弹窗、从通知创建规则/触发动作 |
| `RulesScreen` | 规则 Tab：RuleCard 列表（动作链摘要、命中数、开关、重扫、重置命中数、长按删除） |
| `RuleWizardScreen` | 规则向导：来源 App（多选）→ 匹配模式/关键字 → 手机状态条件 → **顺序动作链**（拖拽排序、参数编辑） |
| `SettingsScreen` | 设置：保留天数、导入/导出、权限管理（监听/通知/电池优化）、崩溃日志、存储占用、恢复常驻通知、版本等 |
| `SetupWizardScreen` | 首次引导向导（分步授权） |
| `StorageUsageScreen` | 存储占用统计与清理 |

### 组件（`ui/components/`）

`NotixDialog` / `NotixConfirmDialog`（统一弹窗体系）、`NotificationCard` / `RuleCard` / `SettingRow` / `SectionHeader` / `EmptyState` / `SearchField` / `Chip` / `Buttons` / `RealAppIcon` / `HistoryNotificationDetailsDialog` / `NotificationDetailDialog` / `CrashLogDialog` / `DesignSystemPreview` 等。

### 主题（`ui/theme/`）

Material 3 自定义 Light/Dark 配色 + **语义令牌体系**：`NotixTheme` 通过 `CompositionLocal` 提供 `notixColors / notixType / notixSpacing / notixLayout / notixElevation`，页面统一经 `MaterialTheme.notix*` 取用，不直接判断 Light/Dark。动态取色（Material You）默认关闭（`dynamicColor = false`）。

---

## 12. 并发与一致性模型

- **单写者执行器**：历史写入、动作执行各用独立单线程执行器（Service 内），避免并发写文件竞态。
- **id-keyed 规则锁**：`RuleStorage` 对每条规则变更加锁，防止「监听器 bump hitCount」与「UI 编辑」相互覆盖（`RuleMutations` 保证不重键 id）。
- **多层防抖**：服务层 3s / TTS 5s / UI 广播 400ms，分别防重复处理、重复播报、重复读盘。
- **自包名守卫**：防止对自己重发的通知递归处理。
- **崩溃兜底**：通知处理顶层 try-catch + 崩溃日志，单条通知异常不影响进程。

---

## 13. 隐私与离线设计

- **零网络权限**：Manifest 无 `INTERNET`，应用不做任何 HTTP 请求，所有数据留在设备本地。
- **不枚举已装应用**：无 `<queries>`、无 `QUERY_ALL_PACKAGES`；App 信息在通知实际到达时按包名懒解析（`PackageManager` 按指定包）。
- **RemoteViews 反射提取默认关闭**：避免无谓读取通知内部结构。
- **规则来源 App 仅取历史中出现过的包**（`RuleWizardSupport.mergeKnownApps` 不读已安装列表）。
- 崩溃日志/通知历史均为应用私有目录，导出需用户显式操作（SAF）。

---

## 14. 测试

### 单元测试（`app/src/test/`，纯 JVM，无 Robolectric）

依赖 `testOptions.unitTests.isReturnDefaultValues = true`；Android 副作用均收敛到接口（`ActionFlowHost` / runner），测试注入 Fake。

| 测试类 | 覆盖 |
|---|---|
| `ActionFlowExecutorTest` | 串行执行、失败继续、at-most-once、取消、宿主销毁 |
| `ActionFlowModelTest` | `ActionSpec` / 参数模型 / `isValid` |
| `ActionFlowEditorTest` / `ActionFlowCopyBehaviorTest` / `DismissSpecTest` | COPY 模式、DISMISS 分派等行为 |
| `RuleImportExportRoundTripTest` | v4 导入导出往返 |
| `RuleWizardSupportTest` | 向导摘要/合并纯逻辑 |
| `ExampleUnitTest` | 冒烟 |

### 仪器化测试（`app/src/androidTest/`）

`BaseActionFlowTest` + `TestNotificationFactory` / `TestRuleFactory` / `TestPendingIntentReceiver` 支撑的 `ActionFlowBasicTest` / `ActionFlowClickFallbackTest` / `ActionFlowDebounceTest` / `ActionFlowDestroyTest` / `ActionFlowIntegrationTest` / `ActionFlowTtsConcurrencyTest`，以及 `RulesScreenFlowTest` / `RulesScreenFlow4BTest` / `RulesScreenFlowSaveValidationTest` / `RulesScreenFlowWarningTest`（Compose UI 流程）等。

---

## 15. Android Manifest 与权限

**权限**（`AndroidManifest.xml`）：

```xml
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

无网络、存储、相机、定位权限。

**组件**：
- `MainActivity` — `MAIN`/`LAUNCHER`，exported。
- `NotificationBlockerService` — `BIND_NOTIFICATION_LISTENER_SERVICE` 权限、`NotificationListenerService` intent-filter、`foregroundServiceType="specialUse"` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`。
- `FileProvider` — 崩溃日志等文件分享（`file_paths.xml`）。

**备份**：`fullBackupContent="@xml/backup_rules"`、`dataExtractionRules="@xml/data_extraction_rules"` 控制自动备份范围。

---

## 16. 已知占位与限制

- **`STRONG_REMIND` / `POSTPONE` 两个动作仅执行占位**：可保存、会命中，但 `ActionFlowExecutor` 记日志后跳过，运行时无副作用（代码标注「v8.11+ 接入」）。
- **`repostNotification()` 与 `RULE_REPOST_CHANNEL_ID` 为旧 SILENT 模型残留**：函数定义于 Service（约 645 行）但全库无调用点，属死代码；渠道仍用于自包名/重发守卫。
- **`ADVANCED` 匹配模式仅 UI 展示**，`matchesCondition` 恒返回 false。
- **`RemoteViewsTextExtractor` 默认关闭**，作为实验性能力存在。
- **`BlockedNotificationHistoryStorage` 仅迁移用**：启动并入统一历史后清空，Service 不再写入。
- 本仓库无预置规则、无 fastlane 元数据；`.github` 仅 `release.yml` 一个 CI 工作流（凭据全部走 GitHub Secrets）。
