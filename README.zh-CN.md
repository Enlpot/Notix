---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 26c411d2a0f0a2e307f470b439631140_7ce604e29baf11f19bec525400826444
    ReservedCode1: xl3YQwpt1qxXCkkM3Da4iWSSYcvEKtN2+oKcbREJlhLg8CbyuTRQGwjy9wCl+SWt4g/+OuUopxU958ZkFvR5aZqSVvOoQg1EuiHE4zYJ3TJWki6EnzlpR1Ru3muRDVdPs20LFnNe7/caPVp+cYog1wMy52uH7nZjh6TqkSAhnggPfYQOTTVCHdeaZTA=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 26c411d2a0f0a2e307f470b439631140_7ce604e29baf11f19bec525400826444
    ReservedCode2: xl3YQwpt1qxXCkkM3Da4iWSSYcvEKtN2+oKcbREJlhLg8CbyuTRQGwjy9wCl+SWt4g/+OuUopxU958ZkFvR5aZqSVvOoQg1EuiHE4zYJ3TJWki6EnzlpR1Ru3muRDVdPs20LFnNe7/caPVp+cYog1wMy52uH7nZjh6TqkSAhnggPfYQOTTVCHdeaZTA=
---

[English](README.md) | **简体中文**

# Notix

Notix 是一款完全离线的 Android 应用，通过可自定义的规则拦截通知并自动执行相应动作——拦截、静默、朗读、复制、打开，甚至点击通知上的操作按钮。

> **衍生于 Anuj 的 [DoNotNotify](https://github.com/anujja/DoNotNotify)**（MIT License）。Notix 是经过品牌重塑与重新打包的分支，拥有独立的发布渠道、包名和签名密钥。

## 功能特性

- **实时通知处理** — 基于 Android 的 NotificationListenerService，配合前台保活服务与周期性健康检查，即使系统尝试回收后台进程，规则也能持续生效
- **Action Flow 规则** — 每条规则执行严格有序的动作链：**消除（Dismiss）**、**静默（Silent，低打扰频道重发）**、**打开（Open）**、**点击按钮（Click Button）**、**复制（Copy，标题/正文/标题+正文）**、**TTS 朗读** 与 **延迟（Delay）**
- **灵活匹配** — 规则可指定一个或多个来源 App，关键字匹配支持：包含任一、包含全部、不包含任一、不包含全部，以及"包含 A 且不包含 B"的混合模式
- **情境条件** — 可按屏幕状态（亮/灭）、充电状态（有线/无线/电池）、勿扰模式、蓝牙耳机连接（可选指定设备名）以及时间段+星期多选来限定规则生效范围
- **可视化规则向导** — 分步向导（应用 → 匹配 → 条件 → 动作）让复杂规则的创建变得简单；也可在历史中点击任意通知直接由此创建规则
- **通知历史** — 以**按时间 / 按应用 / 已过滤**三个子标签浏览所有收到的通知，并配有按日筛选的统计图表面板；保留时长与清理由用户完全掌控
- **已处理追踪** — 查看哪些通知被处理、由哪条规则处理，规则附带命中次数统计
- **通知还原** — 从历史中将被处理的通知重新发布到通知栏
- **TTS 朗读** — 通过懒加载 TTS 引擎朗读通知（优先简体中文，不可用时回退系统默认语言），模板支持 `{app}` / `{title}` / `{text}` 占位符
- **导入/导出** — 将规则以带版本号的 JSON 文件备份与恢复，并兼容旧版导出格式
- **内置崩溃日志** — 设备端崩溃日志查看器，便于排查问题
- **设置向导** — 首次启动引导授予通知监听权限、忽略电池优化，并针对小米、华为、OPPO、一加、vivo、三星等厂商引导开启自启动
- **纯黑深色主题** — 遵循 Material Design 3 深色规范的 OLED 友好纯黑主题；紧凑的 56dp 纯图标底部导航（长按显示标签）与统一的横屏布局（左侧图表面板 + 右侧内容）带来现代 UI 体验
- **完全离线** — 无网络权限，不收集任何数据，一切都在设备本地完成

## 运行要求

- Android 7.0（API 24）及以上
- 通知监听器权限（Android 13+ 还需通知发送权限以支撑拦截/静默流程）

## 截图

*截图将在此处添加。*

## 构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 安装 Debug 构建到已连接的设备
./gradlew installDebug
```

Release APK 使用 R8 混淆并以 Notix 发布密钥库签名。签名凭据从 `local.properties`（键名 `KEYSTORE_NOTIX_*`）或环境变量读取，**绝不**提交到仓库。

项目使用 Gradle 8.13、Android Gradle Plugin 8.13.2、Kotlin 2.0.21，`compileSdk 36` / `minSdk 24`，Java 11 兼容。

## 使用说明

1. 安装并启动应用——设置向导会引导你授予通知监听权限
2. （推荐）将 Notix 加入电池优化豁免，并在 OEM 设备上开启自启动，以保证监听服务持续运行
3. 在 **History** 标签页查看收到的通知，图表面板提供按日的概览
4. 点击某条通知即可由此创建规则；或前往 **Rules** 标签页通过可视化向导自定义规则（应用 → 匹配 → 条件 → 动作）
5. 被规则处理的通知会出现在 **Blocked** 标签页——点击任意条目可查看详情、编辑处理它的规则或还原通知
6. 在 **Settings** 中可导入/导出规则、重置命中计数、清除历史或查看崩溃日志

## 文档

面向开发者与贡献者的详细文档位于 [`docs/`](docs/) 目录：

- **[架构与代码库概览](docs/ARCHITECTURE.md)** — 项目结构、数据模型、核心服务、存储层、UI 层、数据流图、导航图与类依赖关系
- **[API 参考](docs/API_REFERENCE.md)** — 类与 Composable 的方法级参考文档
- **[开发者指南](docs/DEVELOPER_GUIDE.md)** — 关于新增功能、页面、规则、存储及运行测试的实战指南

另见 [CONTRIBUTING.md](CONTRIBUTING.md)（贡献指南）与 [RELEASE_NOTES.md](RELEASE_NOTES.md)（版本历史）。

## License

MIT License — 详见 [LICENSE](LICENSE)。

本项目基于 Anuj 的 [DoNotNotify](https://github.com/anujja/DoNotNotify)（© 2025）衍生而来，并包含 Notix 自身的修改（© 2026）。依据 MIT License 要求，两份版权声明均完整保留在 LICENSE 文件中。
*（内容由AI生成，仅供参考）*
