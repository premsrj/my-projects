package com.example.foldercleaner.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = SoftWhite,
    secondary = Moss,
    onSecondary = Slate,
    background = Sand,
    onBackground = Slate,
    surface = SoftWhite,
    onSurface = Slate
)

private val DarkColors = darkColorScheme(
    primary = Moss,
    secondary = Clay,
    background = Slate,
    onBackground = SoftWhite,
    surface = ColorTokens.DarkSurface,
    onSurface = SoftWhite
)

@Composable
fun FolderCleanerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

private object ColorTokens {
    val DarkSurface = Slate
}
