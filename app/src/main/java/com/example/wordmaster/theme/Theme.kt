package com.example.wordmaster.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = TerracottaBrand,
  secondary = CoralAccent,
  background = DeepDark,
  surface = DarkSurface,
  onPrimary = Ivory,
  onSecondary = Ivory,
  onBackground = WarmSilver,
  onSurface = WarmSilver,
  error = ErrorCrimson
)

private val LightColorScheme =
  lightColorScheme(
    primary = TerracottaBrand,
    secondary = CoralAccent,
    background = Parchment,
    surface = Ivory,
    onPrimary = Ivory,
    onSecondary = AnthropicNearBlack,
    onBackground = AnthropicNearBlack,
    onSurface = AnthropicNearBlack,
    surfaceVariant = WarmSand,
    onSurfaceVariant = OliveGray,
    outline = BorderWarm,
    error = ErrorCrimson
  )

@Composable
fun WordMasterTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
