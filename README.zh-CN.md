[English](README.md) | **简体中文**

# Notix

Notix 是一款 Android 应用，通过可自定义的规则过滤和拦截不必要的通知。

> **衍生于 Anuj 的 [DoNotNotify](https://github.com/anujja/DoNotNotify)**（MIT License）。Notix 是经过品牌重塑与重新打包的分支，拥有独立的发布渠道、包名和签名密钥。

## 功能特性

- **Notification Blocking** — 基于 Android 的 NotificationListenerService 实时拦截通知
- **Flexible Rules** — 支持创建 denylist（拦截匹配项）、allowlist（仅放行匹配项）或 stack 规则
- **Notification Stacking** — STACK 规则不做拦截；将高频应用的多条通知折叠为单个可展开的 Notification Group，既保留通知又避免干扰
- **Pattern Matching** — 使用简单的 contains 或正则表达式匹配通知标题/正文
- **Time-Based Rules** — 可设定规则仅在特定时间段内生效
- **Notification History** — 查看所有接收到的通知，保留时长可配置
- **Blocked History** — 追踪哪些通知被拦截，以及由哪条规则拦截
- **Import/Export** — 将规则以 JSON 文件形式备份与恢复
- **Fully Offline** — 无网络权限，不收集任何数据

## 运行要求

- Android 7.0（API 24）及以上
- 授予通知监听器（Notification listener）权限

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

Release APK 使用 Notix 发布密钥库签名。签名凭据从 `local.properties`（键名 `KEYSTORE_NOTIX_*`）或环境变量读取，**绝不**提交到仓库。

## 使用说明

1. 首次启动后按提示授予通知监听器权限
2. 在 **History** 标签页查看接收到的通知
3. 点击某条通知即可创建拦截规则；或前往 **Rules** 标签页手动创建规则
4. 被拦截的通知会出现在 **Blocked** 标签页
5. 点击任意被拦截的通知可查看详情，或编辑拦截它的那条规则

## 文档

面向开发者与贡献者的详细文档位于 [`docs/`](docs/) 目录：

- **[架构与代码库概览](docs/ARCHITECTURE.md)** — 项目结构、数据模型、核心服务、存储层、UI 层、数据流图、导航图与类依赖关系
- **[API 参考](docs/API_REFERENCE.md)** — 每个类与 Composable 的方法级参考文档
- **[开发者指南](docs/DEVELOPER_GUIDE.md)** — 关于新增功能、页面、规则、存储及运行测试的实战指南

## License

MIT License — 详见 [LICENSE](LICENSE)。

本项目基于 Anuj 的 [DoNotNotify](https://github.com/anujja/DoNotNotify)（© 2025）衍生而来，并包含 Notix 自身的修改（© 2026）。依据 MIT License 要求，两份版权声明均完整保留在 LICENSE 文件中。
