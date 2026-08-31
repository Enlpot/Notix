package com.enlpot.notix.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.enlpot.notix.data.dao.NotificationChangeDao
import com.enlpot.notix.data.dao.WordFrequencyDao
import com.enlpot.notix.data.dao.NotificationGroupDao
import com.enlpot.notix.data.entity.NotificationChangeEntity
import com.enlpot.notix.data.entity.NotificationGroupEntity
import com.enlpot.notix.data.entity.WordFrequencyEntity

@Database(
    entities = [NotificationGroupEntity::class, NotificationChangeEntity::class, WordFrequencyEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationGroupDao(): NotificationGroupDao
    abstract fun notificationChangeDao(): NotificationChangeDao

    abstract fun wordFrequencyDao(): WordFrequencyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v8.43.0：数据库 v4 -> v5 迁移，新增 word_frequency 词频统计表
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS word_frequency (
                        word TEXT NOT NULL,
                        time_range TEXT NOT NULL,
                        count INTEGER NOT NULL DEFAULT 0,
                        last_updated INTEGER NOT NULL,
                        PRIMARY KEY (word, time_range)
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_word_frequency_time_range_count ON word_frequency(time_range, count)")
            }
        }

        // v8.43.0：数据库 v3 -> v4 迁移，notification_change 表新增 channel_id 字段
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notification_change ADD COLUMN channel_id TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "notix.db"
            )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

