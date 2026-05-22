package com.example.diarioobras.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CavvaColorScheme = darkColorScheme(
    primary          = Signal,
    onPrimary        = Asphalt,
    background       = Asphalt,
    onBackground     = FogWhite,
    surface          = AsphaltMid,
    onSurface        = FogWhite,
    surfaceVariant   = AsphaltLight,
    onSurfaceVariant = FogWhite,
    outline          = FieldBorder,
    error            = CavvaDanger,
    onError          = FogWhite
)

@Composable
fun DiarioObrasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CavvaColorScheme,
        typography = Typography,
        content = content
    )
}
