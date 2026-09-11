package com.enlpot.notix.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.enlpot.notix.data.dao.NotificationChangeDao
import com.enlpot.notix.data.dao.NotificationGroupDao
import com.enlpot.notix.data.entity.NotificationChangeEntity
import com.enlpot.notix.data.entity.NotificationGroupEntity

@Database(
    entities = [NotificationGroupEntity::class, NotificationChangeEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationGroupDao(): NotificationGroupDao
    abstract fun notificationChangeDao(): NotificationChangeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v8.43.0：数据库 v4 -> v5 迁移，新增 word_frequency 词频统计表（历史迁移路径保留）
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

        // v8.50.0：数据库 v5 -> v6 迁移，notification_change 表新增 cancel_reason 字段
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notification_change ADD COLUMN cancel_reason INTEGER")
            }
        }

        // v8.43.0：数据库 v3 -> v4 迁移，notification_change 表新增 channel_id 字段
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notification_change ADD COLUMN channel_id TEXT")
            }
        }

        // v8.57.0：移除词频/词云功能，删除 word_frequency 表
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS word_frequency")
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
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
