# Notix — 开发指南

面向 Notix 代码库开发者的实用指南，对应 **v8.15.2**。

## 目录

- [开始](#开始)
- [构建与运行](#构建与运行)
- [项目结构](#项目结构)
- [如何新增功能](#如何新增功能)
- [理解规则系统](#理解规则系统)
- [理解动作流](#理解动作流)
- [如何新增存储机制](#如何新增存储机制)
- [如何新增屏面](#如何新增屏面)
- [如何新增弹窗](#如何新增弹窗)
- [关键设计决策](#关键设计决策)
- [常见模式](#常见模式)
- [测试](#测试)
- [发布流程](#发布流程)

---

## 开始

### 前置条件

- Android Studio（最新稳定版）
- JDK 11+
- 安装 API 36 的 Android SDK
- 运行 API 24+ 的 Android 设备或模拟器

### 项目设置

1. 克隆仓库。
2. 用 Android Studio 打开（Gradle 自动同步）。
3. 构建：`./gradlew assembleDebug`
4. 安装到设备：`./gradlew installDebug`

### 签名（仅 release）

release 签名配置从 `local.properties` 或环境变量读取 `KEYSTORE_NOTIX_FILE / KEYSTORE_NOTIX_PASSWORD / KEYSTORE_NOTIX_ALIAS / KEYSTORE_NOTIX_KEYPASSWORD`。debug 构建无需密钥库。**切勿提交密钥库或其密码**（`.gitignore` 已排除 `*.jks`、`*.keystore`、`signing.properties`、`keystore.properties`、`local.properties`）。

### 重要：通知使用权

Notix 是 `NotificationListenerService`，该权限只能通过系统设置授予。首次运行的引导向导会引导用户完成。开发时：**设置 > 应用 > 特殊应用权限 > 通知使用权 > Notix**。

---

## 构建与运行

```bash
# Debug APK
./gradlew assembleDebug

# Release APK（R8 混淆；需要签名配置）
./gradlew assembleRelease

# 安装到已连接设备
./gradlew installDebug

# 运行单元测试
./gradlew testDebugUnitTest

# 运行仪器化测试（需要设备/模拟器）
./gradlew connectedAndroidTest

# 清理构建产物
./gradlew clean
```

单元测试跑在纯 JVM（无 Robolectric）；`testOptions.unitTests.isReturnDefaultValues = true` 会 stub `android.util.Log`。

---

## 项目结构

```
app/src/main/java/com/enlpot/notix/
├── NotixApp.kt                  # 应用入口
├── MainActivity.kt              # UI 根与状态
├── NotificationBlockerService.kt# 监听引擎
├── BlockerRule.kt               # 规则 + 动作模型
├── RuleMatcher.kt               # 纯决策引擎
├── ActionFlowExecutor.kt        # 动作链执行器 + 执行体
├── RuleStorage.kt / RuleIds.kt / RuleMutations.kt / RuleImport.kt / RuleWizardSupport.kt
├── NotificationHistoryStorage.kt / BlockedNotificationHistoryStorage.kt / NotificationHistoryEntry.kt / SimpleNotification.kt
├── StatsStorage.kt / AppInfoStorage.kt / UnmonitoredAppsStorage.kt / NotificationActionRepository.kt
├── TtsSpeaker.kt / RemoteViewsTextExtractor.kt / NotificationColorEngine.kt / CrashLogManager.kt / ExternalLinks.kt
├── health/HealthCheckWorker.kt
├── setup/OemAutostart.kt / SetupState.kt
└── ui/
    ├── components/              # 可复用组件
    ├── screens/                 # History / Rules / RuleWizard / Settings / SetupWizard / StorageUsage
    └── theme/                   # 主题 + 语义令牌
```

---

## 如何新增功能

### 示例：给动作链新增一个动作

动作链是核心扩展点。新增动作需改动以下文件：

1. **`BlockerRule.kt`** — 在 `enum RuleAction` 中增加值；新增参数数据类（如 `MyActionParams`）；如需在 `ActionSpec.isValid` 中加校验。
2. **`ActionFlowExecutor.kt`** — 在 `advance()` 的 `when` 中新增分支：调用执行体后 `completeAction(...)`；若为异步动作，必须等其完成回调后再推进（参照 TTS/DELAY）。Android 副作用一律经 `ActionFlowHost` 与 `SyncActionRunner` / `AsyncActionRunner` 走。
3. **`RuleWizardSupport.kt`** — 新增 `hasActionParams(type)`、`defaultParamsFor(type)`、`*Spec(...)` 构造器，以及 `actionFlowSummary` 的对应分支。
4. **`ui/screens/RuleWizardScreen.kt`** — 为新动作增加 label/图标/描述与 `ActionParamEditor` UI。
5. **测试** — 引擎逻辑在 `ActionFlowExecutorTest`；行为测试新建类。

### 文件修改顺序（通用）

1. 数据模型（`BlockerRule.kt`、`SimpleNotification.kt`）
2. 纯逻辑（`RuleMatcher.kt`、`RuleMutations.kt`、`RuleWizardSupport.kt`）
3. 存储（如需）
4. 引擎（`NotificationBlockerService.kt`、`ActionFlowExecutor.kt`）
5. UI 组件（`ui/components/`）
6. UI 屏面（`ui/screens/`）
7. Activity 接线（`MainActivity.kt`）
8. 测试

> 把 Android 副作用收口到接口（`ActionFlowHost`、`*ActionRunner`）后面，逻辑才能保持 JVM 可测。

---

## 理解规则系统

### 规则结构

```
sourcePackages（≥1 个 App）
  → condition:     关键字匹配（MatchMode + include/exclude 关键字）
  → extraCondition:手机状态（亮灭屏 / 充电 / 勿扰 / 蓝牙 / 时间段）
  → actions:       顺序动作链
```

### 决策流程（`RuleMatcher`）

1. 规则必须 `isValid && isEnabled`。
2. 来源 App 过滤：通知包名必须在 `sourcePackages` 中。
3. 关键字匹配（`matchesCondition`）：按 `MatchMode`，对标题或正文忽略大小写匹配。**无条件 = 全部命中。**
4. 额外条件（`matchesExtra`）：所有已配置的手机状态检查必须通过；时间段支持跨天；星期为空 = 每天。
5. **首个通过全部检查的规则胜出**（`planNotificationDecision` → `RuleDecision.Apply`）；否则 `Pass`。

### 命中计数

每条命中的通知都会经 `RuleStorage.incrementHitCounts` 累加对应规则的 `hitCount`。计数显示在规则卡片上；可在规则页重置。

### 规则 id 与身份

- 每条规则有稳定 `id`（`RuleIds`）；更新时禁止重键（`RuleMutations`），因为规则 id 拥有其通知渠道。
- 导入/旧规则在加载时规范化并净化；无效规则（无来源 App 或动作链为空）会被过滤。

---

## 理解动作流

- `rule.actions` 是有序的 `List<ActionSpec>`；`actions[0]` 最先执行。
- 执行**严格串行**：每个动作完成（成功或失败）后进入下一个；失败被记录（`ActionFailure`）且流程继续。
- 异步动作（TTS、DELAY）只在完成回调后推进；重复回调被忽略（at-most-once）。
- 宿主（Service）销毁时流程取消（`hostAlive`）。
- **占位**：`STRONG_REMIND` 与 `POSTPONE` 可保存但当前为空操作（日志 `skipped (execution TODO)`）。

新增动作：见 [如何新增功能](#如何新增功能)。

---

## 如何新增存储机制

应用使用多种存储模式；按需选择：

### JSON 文件（结构化列表）— `RuleStorage` / `NotificationHistoryStorage`

用 `AtomicFile` + 进程级缓存 + 锁保证读改写安全：

```kotlin
class MyStorage(context: Context) {
    private val gson = Gson()
    private val file = File(context.filesDir, "my_data.json")
    private val atomicFile = AtomicFile(file)

    fun getData(): List<MyData> {
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<List<MyData>>() {}.type
        return gson.fromJson(atomicFile.readFully().toString(Charsets.UTF_8), type) ?: emptyList()
    }

    fun saveData(data: List<MyData>) {
        val stream = atomicFile.startWrite()
        try {
            stream.write(gson.toJson(data).toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            atomicFile.failWrite(stream)
            throw e
        }
    }
}
```

> 若数据可能被多线程写入（如监听器执行器与 UI），仿照 `RuleStorage` 加共享锁与缓存。

### SharedPreferences（键值）— `StatsStorage` / `UnmonitoredAppsStorage`

基础类型仿照 `StatsStorage`（读改写加锁）；Gson 序列化集合仿照 `UnmonitoredAppsStorage`（带缓存）。

### SQLite（可查询 / 较大数据）— `AppInfoStorage`

仿照 `AppInfoStorage` / `AppInfoDatabaseHelper`（`SQLiteOpenHelper`）。

### 内存（临时）— `NotificationActionRepository`

单例 `object` + `ConcurrentHashMap`。

### 集成

创建存储类后，在需要处实例化（写路径归 Service，读路径归 Activity/屏面）。遵守**单写者**原则：并发写者必须共享同一把锁。

---

## 如何新增屏面

1. **在 `ui/screens/` 创建屏面 composable**。
2. **在 `MainActivity` 增加导航状态**：
   ```kotlin
   private var showMyScreen by mutableStateOf(false)
   ```
3. **在根 composable 用状态判断 + `BackHandler` 接线**。应用使用状态式导航（无 Jetpack Navigation）：
   ```kotlin
   if (showMyScreen) {
       BackHandler { showMyScreen = false }
       MyScreen(onClose = { showMyScreen = false }, ...)
   }
   ```

文案从 `strings.xml` 读取（有 `values`、`values-zh-rCN`、`values-es`、`values-fr`、`values-ja`、`values-ko`、`values-pl`、`values-ru`）；用语义令牌（`MaterialTheme.notix*`）而非硬编码颜色/间距。

---

## 如何新增弹窗

1. **在 `ui/components/` 创建弹窗 composable**，尽量继承统一体系 `NotixDialog` / `NotixConfirmDialog`。
2. **在所属 composable 增加触发状态**：
   ```kotlin
   var itemToShow by remember { mutableStateOf<MyData?>(null) }
   ```
3. **条件展示**（可空状态：非空显示，null 关闭）：
   ```kotlin
   itemToShow?.let { item -> MyDialog(data = item, onDismiss = { itemToShow = null }, ...) }
   ```

---

## 关键设计决策

### 无架构框架
直接用 Compose `mutableStateOf` 管理状态，状态归 `MainActivity`，作为参数下传（无 ViewModel/LiveData/StateFlow）。此规模下合适；复杂度增长后再重构。

### 单模块
一切都在 `:app`。此规模避免构建复杂度。

### 无网络
零网络权限；所有数据留在设备。不要未经充分理由与隐私评审引入联网。

### Gson 而非 Kotlin 序列化
数据类用 `@Keep` 保 R8（Gson 反射）。任何被 Gson 接触的类都需 `@Keep`。

### 整表替换 + AtomicFile
JSON 存储整体原子替换；配合单写者执行器与锁，对本数据规模正确。

### 布尔导航
状态式导航代替 Jetpack Navigation — 依赖最少，无深链。

### 引擎 / Android 副作用接缝
决策引擎（`RuleMatcher`）与动作引擎（`ActionFlowExecutor`）只依赖接口（`ActionFlowHost`、`*ActionRunner`）；Service 提供真实实现。保持此结构以维持 JVM 可测。

---

## 常见模式

### 在 composable 中访问存储
用 `remember { Storage(context) }` 实例化；用 `produceState` + `Dispatchers.IO` 异步加载（如 `AppInfoStorage` 的 App 图标）。

### App 图标取色
用 `NotificationColorEngine.getNotificationColors(context, packageName)` 取强调色（图标不可解析时回退到 hash 色）。

### 防抖
- Service：每 `sbn.key` 3 秒 Action-Flow 防抖、3 秒 `recentlyBlocked` 窗口。
- TTS：`TtsSpeaker` 内 5 秒防抖。
- UI 广播刷新：`MainActivity` 400ms 防抖。

### 规则变更安全
不要绕过 `RuleStorage` 的 id-keyed 方法（`incrementHitCounts`、`updateRuleById`、`deleteRuleById` 等）直接改规则列表。它们在锁内重新读取，不会复活已删除的规则。

### 用 Snackbar 代替 Toast
使用应用内 `SnackbarHostState`（见 `RuleWizardScreen`）而非系统 Toast。

---

## 测试

### 单元测试（`app/src/test/`）

纯 JVM，无 Robolectric。运行：`./gradlew testDebugUnitTest`。

- `ActionFlowExecutorTest` / `ActionFlowModelTest` / `ActionFlowCopyBehaviorTest` / `DismissSpecTest` / `ActionFlowEditorTest` — 注入 Fake 的引擎行为。
- `RuleImportExportRoundTripTest` — v4 导入导出往返。
- `RuleWizardSupportTest` — 向导纯辅助函数。

为 `ActionFlowHost` / `SyncActionRunner` / `AsyncActionRunner` 注入 Fake，断言 `FlowResult` / `FlowExecution`。示例：

```kotlin
@Test
fun `dismiss 失败后流程继续`() {
    val host = FakeHost(failDismiss = true)
    val exec = ActionFlowExecutor(RealSyncActionRunner(host), RealAsyncRunner(host))
    val flow = exec.execute(listOf(dismissSpec(), copySpec(CopyMode.TITLE)), ctx)
    assertEquals(FlowStatus.PARTIAL_FAILURE, flow.result?.status)
}
```

### 仪器化测试（`app/src/androidTest/`）

运行：`./gradlew connectedAndroidTest`。用 `TestNotificationFactory` / `TestRuleFactory` / `TestPendingIntentReceiver` 与 `BaseActionFlowTest` 驱动真实通知；`RulesScreenFlow*` 系列覆盖 Compose UI 流程。

---

## 发布流程

### 版本号更新

在 `app/build.gradle.kts`：

```kotlin
defaultConfig {
    versionCode = 132          // 每次发布递增
    versionName = "8.16.0"     // 可读版本号
}
```

同步更新 `RELEASE_NOTES.md`、`VERSION_HISTORY.md` / `VERSION_HISTORY.zh-CN.md`、`CHANGELOG.md`。

### CI 自动发布

`.github/workflows/release.yml` 在推送到 `main`（及手动触发）时运行：
1. 读取 `versionName`；若 `v<version>` GitHub Release 已存在则跳过。
2. 从 `secrets.NOTIX_KEYSTORE_BASE64` 解码密钥库（外加 `NOTIX_KEYSTORE_PASSWORD`、`KEYSTORE_NOTIX_KEYPASSWORD`、`KEYSTORE_NOTIX_ALIAS`）并执行 `assembleRelease`。
3. 将 APK 作为 GitHub Release 发布，附 `RELEASE_NOTES.md`。

> **重要：** 保持 GitHub Secrets 与本地密钥库一致。发布密钥库是所有已发布 APK 的签名身份——妥善保管密钥库与密码，切勿提交。
