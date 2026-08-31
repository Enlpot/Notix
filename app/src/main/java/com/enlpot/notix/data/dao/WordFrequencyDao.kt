package com.enlpot.notix.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enlpot.notix.data.entity.WordFrequencyEntity

@Dao
interface WordFrequencyDao {

    /** 查询某个时间范围的 Top N 热词（按词频降序） */
    @Query("SELECT * FROM word_frequency WHERE time_range = :timeRange ORDER BY count DESC LIMIT :limit")
    suspend fun getTopWords(timeRange: String, limit: Int = 50): List<WordFrequencyEntity>

    /** 查询某个词语在某个时间范围的词频 */
    @Query("SELECT * FROM word_frequency WHERE word = :word AND time_range = :timeRange")
    suspend fun getWordFrequency(word: String, timeRange: String): WordFrequencyEntity?

    /** 插入或更新词频（冲突时替换） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wordFrequency: WordFrequencyEntity)

    /** 批量插入或更新词频 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(wordFrequencies: List<WordFrequencyEntity>)

    /** 某个词语词频 +1（不存在则插入，count=1） */
    @Query("""
        INSERT INTO word_frequency (word, time_range, count, last_updated)
        VALUES (:word, :timeRange, 1, :timestamp)
        ON CONFLICT(word, time_range) DO UPDATE SET
            count = count + 1,
            last_updated = :timestamp
    """)
    suspend fun incrementWord(word: String, timeRange: String, timestamp: Long = System.currentTimeMillis())

    /** 某个词语词频 -1（减到0则删除） */
    @Query("""
        UPDATE word_frequency SET count = count - 1, last_updated = :timestamp
        WHERE word = :word AND time_range = :timeRange AND count > 0
    """)
    suspend fun decrementWord(word: String, timeRange: String, timestamp: Long = System.currentTimeMillis())

    /** 删除词频为0的记录 */
    @Query("DELETE FROM word_frequency WHERE count <= 0")
    suspend fun deleteZeroCount()

    /** 清空某个时间范围的所有词频 */
    @Query("DELETE FROM word_frequency WHERE time_range = :timeRange")
    suspend fun clearByTimeRange(timeRange: String)

    /** 清空所有词频 */
    @Query("DELETE FROM word_frequency")
    suspend fun clearAll()

    /** 查询某个时间范围的词语总数 */
    @Query("SELECT COUNT(*) FROM word_frequency WHERE time_range = :timeRange")
    suspend fun countByTimeRange(timeRange: String): Int

    /** 查询某个时间范围的总通知数（所有词频之和，用于估算） */
    @Query("SELECT SUM(count) FROM word_frequency WHERE time_range = :timeRange")
    suspend fun sumCountByTimeRange(timeRange: String): Int?
}
