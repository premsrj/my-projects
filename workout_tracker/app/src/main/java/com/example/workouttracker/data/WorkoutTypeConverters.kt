package com.example.workouttracker.data

import androidx.room.TypeConverter

class WorkoutTypeConverters {
    @TypeConverter
    fun toExerciseType(value: String): ExerciseType = ExerciseType.valueOf(value)

    @TypeConverter
    fun fromExerciseType(type: ExerciseType): String = type.name
}
