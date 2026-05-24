package com.example.lifetogether.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class SizingTokens(
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val touchTargetMinimum: Dp = 48.dp,
    val avatarSmall: Dp = 40.dp,
    val avatarMedium: Dp = 56.dp,
    val avatarLarge: Dp = 72.dp,
)

val AppSizing = SizingTokens()

val LocalSizing = staticCompositionLocalOf { AppSizing }
