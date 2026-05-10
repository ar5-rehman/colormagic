package com.colormagic.kids.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,

    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    background = Neutral,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,

    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun ColorMagicKidsTheme(
    content: @Composable () -> Unit
) {
    // Brand identity is fixed across light & dark system settings.
    // Dynamic color (Material You) is intentionally disabled, and we
    // never swap to a darkColorScheme — the kids palette must stay
    // pastel regardless of the device theme.
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
