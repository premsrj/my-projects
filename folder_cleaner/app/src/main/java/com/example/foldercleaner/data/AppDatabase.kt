package com.example.foldercleaner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SelectedFolderEntity::class, IgnoredExtensionEntity::class, CleanupRunEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun selectedFolderDao(): SelectedFolderDao
    abstract fun ignoredExtensionDao(): IgnoredExtensionDao
    abstract fun cleanupRunDao(): CleanupRunDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun create(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val created = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "folder_cleaner.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                instance = created
                created
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE selected_folders ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cleanup_runs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        executedAtMillis INTEGER NOT NULL,
                        trigger TEXT NOT NULL,
                        scannedCount INTEGER NOT NULL,
                        deletedCount INTEGER NOT NULL,
                        skippedCount INTEGER NOT NULL,
                        failedCount INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE cleanup_runs ADD COLUMN reclaimedBytes INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
