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
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationGroupDao(): NotificationGroupDao
    abstract fun notificationChangeDao(): NotificationChangeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
