---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 26c411d2a0f0a2e307f470b439631140_a3828a979dfc11f1a65b525400826444
    ReservedCode1: 6b6yMipZNB9AVzfqAhBVnl/Xp8SN+5wu7JdmJcGutGGDJ5kKkWpB11+pPaQg/mKu215r7QUTHwDq2OiaJoh+eIVwAVJWMDBKaN7LptVtN1tcWBH4HnKMWCEBSUId97tNFvMejoig7dbsutSbx4Viq4NI1jpORgcYcmYsG6Ij+V72FI/yynOmTpLVaBM=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 26c411d2a0f0a2e307f470b439631140_a3828a979dfc11f1a65b525400826444
    ReservedCode2: 6b6yMipZNB9AVzfqAhBVnl/Xp8SN+5wu7JdmJcGutGGDJ5kKkWpB11+pPaQg/mKu215r7QUTHwDq2OiaJoh+eIVwAVJWMDBKaN7LptVtN1tcWBH4HnKMWCEBSUId97tNFvMejoig7dbsutSbx4Viq4NI1jpORgcYcmYsG6Ij+V72FI/yynOmTpLVaBM=
---

# Notix Release Notes

## 8.1 (2026-08-22)

自上一已发布版本 8.0 以来的变更汇总。

### 改进

- 历史页多级吸顶：底部 tab 固定吸顶，统计区与折叠分组头吸附在 tab 下方，滚动时不再顶掉标签栏
- 规则向导简化：移除底部删除键、动作箭头与添加条件按钮，条件区固定三行展示，新建规则更聚焦
- 通知详情弹窗加宽（两侧 padding 缩至 12dp），固定显示删除 / 打开 / 创建规则 / 还原 四个按钮
- 启动图标重制：替换为自适应矢量图标（三杠灰）并细化缩放表现

### 修复

- 通知历史列表布局顺序错误
- 折叠段展开/收起后滚动位置错误：收起折叠段后自动回到段头
- 同应用多折叠段折叠计数错误
- 通知详情弹窗按钮显示异常
## 8.2 (2026-08-23)

自上一已发布版本 8.1 以来的变更汇总。

### 改进

- 按钮体系统一：通知详情弹窗四按钮改为 Material3 Button 体系（删除红底/打开主题色/还原灰底/创建规则描边）；规则页、向导页、编辑页按钮统一 14dp 圆角
- 危险操作与圆角档位收敛：红底按钮/红字 TextButton 两种危险形态全项目一致；圆角统一为大容器 16dp、卡片 12dp、小组件 8dp
- 标题层级与弹窗统一：主页面 headlineMedium+Bold（设置页补齐主标题），次级页 titleLarge+返回键；冗余弹窗组件合并
- 图标规范化：周视图 ◀/▶ 与残留 Unicode 装饰字符替换为矢量图标

### 修复

- 移除设置页日期快捷入口与列表项不一致的描边按钮形态
- 通知详情弹窗冗余组件合并，删除未使用的 NotificationDetailsDialog

*（内容由AI生成，仅供参考）*
