package com.example.lifetogether.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.lifetogether.R

val bodyFontFamily = FontFamily(
    Font(R.font.lato_regular),
)
val bodyFontFamilyItalic = FontFamily(
    Font(R.font.lato_italic),
)

val displayFontFamily = FontFamily(
    Font(R.font.montserrat_alternates_regular),
)

// Default Material 3 typography values
val baseline = Typography()

// Display, headline, title, body, label (italic)
val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(
        fontFamily = displayFontFamily,
        fontSize = 36.sp,
    ),
    displayMedium = baseline.displayMedium.copy(
        fontFamily = displayFontFamily,
        fontSize = 32.sp,
    ),
    displaySmall = baseline.displaySmall.copy(
        fontFamily = displayFontFamily,
        fontSize = 28.sp,
    ),
    headlineLarge = baseline.headlineLarge.copy(
        fontFamily = displayFontFamily,
        fontSize = 28.sp,
    ),
    headlineMedium = baseline.headlineMedium.copy(
        fontFamily = displayFontFamily,
        fontSize = 26.sp,
    ),
    headlineSmall = baseline.headlineSmall.copy(
        fontFamily = displayFontFamily,
        fontSize = 24.sp,
    ),
    titleLarge = baseline.titleLarge.copy(
        fontFamily = displayFontFamily,
        fontSize = 22.sp,
    ),
    titleMedium = baseline.titleMedium.copy(
        fontFamily = displayFontFamily,
        fontSize = 20.sp,
    ),
    titleSmall = baseline.titleSmall.copy(
        fontFamily = displayFontFamily,
        fontSize = 18.sp,
    ),
    bodyLarge = baseline.bodyLarge.copy(
        fontFamily = bodyFontFamily,
        fontSize = 16.sp,
    ),
    bodyMedium = baseline.bodyMedium.copy(
        fontFamily = bodyFontFamily,
        fontSize = 14.sp,
    ),
    bodySmall = baseline.bodySmall.copy(
        fontFamily = bodyFontFamily,
        fontSize = 12.sp,
    ),
    labelLarge = baseline.labelLarge.copy(
        fontFamily = bodyFontFamilyItalic,
        fontSize = 20.sp,
    ),
    labelMedium = baseline.labelMedium.copy(
        fontFamily = bodyFontFamilyItalic,
        fontSize = 16.sp,
    ),
    labelSmall = baseline.labelSmall.copy(
        fontFamily = bodyFontFamilyItalic,
        fontSize = 12.sp,
    ),
)
