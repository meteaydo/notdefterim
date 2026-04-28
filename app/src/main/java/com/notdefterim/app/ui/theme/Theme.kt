package com.notdefterim.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ─── Renk Şemaları ──────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
  primary = PrimaryDark,
  onPrimary = OnPrimaryDark,
  primaryContainer = PrimaryContainerDark,
  onPrimaryContainer = OnPrimaryContainerDark,
  secondary = SecondaryDark,
  onSecondary = OnSecondaryDark,
  secondaryContainer = SecondaryContainerDark,
  onSecondaryContainer = OnSecondaryContainerDark,
  tertiary = TertiaryDark,
  onTertiary = OnTertiaryDark,
  background = BackgroundDark,
  onBackground = OnBackgroundDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  error = ErrorDark,
  onError = OnErrorDark,
  outline = OutlineDark,
  outlineVariant = OutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
  primary = PrimaryLight,
  onPrimary = OnPrimaryLight,
  primaryContainer = PrimaryContainerLight,
  onPrimaryContainer = OnPrimaryContainerLight,
  secondary = SecondaryLight,
  onSecondary = OnSecondaryLight,
  secondaryContainer = SecondaryContainerLight,
  onSecondaryContainer = OnSecondaryContainerLight,
  tertiary = TertiaryLight,
  onTertiary = OnTertiaryLight,
  background = BackgroundLight,
  onBackground = OnBackgroundLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  error = ErrorLight,
  onError = OnErrorLight,
  outline = OutlineLight,
  outlineVariant = OutlineVariantLight
)

// ─── Not Kartı Renkleri için CompositionLocal ────────────────────────────────

data class NoteCardColors(val colors: List<Color>)

val LocalNoteCardColors = staticCompositionLocalOf {
  NoteCardColors(NoteColorsDark)
}

// ─── Uygulama Teması ─────────────────────────────────────────────────────────

@Composable
fun NotDefterimTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic Color: Android 12+ (API 31+) cihazlarda cüzdan/duvar kağıdı rengiyle uyum sağlar
  dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
    dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val noteCardColors = if (darkTheme) {
    NoteCardColors(NoteColorsDark)
  } else {
    NoteCardColors(NoteColorsLight)
  }

  CompositionLocalProvider(LocalNoteCardColors provides noteCardColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      content = content
    )
  }
}
