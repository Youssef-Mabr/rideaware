package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
  private fun ratio(a: Color, b: Color): Float {
    val x = a.luminance()
    val y = b.luminance()
    return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
  }

  @Test fun bodyTextAndStatusLabelsMeetMinimumContrastOnCoreSurfaces() {
    listOf(DarkCanvas, DarkSurface, DarkSurfaceElevated).forEach { background ->
      listOf(TextPrimary, TextSecondary, TextMuted, TealPrimary, SuccessGreen, WarningOrange, DangerRed).forEach { foreground ->
        assertTrue("Contrast ${ratio(foreground, background)} for $foreground on $background", ratio(foreground, background) >= 4.5f)
      }
    }
  }

  @Test fun primaryAndDangerButtonTextMeetMinimumContrast() {
    assertTrue(ratio(TealPrimary, DarkCanvas) >= 4.5f)
    assertTrue(ratio(DangerContainer, Color.White) >= 4.5f)
    assertTrue(ratio(DarkSurfaceBorder, DarkSurfaceElevated) >= 3f)
  }
}
