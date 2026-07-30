package com.illumined.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object IlluminedThemeTokens {
    val Blue = Color(0xFF3B6FA0)
    val Gold = Color(0xFFBF944A)
    val Cream = Color(0xFFF7F7F5)
    val Parchment = Color(0xFFE3E3DA)
    val Ink = Color(0xFF1E1C1A)
    val SecondaryText = Color(0xFF6B665E)
    val Card = Color(0xFFFDFDFC)
    val Serif = FontFamily.Serif
    val CardCornerRadius = 16.dp
    val FieldCornerRadius = 12.dp
    val ButtonCornerRadius = 14.dp
    val CardBorderWidth = 1.dp
    const val CardBorderAlpha = 0.22f
}

private val IlluminedColors = lightColorScheme(
    primary = IlluminedThemeTokens.Blue,
    onPrimary = Color.White,
    secondary = IlluminedThemeTokens.Gold,
    background = IlluminedThemeTokens.Cream,
    onBackground = IlluminedThemeTokens.Ink,
    surface = IlluminedThemeTokens.Card,
    onSurface = IlluminedThemeTokens.Ink,
    surfaceVariant = IlluminedThemeTokens.Cream,
    onSurfaceVariant = IlluminedThemeTokens.SecondaryText,
    outline = IlluminedThemeTokens.Gold.copy(alpha = IlluminedThemeTokens.CardBorderAlpha),
)

private fun serif(style: androidx.compose.ui.text.TextStyle) = style.copy(fontFamily = IlluminedThemeTokens.Serif)

private val IlluminedTypography = Typography().let { source ->
    source.copy(
        displayLarge = serif(source.displayLarge), displayMedium = serif(source.displayMedium), displaySmall = serif(source.displaySmall),
        headlineLarge = serif(source.headlineLarge), headlineMedium = serif(source.headlineMedium), headlineSmall = serif(source.headlineSmall),
        titleLarge = serif(source.titleLarge), titleMedium = serif(source.titleMedium), titleSmall = serif(source.titleSmall),
        bodyLarge = serif(source.bodyLarge), bodyMedium = serif(source.bodyMedium), bodySmall = serif(source.bodySmall),
        labelLarge = serif(source.labelLarge), labelMedium = serif(source.labelMedium), labelSmall = serif(source.labelSmall),
    )
}

private val IlluminedShapes = Shapes(
    small = RoundedCornerShape(IlluminedThemeTokens.FieldCornerRadius),
    medium = RoundedCornerShape(IlluminedThemeTokens.ButtonCornerRadius),
    large = RoundedCornerShape(IlluminedThemeTokens.CardCornerRadius),
)

@Composable
fun IlluminedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IlluminedColors,
        typography = IlluminedTypography,
        shapes = IlluminedShapes,
    ) {
        CompositionLocalProvider(LocalTextStyle provides IlluminedTypography.bodyLarge, content = content)
    }
}
