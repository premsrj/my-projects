package com.example.workouttracker.ui.track

import com.example.workouttracker.data.ExerciseType
import com.example.workouttracker.data.WorkoutSetEntity
import com.example.workouttracker.data.WorkoutSetWithExercise
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val numberFormat = DecimalFormat("0.##")
private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

fun formatSetSummary(type: ExerciseType, set: WorkoutSetEntity): String {
    return buildList {
        if (type.usesWeight) {
            add("${formatDecimal(set.weight)} kg")
        }
        if (type.usesReps) {
            add("${set.reps ?: 0} reps")
        }
        if (type.usesTime) {
            add("${set.durationSeconds ?: 0} sec")
        }
        if (type.usesDistance) {
            add("${formatDecimal(set.distance)} km")
        }
    }.joinToString(" | ")
}

fun formatSetSummary(type: ExerciseType, set: WorkoutSetWithExercise): String {
    return formatSetSummary(type, set.set)
}

fun formatDecimal(value: Double?): String {
    return numberFormat.format(value ?: 0.0)
}

fun formatDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
    return Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().format(dateFormatter)
}

fun formatTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
    return Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalTime().format(timeFormatter)
}
