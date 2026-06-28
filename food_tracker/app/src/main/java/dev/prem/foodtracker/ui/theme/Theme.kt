package dev.prem.foodtracker.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

private val LightColors = lightColorScheme(
    primary = HerbGreen,
    onPrimary = Color.White,
    secondary = SpiceOrange,
    onSecondary = Color.White,
    tertiary = Berry,
    background = Cream,
    onBackground = Graphite,
    surface = Color.White,
    onSurface = Graphite,
    surfaceVariant = Mist,
    onSurfaceVariant = Graphite
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DD39E),
    onPrimary = Color(0xFF0A2D1D),
    secondary = Color(0xFFF2B365),
    onSecondary = Color(0xFF321B00),
    tertiary = Color(0xFFF0A98B),
    background = Color(0xFF121A16),
    onBackground = Color(0xFFEAF4EE),
    surface = Color(0xFF1A2620),
    onSurface = Color(0xFFEAF4EE),
    surfaceVariant = Color(0xFF26342D),
    onSurfaceVariant = Color(0xFFC7D5CC)
)

@Composable
fun FoodTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            activity.window.setBackgroundDrawable(ColorDrawable(colorScheme.background.toArgb()))
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FoodTypography,
        content = content
    )
}
