package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PulseCallColorScheme = darkColorScheme(
  primary = PulseRose,
  onPrimary = TextPrimary,
  primaryContainer = PulseRoseContainer,
  onPrimaryContainer = PulseRoseLight,
  secondary = VisionCyan,
  onSecondary = ObsidianBg,
  secondaryContainer = VisionCyanContainer,
  onSecondaryContainer = VisionCyanLight,
  tertiary = ElectricViolet,
  background = ObsidianBg,
  onBackground = TextPrimary,
  surface = ObsidianSurface,
  onSurface = TextPrimary,
  surfaceVariant = ObsidianSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  error = DangerRed,
)

@Composable
fun PulseCallTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = PulseCallColorScheme,
    typography = Typography,
    content = content,
  )
}
