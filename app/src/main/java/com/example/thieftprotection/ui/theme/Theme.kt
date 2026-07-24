package com.example.thieftprotection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BeigeMossColorScheme = lightColorScheme(
    primary = MossGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = MossGreenLight,
    onPrimaryContainer = TextDarkForest,

    secondary = MossGreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = MossGreenLight,
    onSecondaryContainer = TextDarkForest,

    tertiary = MossGreenAccent,
    onTertiary = Color.White,

    background = BeigeBackground,
    onBackground = TextDarkForest,

    surface = BeigeCard,
    onSurface = TextDarkForest,

    surfaceVariant = BeigeSurface,
    onSurfaceVariant = TextMutedForest,

    outline = BeigeBorder,
    outlineVariant = BeigeBorder
)

@Composable
fun ThieftProtectionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BeigeMossColorScheme,
        typography = Typography,
        content = content
    )
}