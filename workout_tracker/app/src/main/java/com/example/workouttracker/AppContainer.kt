package com.example.workouttracker

import android.content.Context
import com.example.workouttracker.data.WorkoutDatabase
import com.example.workouttracker.data.WorkoutRepository

class AppContainer(context: Context) {
    private val database = WorkoutDatabase.getInstance(context)
    val repository = WorkoutRepository(database.workoutDao())
}
