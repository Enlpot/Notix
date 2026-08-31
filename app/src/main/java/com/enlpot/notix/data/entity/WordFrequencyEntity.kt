package com.enlpot.notix.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 词频统计表（v8.43.0：通知热词词云功能）
 *
 * 存储通知标题和内容分词后的词频，按时间范围分组。
 * 用于统计页词云展示，用户打开统计页直接读此表，0 计算。
 */
@Entity(
    tableName = "word_frequency",
    primaryKeys = ["word", "time_range"],
    indices = [
        Index(value = ["time_range", "count"])
    ]
)
data class WordFrequencyEntity(
    /** 词语 */
    val word: String,

    /** 时间范围：today / week / month / all */
    val time_range: String,

    /** 出现次数 */
    val count: Int = 0,

    /** 最后更新时间戳 */
    val last_updated: Long = System.currentTimeMillis()
)
