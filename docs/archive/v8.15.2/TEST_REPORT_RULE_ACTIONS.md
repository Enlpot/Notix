# Notix 规则动作完整测试报告

- **测试日期**：2026-09-01
- **被测版本**：8.49.0（versionCode 186，含增强搜索）
- **测试环境**：Android 模拟器 emulator-5554（API 36），调试包 `app-debug.apk`
- **测试方式**：以 NotixTestNotifier（6 个模拟通知 app）发送真实第三方包名通知；规则通过直接写入 `rules.json` 精确构造（覆盖各动作与动作流组合）；以 Service 执行日志（`ActionFlow` / `Action #N`）、通知栏状态、剪贴板、TTS 引擎日志、数据库落库为验证依据。**仅测试，未修改任何 app 代码，未提交未推送。**

---

## 一、功能测试结果

### 1. 单动作测试

| # | 动作 | 测试场景 | 结果 | 证据 |
|---|------|---------|------|------|
| 1 | **DISMISS 移除通知** | 微信通知到达即移除 | ✅ 通过 | `Action #0 DISMISS success` → `SUCCESS executed=1`；通知栏该通知被移除 |
| 2 | **COPY 复制全部**（标题+正文） | 到达即复制 | ✅ 通过 | `COPY success` → `SUCCESS`；剪贴板写入无异常 |
| 3 | **COPY 复制标题** | 仅复制标题 | ✅ 通过 | `COPY success` → `SUCCESS`（mode=TITLE） |
| 4 | **COPY 复制正文** | 仅复制正文 | ✅ 通过 | `COPY success` → `SUCCESS`（mode=TEXT） |
| 5 | **CLICK_BUTTON 点击按钮** | 测试通知无 action 按钮 | ⚠️ 部分通过 | 失败路径正确：`CLICK_BUTTON failed: no notification actions` → `PARTIAL_FAILURE` 且 Flow 不中断；**成功路径无法在模拟器验证**（测试 app 通知不带按钮） |
| 6 | **OPEN_NOTIFICATION 打开通知** | 测试通知无 contentIntent | ⚠️ 部分通过 | 失败路径正确：`OPEN_NOTIFICATION failed: no contentIntent` → `PARTIAL_FAILURE`；**成功路径无法验证** |
| 7 | **TTS 播报** | 到达即朗读 | ✅ 通过 | 异步等待播报完成：`TTS start` →（约 8s，Google TTS 引擎合成中文）→ `TTS success` → `SUCCESS`。引擎日志确认 `Synthesis request for locale zho-CHN` |
| 8 | **DELAY 等待** | 等待 2 秒 | ✅ 通过 | `DELAY start` →（2.00s）→ `DELAY success`，时序准确 |
| 9 | **STRONG_REMIND 强提醒** | — | ⚠️ 未实现 | `STRONG_REMIND skipped (execution TODO)`：动作被记录为 success，但**实际无任何强提醒效果** |
| 10 | **POSTPONE 延迟重发** | — | ⚠️ 未实现 | `POSTPONE skipped (execution TODO)`：同上，**实际无效果** |

### 2. 动作流顺序测试

| 场景 | 动作链 | 结果 | 证据 |
|------|--------|------|------|
| 失败继续 | CLICK_BUTTON(不存在) → COPY | ✅ 通过 | 失败后继续执行后续：`#0 CLICK_BUTTON failed` → `#1 COPY success` → `PARTIAL_FAILURE executed=2 failures=1` |
| 等待时序 | DELAY(2s) → COPY | ✅ 通过 | `#0 DELAY start` →（2s）→ `#0 success` → `#1 COPY success` → `SUCCESS executed=2` |
| 混合链 | DISMISS → CLICK_BUTTON → COPY | ✅ 通过 | 顺序严格：`#0 DISMISS success` → `#1 CLICK_BUTTON failed` → `#2 COPY success` → `PARTIAL_FAILURE executed=3` |
| 完整链 | DISMISS → COPY → DELAY(1s) → TTS | ✅ 通过 | 顺序正确、异步 DELAY 等待后推进；仅 TTS 因「无 params」失败（见 Bug-2），其余全部成功 |

### 3. 附带验证

- ✅ **hitCount 命中计数**：发 3 条命中通知后 `rules.json` 内 `hitCount=3`，计数正确累加并落盘。
- ✅ **多通知并发 Flow 隔离**：同规则多条通知到达，各自 Flow 独立执行，无状态串扰（日志中每个 key 独立 start/complete）。

---

## 二、发现的 Bug

### 🟠 Bug-1（已复测修正，中低）：规则文件含显式 `"params": null` 时整文件判坏、全部规则连坐丢失
- **⚠️ 2026-09-02 复测结论**：UI 正常创建「移除通知」规则 → 重启 app，**规则完好无损，不丢失**（实测 `rules.json` 内容无变化、无 corrupt、无报错）。**该 bug 在正常用户路径下不成立，此前按严重级误报，现修正。**
- **为何 UI 路径安全**：`RuleWizardSupport.dismissSpec(false)` 返回的 `ActionSpec(DISMISS, params=null)`，经默认 `Gson()` 序列化时**自动跳过 null 字段**（`serializeNulls=false`），落盘即为干净的 `{"type":"DISMISS"}`，加载正常。
- **真实隐患（仍存在）**：若 `rules.json` 被人为/第三方/导入流程显式写入 `"params": null`（实测手工构造该格式重启必触发 `JsonSyntaxException: Expected a JsonObject but was JsonNull; at path $.actions.params`），`RuleStorage.loadLocked` 走 `preserveCorruptFile` 分支，**整个文件所有规则连坐清空**（仅留 `.corrupt` 备份）。属解析健壮性缺陷，而非 UI 触发路径。
- **修复建议**（健壮性兜底，非紧急）：
  1. `loadLocked` 改为逐条 try-parse：跳过坏的那一条、保留其余规则，不再整文件判坏全丢；
  2. 或为 `ActionSpec.params` 提供容错 TypeAdapter（显式 `null` → 解析为 null），与「字段缺失 → null」语义统一。

### 🟠 Bug-2（已修复 ✅）：TTS 动作无 `params` 时报「params missing」失败
- **现象**：`{"type":"TTS"}`（无 params 字段）→ 执行报 `TTS failed: params missing` → `PARTIAL_FAILURE`。
- **根因**：`ActionFlowExecutor` 对 TTS 分支强制 `if (spec.params == null) → params missing`；但 `TtsParams.template` 本为可选（null = 默认模板），`RealAsyncRunner.runTts` 在 params 为 null 时也能正常用默认模板。
- **修复（2026-09-02 已实施）**：删除 TTS 分支多余的 `params == null` 判断，直接走 `asyncRunner.runTts(...)`。
- **复测结果**：`{"type":"TTS"}` 无 params 规则 → `Action #0 TTS start` →（约 9s，默认模板播报）→ `success` → `SUCCESS`。✅

### 🟠 Bug-3（已处理：改为显式失败，功能仍未实现 ⏳）：STRONG_REMIND / POSTPONE 动作静默无效
- **现象**：UI 可配置「强提醒」「延迟重发」动作，但执行层 `skipped (execution TODO)`，且**记录为 success**（`ActionFlow complete status=SUCCESS`），无任何实际效果、无任何用户提示。
- **修复（2026-09-02 已实施第一阶段）**：执行层不再静默跳过，改为**显式失败** `ActionFailure(..., "not implemented")`，Flow 结果 `status=PARTIAL_FAILURE`，让「未生效」可见可查。
- **复测结果**：STRONG_REMIND / POSTPONE（带 delayMs）→ `Action #0 xxx failed: not implemented` → `PARTIAL_FAILURE failures=1`。✅
- **注意（测试补充发现）**：POSTPONE 的 `ActionSpec.isValid` 要求 `params.delayMs` 存在且 >0，**无 delayMs 的 POSTPONE 规则会被 RuleStorage 加载时判定 invalid 丢弃**（`Dropped 1 invalid rule(s) on load`）。UI 路径总会带 delayMs，不受影响；属合理校验。
- **遗留**：真实功能（强提醒 heads-up+响铃+震动、延迟重投递）仍未实现，待后续开发。

### 🟡 Bug-4（低）：CLICK_BUTTON / OPEN_NOTIFICATION 成功路径缺少可验证载体
- **现象**：测试 app（NotixTestNotifier）通知**不带 action 按钮与 contentIntent**，两个动作只能验证失败路径（找不到按钮/无 Intent → PARTIAL_FAILURE 且不崩溃），成功路径未覆盖。
- **说明**：非代码缺陷，属测试覆盖缺口。`RealSyncActionRunner.clickButton`/`openNotification` 的实现逻辑（精确匹配→包含匹配→send PendingIntent）代码审查无误。
- **建议**：后续给 NotixTestNotifier 的某个 app 增加带 action 按钮的通知（如「回复」按钮）与 contentIntent，补齐成功路径回归。

---

## 三、功能优化建议

1. **规则 JSON 校验/备份可视化**：当前规则文件「一损俱损」，建议设置页提供「规则备份/导出/导入」与「损坏规则恢复」入口，corrupt 备份可一键找回。
2. **动作执行结果可见化**：`FlowResult`（SUCCESS/PARTIAL_FAILURE/失败原因）目前只写日志；建议在通知历史/规则卡片上展示「上次执行结果」或异常标记，便于用户感知规则是否真正生效。
3. **TTS 播报体验**：实测 TTS 合成耗时约 8s（首次加载模型），建议播报前不阻塞后续动作的关键路径可接受，但连续多条通知播报存在叠加/延迟，可评估「队列合并 + 去重」策略。
4. **DISMISS + 其他动作组合**：DISMISS 移除通知后，后续 CLICK_BUTTON/OPEN_NOTIFICATION 仍基于内存中的 PendingIntent 快照执行（实测 Flow 正常推进），符合预期；建议在文档中明确该语义，避免用户误解「移除=无法点击」。
5. **动作流超时保护**：DELAY/TTS 为异步挂起，若 TTS 引擎异常导致 onDone 永不回调，Flow 会一直挂起（当前无超时兜底）；建议增加异步动作超时（如 15s）强制推进，防「卡死」动作链。

---

## 四、测试结论

- 核心动作执行链路**健壮**：8 种动作中 6 种（DISMISS/COPY/TTS/DELAY/CLICK_BUTTON/OPEN_NOTIFICATION）执行与失败处理符合设计；动作流严格串行、失败继续、异步等待时序全部正确；hitCount 计数正常。
- **经 2026-09-02 真实 UI 复测**：Bug-1 在正常用户路径下**不成立**（DISMISS 规则重启不丢失），已修正为健壮性隐患（中低优先级）。
- **2026-09-02 修复进展**：Bug-2 已修复（TTS 无 params 正常播报）；Bug-3 第一阶段已处理（强提醒/延迟重发改为显式失败，不再静默假成功，真实功能待实现）；Bug-1 的逐条隔离解析兜底建议择机做；Bug-4 为测试覆盖项。
- **当前优先级**：Bug-3 真实功能实现（强提醒/延迟重发）为可选开发项；Bug-1 逐条隔离解析兜底建议择机做；Bug-4 补齐测试覆盖。
