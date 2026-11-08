package com.meta.spatial.samples.startersample

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Runtime subset of config/design_tokens.proposed.json.
internal object PosterPulse {
  val Background = Color(0xFF0B0B0F)
  val Surface = Color(0xFF1C1C22)
  val SurfaceRaised = Color(0xFF2A2A31)
  val TextPrimary = Color(0xFFEDE8E2)
  val TextSecondary = Color(0xFFB7B2B7)
  val BrandRed = Color(0xFFE63334)
  val FocusEdge = Color(0xFFFF5B5F)
  val Error = Color(0xFFFF7A80)
  val AtmospherePurple = Color(0xFF8A84BA)
  val Outline = Color(0xFF706870)

  val Space4 = 4.dp
  val Space8 = 8.dp
  val Space12 = 12.dp
  val Space16 = 16.dp
  val ControlShape = RoundedCornerShape(8.dp)
  val PanelShape = RoundedCornerShape(16.dp)
  val DisplaySize = 40.sp
  val BodySize = 20.sp
  val MetadataSize = 16.sp
}

private val colors = darkColorScheme(
    primary = PosterPulse.BrandRed,
    onPrimary = PosterPulse.Background,
    background = PosterPulse.Background,
    onBackground = PosterPulse.TextPrimary,
    surface = PosterPulse.Surface,
    onSurface = PosterPulse.TextPrimary,
    surfaceVariant = PosterPulse.SurfaceRaised,
    onSurfaceVariant = PosterPulse.TextSecondary,
    outline = PosterPulse.Outline,
    error = PosterPulse.Error,
    tertiary = PosterPulse.AtmospherePurple,
)

private val baseTypography = Typography()
private val typography = Typography(
    bodyLarge = baseTypography.bodyLarge.copy(fontSize = PosterPulse.BodySize, lineHeight = 28.sp),
    labelLarge = baseTypography.labelLarge.copy(fontSize = PosterPulse.MetadataSize),
)

@Composable
internal fun PosterPulseTheme(content: @Composable () -> Unit) {
  MaterialTheme(
      colorScheme = colors,
      shapes = Shapes(
          small = PosterPulse.ControlShape,
          medium = PosterPulse.ControlShape,
          large = PosterPulse.PanelShape,
      ),
      typography = typography,
      content = content,
  )
}
