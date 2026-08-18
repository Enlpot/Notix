# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep all Gson-serialized model classes and enums with their original field names.
# R8 would otherwise rename fields (action -> g, etc.) and break Gson's reflective
# serialization, producing rules.json without the action field -> rules dropped on load
# ("rules disappear after app update" bug). Field names must stay stable across versions.
-keep class com.enlpot.notix.BlockerRule { *; }
-keep class com.enlpot.notix.SourceApp { *; }
-keep class com.enlpot.notix.RuleCondition { *; }
-keep class com.enlpot.notix.ExtraCondition { *; }
-keep class com.enlpot.notix.TimeCondition { *; }
-keep class com.enlpot.notix.ActionParams { *; }
-keep enum com.enlpot.notix.RuleAction { *; }
-keep enum com.enlpot.notix.MatchMode { *; }
-keep enum com.enlpot.notix.ScreenState { *; }
-keep enum com.enlpot.notix.ChargingState { *; }
-keep enum com.enlpot.notix.DndState { *; }
-keep enum com.enlpot.notix.BluetoothState { *; }
