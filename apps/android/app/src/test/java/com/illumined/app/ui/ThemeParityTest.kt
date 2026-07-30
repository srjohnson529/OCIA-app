package com.illumined.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.illumined.app.ui.theme.IlluminedThemeTokens
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeParityTest {
    @Test fun paletteMatchesIosIlluminedThemeRgbValues() {
        assertEquals(Color(0xFF3B6FA0), IlluminedThemeTokens.Blue)
        assertEquals(Color(0xFFBF944A), IlluminedThemeTokens.Gold)
        assertEquals(Color(0xFFF7F7F5), IlluminedThemeTokens.Cream)
        assertEquals(Color(0xFFE3E3DA), IlluminedThemeTokens.Parchment)
        assertEquals(Color(0xFF1E1C1A), IlluminedThemeTokens.Ink)
        assertEquals(Color(0xFF6B665E), IlluminedThemeTokens.SecondaryText)
    }

    @Test fun sharedGeometryAndFontFallbackMatchIosIntent() {
        assertEquals(16.dp, IlluminedThemeTokens.CardCornerRadius)
        assertEquals(12.dp, IlluminedThemeTokens.FieldCornerRadius)
        assertEquals(14.dp, IlluminedThemeTokens.ButtonCornerRadius)
        assertEquals(1.dp, IlluminedThemeTokens.CardBorderWidth)
        assertEquals(0.22f, IlluminedThemeTokens.CardBorderAlpha)
        assertEquals(FontFamily.Serif, IlluminedThemeTokens.Serif)
    }
}
