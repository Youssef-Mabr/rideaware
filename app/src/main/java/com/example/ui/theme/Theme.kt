package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RideAwareColorScheme = darkColorScheme(
  primary = TealPrimary,
  onPrimary = Color(0xFF041E17),
  primaryContainer = Color(0xFF00382E),
  onPrimaryContainer = TealAccent,
  secondary = CyanAccent,
  onSecondary = Color(0xFF003258),
  secondaryContainer = Color(0xFF0C243C),
  onSecondaryContainer = Color(0xFFBCE3FF),
  tertiary = WarningOrange,
  onTertiary = Color(0xFF451A00),
  error = DangerRed,
  onError = Color(0xFF450000),
  background = DarkCanvas,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = DarkSurfaceBorder,
  outlineVariant = Color(0xFF1E2C44)
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = RideAwareColorScheme,
    typography = Typography,
    content = content
  )
}

