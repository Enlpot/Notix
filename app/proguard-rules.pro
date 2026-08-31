# ============ Gson 序列化保护（release R8 混淆）============
# 背景：release 开启 R8 混淆。toParamsJson()/asParams()/规则存储均用 Gson 反射，
# 按字段名输出 JSON key。若字段被混淆（如 ClickButtonParams.buttonLabel -> a），
# 读取端硬编码 get("buttonLabel") 查不到 → 动作参数/规则字段丢失（release 专有 bug，debug 无）。
# 修复：保持所有参与 Gson 序列化的 data class 字段名 + 枚举常量名。

# 规则与动作规格（RuleStorage 整规则 JSON 存储 + RuleImport 导入导出）
-keepclassmembers class com.enlpot.notix.BlockerRule { <fields>; }
-keepclassmembers class com.enlpot.notix.ActionSpec { <fields>; }
-keepclassmembers class com.enlpot.notix.RuleCondition { <fields>; }
-keepclassmembers class com.enlpot.notix.ExtraCondition { <fields>; }
-keepclassmembers class com.enlpot.notix.TimeCondition { <fields>; }
-keepclassmembers class com.enlpot.notix.SourceApp { <fields>; }

# 动作参数 data class（toParamsJson/asParams 反射，字段名即 JSON key）
-keepclassmembers class com.enlpot.notix.ClickButtonParams { <fields>; }
-keepclassmembers class com.enlpot.notix.CopyParams { <fields>; }
-keepclassmembers class com.enlpot.notix.DelayParams { <fields>; }
-keepclassmembers class com.enlpot.notix.TtsParams { <fields>; }
-keepclassmembers class com.enlpot.notix.DismissParams { <fields>; }
-keepclassmembers class com.enlpot.notix.StrongRemindParams { <fields>; }
-keepclassmembers class com.enlpot.notix.PostponeParams { <fields>; }

# 枚举：Gson 序列化用 name()、反序列化用 valueOf()，必须保留枚举常量与 valueOf
-keep enum com.enlpot.notix.RuleAction { *; }
-keep enum com.enlpot.notix.CopyMode { *; }
-keep enum com.enlpot.notix.MatchMode { *; }
-keep enum com.enlpot.notix.ScreenState { *; }
-keep enum com.enlpot.notix.ChargingState { *; }
-keep enum com.enlpot.notix.DndState { *; }
-keep enum com.enlpot.notix.BluetoothState { *; }
