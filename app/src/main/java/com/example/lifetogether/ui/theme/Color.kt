package com.example.lifetogether.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private object DarkPalette {
	val Background = Color(0xFF120E15)
	val Surface = Color(0xFF120E15)
	val SurfaceVariant = Color(0xFF1E1822)
	val OnBackground = Color(0xFFE6E1E5)
	val OnSurface = Color(0xFFE6E1E5)
	val OnSurfaceVariant = Color(0xFFCAC4D0)

	val Primary = Color(0xFF7E1E80)
	val OnPrimary = Color(0xFFFFFFFF)
	val PrimaryContainer = Color(0xFF3A233D)
	val OnPrimaryContainer = Color(0xFFE2DCE6)

	val Secondary = Color(0xFF4DB6AC)
	val OnSecondary = Color(0xFF002221)
	val SecondaryContainer = Color(0xFF004F4F)
	val OnSecondaryContainer = Color(0xFFA6F4EA)

	val Tertiary = Color(0xFFB39AC0)
	val OnTertiary = Color(0xFF22102B)
	val TertiaryContainer = Color(0xFF3B2C46)
	val OnTertiaryContainer = Color(0xFFE7DCEF)

	val Error = Color(0xFFF2B8B5)
	val OnError = Color(0xFF601410)
	val ErrorContainer = Color(0xFF7A2924)
	val OnErrorContainer = Color(0xFFFFDAD6)

	val Outline = Color(0xFF958D9A)
	val OutlineVariant = Color(0xFF4A4350)
	val Scrim = Color(0xFF000000)
	val InverseSurface = Color(0xFFE6E1E5)
	val InverseOnSurface = Color(0xFF2D2832)
	val InversePrimary = Color(0xFF9D4AA1)
}

// Light mode values are intentionally identical for now and can diverge later.
private object LightPalette {
	val Background = DarkPalette.Background
	val Surface = DarkPalette.Surface
	val SurfaceVariant = DarkPalette.SurfaceVariant
	val OnBackground = DarkPalette.OnBackground
	val OnSurface = DarkPalette.OnSurface
	val OnSurfaceVariant = DarkPalette.OnSurfaceVariant

	val Primary = DarkPalette.Primary
	val OnPrimary = DarkPalette.OnPrimary
	val PrimaryContainer = DarkPalette.PrimaryContainer
	val OnPrimaryContainer = DarkPalette.OnPrimaryContainer

	val Secondary = DarkPalette.Secondary
	val OnSecondary = DarkPalette.OnSecondary
	val SecondaryContainer = DarkPalette.SecondaryContainer
	val OnSecondaryContainer = DarkPalette.OnSecondaryContainer

	val Tertiary = DarkPalette.Tertiary
	val OnTertiary = DarkPalette.OnTertiary
	val TertiaryContainer = DarkPalette.TertiaryContainer
	val OnTertiaryContainer = DarkPalette.OnTertiaryContainer

	val Error = DarkPalette.Error
	val OnError = DarkPalette.OnError
	val ErrorContainer = DarkPalette.ErrorContainer
	val OnErrorContainer = DarkPalette.OnErrorContainer

	val Outline = DarkPalette.Outline
	val OutlineVariant = DarkPalette.OutlineVariant
	val Scrim = DarkPalette.Scrim
	val InverseSurface = DarkPalette.InverseSurface
	val InverseOnSurface = DarkPalette.InverseOnSurface
	val InversePrimary = DarkPalette.InversePrimary
}

internal val LifeTogetherDarkColorScheme = darkColorScheme(
	primary = DarkPalette.Primary,
	onPrimary = DarkPalette.OnPrimary,
	primaryContainer = DarkPalette.PrimaryContainer,
	onPrimaryContainer = DarkPalette.OnPrimaryContainer,
	secondary = DarkPalette.Secondary,
	onSecondary = DarkPalette.OnSecondary,
	secondaryContainer = DarkPalette.SecondaryContainer,
	onSecondaryContainer = DarkPalette.OnSecondaryContainer,
	tertiary = DarkPalette.Tertiary,
	onTertiary = DarkPalette.OnTertiary,
	tertiaryContainer = DarkPalette.TertiaryContainer,
	onTertiaryContainer = DarkPalette.OnTertiaryContainer,
	error = DarkPalette.Error,
	onError = DarkPalette.OnError,
	errorContainer = DarkPalette.ErrorContainer,
	onErrorContainer = DarkPalette.OnErrorContainer,
	background = DarkPalette.Background,
	onBackground = DarkPalette.OnBackground,
	surface = DarkPalette.Surface,
	onSurface = DarkPalette.OnSurface,
	surfaceVariant = DarkPalette.SurfaceVariant,
	onSurfaceVariant = DarkPalette.OnSurfaceVariant,
	surfaceContainerHigh = DarkPalette.PrimaryContainer,
	outline = DarkPalette.Outline,
	outlineVariant = DarkPalette.OutlineVariant,
	scrim = DarkPalette.Scrim,
	inverseSurface = DarkPalette.InverseSurface,
	inverseOnSurface = DarkPalette.InverseOnSurface,
	inversePrimary = DarkPalette.InversePrimary,
)

internal val LifeTogetherLightColorScheme = lightColorScheme(
	primary = LightPalette.Primary,
	onPrimary = LightPalette.OnPrimary,
	primaryContainer = LightPalette.PrimaryContainer,
	onPrimaryContainer = LightPalette.OnPrimaryContainer,
	secondary = LightPalette.Secondary,
	onSecondary = LightPalette.OnSecondary,
	secondaryContainer = LightPalette.SecondaryContainer,
	onSecondaryContainer = LightPalette.OnSecondaryContainer,
	tertiary = LightPalette.Tertiary,
	onTertiary = LightPalette.OnTertiary,
	tertiaryContainer = LightPalette.TertiaryContainer,
	onTertiaryContainer = LightPalette.OnTertiaryContainer,
	error = LightPalette.Error,
	onError = LightPalette.OnError,
	errorContainer = LightPalette.ErrorContainer,
	onErrorContainer = LightPalette.OnErrorContainer,
	background = LightPalette.Background,
	onBackground = LightPalette.OnBackground,
	surface = LightPalette.Surface,
	onSurface = LightPalette.OnSurface,
	surfaceVariant = LightPalette.SurfaceVariant,
	onSurfaceVariant = LightPalette.OnSurfaceVariant,
	surfaceContainerHigh = DarkPalette.PrimaryContainer,
	outline = LightPalette.Outline,
	outlineVariant = LightPalette.OutlineVariant,
	scrim = LightPalette.Scrim,
	inverseSurface = LightPalette.InverseSurface,
	inverseOnSurface = LightPalette.InverseOnSurface,
	inversePrimary = LightPalette.InversePrimary,
)
