package com.example.lifetogether.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

@Composable
fun LifeTogetherTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content:
    @Composable
    () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> LifeTogetherDarkColorScheme
        else -> LifeTogetherLightColorScheme
    }

    CompositionLocalProvider(
        LocalTextStyle provides AppTypography.bodyMedium,
        LocalSpacing provides AppSpacing,
        LocalSizing provides AppSizing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = { Surface { content() } },
        )
    }
}

object LifeTogetherTokens {
    val spacing: SpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val sizing: SizingTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalSizing.current
}
