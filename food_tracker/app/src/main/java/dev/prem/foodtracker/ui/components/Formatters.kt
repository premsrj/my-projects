package dev.prem.foodtracker.ui.components

import kotlin.math.roundToInt

fun formatDouble(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

fun parseNonNegativeDouble(input: String): Double? {
    val value = input.trim().toDoubleOrNull() ?: return null
    return if (value >= 0.0) value else null
}
