package com.enlpot.notix.plugin.wordtokenizer

import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.corpus.io.IOUtil

/**
 * HanLP 高级分词插件（v8.43.0）
 *
 * 基于 HanLP portable 版，提供更精准的中文分词。
 * 编译打包成 dex 文件后，由主 app 通过 DexClassLoader 动态加载。
 *
 * 用户可选安装，安装后主 app 的词云功能将使用此分词器。
 */
class HanLPWordTokenizer {

    init {
        configureDictPath()
    }

    /**
     * 配置 HanLP 词典路径。
     * 主 app 在加载插件前通过系统属性 notix.hanlp.root 传入词典根目录，
     * 这里把 HanLP.Config 各路径设为绝对路径（HanLP 惰性加载，首次 segment 时才读词典）。
     */
    private fun configureDictPath() {
        val root = System.getProperty(KEY_DICT_ROOT)
        if (root.isNullOrBlank()) return
        val d = "$root/data/dictionary"
        try {
            HanLP.Config.CoreDictionaryPath = "$d/CoreNatureDictionary.mini.txt"
            HanLP.Config.BiGramDictionaryPath = "$d/CoreNatureDictionary.ngram.mini.txt"
            HanLP.Config.CoreStopWordDictionaryPath = "$d/stopwords.txt"
            HanLP.Config.PersonDictionaryPath = "$d/person/nr.txt"
            HanLP.Config.PersonDictionaryTrPath = "$d/person/nr.tr.txt"
            HanLP.Config.PlaceDictionaryPath = "$d/place/ns.txt"
            HanLP.Config.PlaceDictionaryTrPath = "$d/place/ns.tr.txt"
            HanLP.Config.OrganizationDictionaryPath = "$d/organization/nt.txt"
            HanLP.Config.OrganizationDictionaryTrPath = "$d/organization/nt.tr.txt"
            HanLP.Config.CharTypePath = "$d/other/CharType.bin"
            HanLP.Config.CharTablePath = "$d/other/CharTable.txt"
            HanLP.Config.CustomDictionaryPath = arrayOf("$d/custom/CustomDictionary.txt")
        } catch (t: Throwable) {
            // 配置失败时使用默认路径（可能不可用），不阻断分词
        }
    }

    companion object {
        const val KEY_DICT_ROOT = "notix.hanlp.root"
    }

    // 停用词表（与内置分词器保持一致，过滤无意义高频词）
    private val stopWords = setOf(
        "的", "了", "是", "在", "有", "和", "与", "或", "等", "及", "也", "都", "就",
        "通知", "消息", "提醒", "您", "你", "我", "他", "她", "它",
        "这个", "那个", "这些", "那些", "一个", "一些", "一下",
        "可以", "可能", "应该", "需要",
        "点击", "查看", "详情", "更多", "立即"
    )

    fun segment(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        return try {
            // 使用 HanLP 标准分词
            val terms = HanLP.segment(text)
            terms.map { it.word.trim() }
                .filter { it.length >= 2 }
                .filter { it !in stopWords }
                .filter { it.matches(Regex("[\\u4e00-\\u9fa5a-zA-Z]+")) }
                .distinct()
        } catch (e: Exception) {
            // HanLP 加载失败时回退到简单分割
            text.split(Regex("[\\p{Punct}\\s\\d]+"))
                .map { it.trim() }
                .filter { it.length >= 2 }
                .filter { it !in stopWords }
                .distinct()
        }
    }

    fun name(): String = "HanLP 高级分词"
}



