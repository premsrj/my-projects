package com.example.workouttracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DeepBlue,
    onPrimary = SurfaceWhite,
    secondary = ElectricCyan,
    onSecondary = DeepBlue,
    tertiary = Ember,
    background = WarmSand,
    onBackground = DeepBlue,
    surface = SurfaceWhite,
    onSurface = DeepBlue
)

private val DarkColors = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepBlue,
    secondary = Ember,
    onSecondary = SurfaceWhite,
    background = DeepBlue,
    onBackground = SurfaceWhite,
    surface = Color(0xFF102839),
    onSurface = SurfaceWhite
)

@Composable
fun WorkoutTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WorkoutTypography,
        content = content
    )
}
