package com.route.readers.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ReadingGreen,
    secondary = ProgressBlue,
    tertiary = CompletedGreen
)

private val LightColorScheme = lightColorScheme(
    primary = ReadingGreen,
    secondary = ProgressBlue,
    tertiary = CompletedGreen
)

@Composable
fun ReadersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
