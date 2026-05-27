package com.example.workouttracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ExerciseEntity::class,
        WorkoutSetEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(WorkoutTypeConverters::class)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE workout_sets ADD COLUMN isPr INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE workout_sets ADD COLUMN prOneRepMax REAL"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE workout_sets ADD COLUMN supersetGroupId TEXT"
                )
                db.execSQL(
                    "ALTER TABLE workout_sets ADD COLUMN supersetRound INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE workout_sets ADD COLUMN supersetPosition INTEGER"
                )
            }
        }

        @Volatile
        private var instance: WorkoutDatabase? = null

        fun getInstance(context: Context): WorkoutDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_tracker.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also {
                    instance = it
                }
            }
        }
    }
}
