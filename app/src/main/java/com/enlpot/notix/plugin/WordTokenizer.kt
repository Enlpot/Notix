package com.enlpot.notix.plugin

/**
 * 分词器接口（v8.43.0：插件化架构）
 *
 * 主 app 提供此接口，内置简单分词器实现；
 * 高级分词插件（如 HanLP）可通过 DexClassLoader 动态加载，实现此接口。
 */
interface WordTokenizer {

    /**
     * 对文本进行分词，返回词语列表。
     * @param text 待分词文本（通知标题+内容）
     * @return 分词结果列表
     */
    fun segment(text: String): List<String>

    /**
     * 分词器名称（用于设置页显示）。
     */
    fun name(): String
}
