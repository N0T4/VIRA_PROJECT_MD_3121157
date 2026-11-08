package com.meta.spatial.samples.startersample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class LibraryDropState(
    val symbol: String,
    val title: String,
    val detail: String,
) {
  Available("+", "DROP TO ADD", "Bring the card into the target"),
  Active("+", "RELEASE TO ADD", "Save to your Library"),
  Saving("", "SAVING", "Adding to your Library"),
  Added("✓", "ADDED", "Saved under Planning"),
  AlreadySaved("✓", "ALREADY SAVED", "Your status and progress are kept"),
  Error("!", "COULD NOT ADD", "Try dropping again or use the card"),
}

private val DropSuccess = Color(0xFFC9E275)

@Composable
internal fun LibraryDropPanel(state: LibraryDropState) {
  val accent = when (state) {
    LibraryDropState.Available -> PosterPulse.BrandRed
    LibraryDropState.Active, LibraryDropState.Saving -> PosterPulse.FocusEdge
    LibraryDropState.Added, LibraryDropState.AlreadySaved -> DropSuccess
    LibraryDropState.Error -> PosterPulse.Error
  }
  val borderWidth = if (state == LibraryDropState.Active) 4.dp else 2.dp

  PosterPulseTheme {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(PosterPulse.PanelShape)
            .background(Brush.verticalGradient(listOf(PosterPulse.Surface, PosterPulse.Background)))
            .border(borderWidth, accent, PosterPulse.PanelShape)
            .padding(PosterPulse.Space16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8, Alignment.CenterVertically),
    ) {
      Text(
          text = "LIBRARY //",
          color = PosterPulse.TextSecondary,
          fontSize = PosterPulse.MetadataSize,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp,
      )
      Box(
          modifier = Modifier.size(58.dp)
              .background(accent.copy(alpha = 0.14f), PosterPulse.ControlShape)
              .border(1.dp, accent, PosterPulse.ControlShape),
          contentAlignment = Alignment.Center,
      ) {
        if (state == LibraryDropState.Saving) {
          CircularProgressIndicator(modifier = Modifier.size(28.dp), color = accent, strokeWidth = 3.dp)
        } else {
          Text(text = state.symbol, color = accent, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }
      }
      Text(
          text = state.title,
          color = PosterPulse.TextPrimary,
          fontSize = 28.sp,
          fontWeight = FontWeight.Black,
          textAlign = TextAlign.Center,
      )
      Text(
          text = state.detail,
          color = PosterPulse.TextSecondary,
          fontSize = PosterPulse.MetadataSize,
          textAlign = TextAlign.Center,
      )
    }
  }
}
