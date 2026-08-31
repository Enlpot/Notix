package com.enlpot.notix.plugin

/**
 * 内置简单分词器（v8.43.0）
 *
 * 无额外依赖、速度极快、包体积0增加。
 * 策略：按标点/空格/数字分割 → 保留2字以上词语 → 过滤停用词 → 常用词词典优先匹配
 *
 * 适用于通知标题这种短文本，完全够用。
 * 需要更精准分词的用户可可选安装 HanLP 高级分词插件。
 */
class SimpleWordTokenizer : WordTokenizer {

    companion object {
        // 停用词表（无实际意义的高频词，过滤掉）
        private val STOP_WORDS = setOf(
            "的", "了", "是", "在", "有", "和", "与", "或", "等", "及", "也", "都", "就",
            "通知", "消息", "提醒", "您", "你", "我", "他", "她", "它", "我们", "你们", "他们",
            "这个", "那个", "这些", "那些", "一个", "一些", "一下", "一样", "一直", "已经",
            "可以", "可能", "应该", "需要", "知道", "看到", "收到", "发现", "进行", "通过",
            "关于", "对于", "由于", "因此", "所以", "但是", "然而", "而且", "并且", "或者",
            "点击", "查看", "详情", "更多", "立即", "马上", "现在", "今天", "昨天", "明天",
            "上午", "下午", "晚上", "凌晨", "中午", "傍晚", "分钟", "小时", "秒", "日", "月", "年"
        )

        // 常用通知词词典（优先匹配，避免被分割）
        private val DICTIONARY = setOf(
            "快递", "包裹", "发货", "收货", "物流", "运输", "配送", "送达", "签收",
            "验证码", "验证", "登录", "注册", "密码", "账号", "账户", "安全",
            "账单", "消费", "支付", "付款", "收款", "转账", "余额", "交易", "退款",
            "直播", "视频", "音频", "音乐", "播放", "暂停", "下载", "上传",
            "系统", "更新", "升级", "维护", "修复", "优化", "功能", "版本",
            "会议", "日程", "提醒", "待办", "任务", "日历", "闹钟",
            "好友", "消息", "聊天", "评论", "点赞", "关注", "粉丝",
            "天气", "温度", "降雨", "空气质量", "预警",
            "导航", "路线", "交通", "路况", "出行",
            "健康", "运动", "步数", "心率", "睡眠",
            "银行", "信用卡", "贷款", "理财", "基金", "股票",
            "机票", "火车票", "酒店", "航班", "列车", "出行",
            "外卖", "订单", "餐饮", "美食", "配送",
            "购物", "商品", "优惠", "折扣", "促销", "秒杀",
            "游戏", "活动", "奖励", "礼包", "任务",
            "学习", "课程", "作业", "考试", "成绩",
            "工作", "邮件", "文档", "审批", "流程",
            "手机", "电脑", "设备", "蓝牙", "WiFi", "网络",
            "充电", "电量", "电池", "省电",
            "存储", "内存", "空间", "清理", "缓存",
            "权限", "隐私", "设置", "配置", "管理"
        )
    }

    override fun segment(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val result = mutableListOf<String>()
        val lowerText = text.lowercase()

        // 1. 先尝试词典匹配（最长匹配）
        var i = 0
        while (i < lowerText.length) {
            var matched = false
            // 从最长的词典词开始尝试匹配
            for (len in minOf(8, lowerText.length - i) downTo 2) {
                val candidate = lowerText.substring(i, i + len)
                if (candidate in DICTIONARY && candidate !in STOP_WORDS) {
                    result.add(candidate)
                    i += len
                    matched = true
                    break
                }
            }
            if (!matched) {
                i++
            }
        }

        // 2. 再按标点/空格/数字分割，提取剩余词语
        val segments = lowerText.split(Regex("[\\p{Punct}\\s\\d]+"))
        for (seg in segments) {
            val trimmed = seg.trim()
            // 保留2字以上、非停用词、且不是纯字母数字的词语
            if (trimmed.length >= 2 && trimmed !in STOP_WORDS) {
                // 检查是否是纯英文（英文整体保留）
                if (trimmed.matches(Regex("[a-zA-Z]+"))) {
                    if (trimmed.length >= 3) { // 英文至少3个字母
                        result.add(trimmed)
                    }
                } else if (trimmed.matches(Regex("[\\u4e00-\\u9fa5]+"))) {
                    // 纯中文，2字以上保留
                    result.add(trimmed)
                }
                // 混合中英文的整体保留
                else if (trimmed.length >= 2) {
                    result.add(trimmed)
                }
            }
        }

        // 3. 去重并返回
        return result.distinct()
    }

    override fun name(): String = "内置简单分词"
}
