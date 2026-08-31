[English](README.md) | **简体中文**

# Notix

> 把通知的控制权拿回来。

Notix 是一款**完全离线**的 Android 通知管理工具，面向重度手机用户。它挂载在系统的通知监听器上，捕获所有进来的通知，并让你用精确、自动化的规则去处理——拦截噪音、静默次要通知、朗读重要内容、复制验证码，甚至直接打开通知并点击其中的按钮。

无账号。无云端。无网络。一切都在你的设备上完成。

> **分支说明。** Notix 是 Anuj 的 [DoNotNotify](https://github.com/anujja/DoNotNotify)（MIT License）经过品牌重塑与重新打包的分支，拥有独立的发布渠道、包名与签名密钥。两份版权声明均完整保留在 [LICENSE](LICENSE) 中。

---

## 为什么是 Notix

多数通知类应用要么过于简单（只有一个静音开关），要么过于 intrusive（云同步、账号、埋点）。Notix 围绕三条原则构建：

- **本地优先。** 你的通知永远不会离开设备，清单里没有任何网络权限。
- **规则驱动。** 行为完全由你显式定义的规则决定——没有内置的垃圾过滤来替你猜测。
- **扛得住系统。** 前台保活服务配合周期性健康检查，即使系统回收后台进程，监听器仍能持续运行。

## 功能特性

**核心引擎**
- 基于 `NotificationListenerService` 的实时捕获，由前台服务与健康检查支撑
- 每条规则执行有序动作链：**消除（Dismiss）· 静默（低优先级重发）· 打开（Open）· 点击按钮（Click Button）· 复制（标题 / 正文 / 标题+正文）· TTS 朗读 · 延迟（Delay）**
- 灵活匹配：指定一个或多个来源 App，支持 `包含任一 / 包含全部 / 不包含任一 / 不包含全部`，以及混合的 `包含 A 且不包含 B`
- 情境条件：屏幕亮灭、充电状态、勿扰模式、蓝牙耳机连接（可选指定设备名）、时间段 + 星期多选

**工作流**
- 可视化规则向导（应用 → 匹配 → 条件 → 动作）；也可在历史中点击任意通知直接由此创建规则
- 通知历史支持**按时间 / 按应用 / 已过滤**三个子标签，并配有按日统计图表面板
- 每条规则带命中计数，并有 **Blocked** 视图展示哪些通知被处理、由哪条规则处理
- 可将任意已处理通知重新发布回通知栏

**易用性**
- 懒加载 TTS，优先简体中文、不可用时回退系统语言，模板支持 `{app}` / `{title}` / `{text}` 占位符
- 带版本号的 JSON 导入/导出（兼容旧版格式）
- 设备端崩溃日志查看器
- 首次启动向导覆盖通知监听授权、电池优化豁免，以及小米、华为、OPPO、一加、vivo、三星等厂商的自启动引导
- OLED 友好的纯黑 Material Design 3 深色主题；紧凑的纯图标底部导航（长按显示标签）与统一的横屏布局

## 截图

> 占位说明——截图待补充。将图片放入 `docs/screenshots/` 并替换下方路径即可。

| 界面 | 文件 | 说明 |
| --- | --- | --- |
| 历史与统计 | `docs/screenshots/history.png` | 带按日图表面板的通知历史 |
| 规则列表 | `docs/screenshots/rules.png` | 带命中计数的已保存规则 |
| 规则向导 | `docs/screenshots/wizard.png` | 可视化规则编辑器（应用 → 匹配 → 条件 → 动作） |
| 设置 | `docs/screenshots/settings.png` | 导入/导出、权限管理、崩溃日志 |

建议尺寸：1080×2340（竖屏）或 2400×1600（横屏），PNG 格式。

## 运行要求

- **Android 7.0（API 24）及以上**
- 通知监听器权限（通过应用内设置向导授予）
- Android 13+ 还需通知发送权限以支撑拦截/静默流程

## 使用说明

1. 安装并启动应用——设置向导会引导你授予通知监听权限。
2. *（推荐）* 将 Notix 加入电池优化豁免，并在 OEM 设备上开启自启动，以保证监听服务持续运行。
3. 在 **History** 标签页查看收到的通知，图表面板提供按日的概览。
4. 点击某条通知即可由此创建规则；或前往 **Rules** 标签页使用可视化向导（应用 → 匹配 → 条件 → 动作）。
5. 被规则处理的通知会出现在 **Blocked** 标签页——点击任意条目可查看详情、编辑处理它的规则或还原通知。
6. 在 **Settings** 中可导入/导出规则、重置命中计数、清除历史或打开崩溃日志。

## 从源码构建

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK（R8 混淆，由 Notix 密钥库签名）
./gradlew installDebug       # 安装 Debug 构建到已连接的设备
```

签名凭据从 `local.properties`（`KEYSTORE_NOTIX_*`）或环境变量读取，**绝不**提交到仓库。

**工具链：** Gradle 8.13 · Android Gradle Plugin 8.13.2 · Kotlin 2.0.21 · `compileSdk 36` / `minSdk 24` / `targetSdk 36` · Java 11。

## 文档

面向开发者与贡献者的详细文档位于 [`docs/`](docs/)：

- [架构与代码库概览](docs/ARCHITECTURE.md)
- [API 参考](docs/API_REFERENCE.md)
- [开发者指南](docs/DEVELOPER_GUIDE.md)

版本历史见 [VERSION_HISTORY.md](VERSION_HISTORY.md)；各版本发布说明见 [RELEASE_NOTES.md](RELEASE_NOTES.md)。

## 状态与路线

Notix 当前为 **v8.9**，处于活跃开发阶段。v1 范围聚焦于通知捕获/历史管线、可视化拖拽规则编辑器（AND/OR）、OTP 自动复制、不重要通知自动移除，以及蓝牙 / 驾驶场景的 TTS 播报。

## License

MIT License — 详见 [LICENSE](LICENSE)。

