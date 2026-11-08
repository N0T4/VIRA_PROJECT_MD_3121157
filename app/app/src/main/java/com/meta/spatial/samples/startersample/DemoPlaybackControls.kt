package com.meta.spatial.samples.startersample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToLong

@Composable
internal fun DemoPlaybackControls(
    isPlaying: Boolean,
    isEnded: Boolean,
    positionMs: Long,
    durationMs: Long,
    isLoading: Boolean,
    error: String?,
    isCardVisible: Boolean,
    inputEnabled: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleCard: () -> Unit,
    onClose: () -> Unit,
) {
  val ready = !isLoading && error == null
  val seekEnabled = inputEnabled && ready && durationMs > 0L
  var seekDraft by remember(durationMs) { mutableStateOf<Float?>(null) }
  LaunchedEffect(seekEnabled) {
    if (!seekEnabled) seekDraft = null
  }
  val positionFraction = if (durationMs > 0L) {
    positionMs.coerceIn(0L, durationMs).toFloat() / durationMs.toFloat()
  } else {
    0f
  }
  val sliderPosition = seekDraft ?: positionFraction
  val displayTimeMs = if (seekDraft == null) positionMs else (sliderPosition * durationMs).roundToLong()
  val elapsed = playbackTime(displayTimeMs)
  val duration = if (durationMs > 0L) playbackTime(durationMs) else "–:––"
  val status = when {
    error != null -> error
    isLoading -> "Loading video…"
    isPlaying -> "Playing"
    isEnded -> "Ended"
    else -> "Paused"
  }

  PosterPulseTheme {
    Column(
        modifier = Modifier.fillMaxSize()
            .clip(PosterPulse.PanelShape)
            .background(PosterPulse.Background)
            .border(1.dp, PosterPulse.Outline, PosterPulse.PanelShape)
            .padding(horizontal = PosterPulse.Space16, vertical = PosterPulse.Space8),
        verticalArrangement = Arrangement.Center,
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space12),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = "VIRA // DEMO PLAYBACK",
            color = PosterPulse.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = status,
            color = if (error == null) PosterPulse.TextSecondary else PosterPulse.Error,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        OutlinedButton(
            onClick = onToggleCard,
            enabled = inputEnabled,
            modifier = Modifier.widthIn(min = 126.dp).heightIn(min = 48.dp),
        ) {
          Text(if (isCardVisible) "Hide card" else "Show card")
        }
      }
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space12),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
            onClick = onPlayPause,
            enabled = inputEnabled && ready,
            modifier = Modifier.widthIn(min = 88.dp).heightIn(min = 48.dp),
        ) {
          Text(when {
            isPlaying -> "Pause"
            isEnded -> "Replay"
            else -> "Play"
          })
        }
        Slider(
            value = sliderPosition,
            onValueChange = { seekDraft = it },
            onValueChangeFinished = {
              val fraction = seekDraft
              seekDraft = null
              if (seekEnabled && fraction != null) onSeek((fraction * durationMs).roundToLong())
            },
            enabled = seekEnabled,
            modifier = Modifier.weight(1f).semantics {
              contentDescription = "Playback position"
              stateDescription = "$elapsed of $duration"
            },
            colors = SliderDefaults.colors(
                thumbColor = PosterPulse.FocusEdge,
                activeTrackColor = PosterPulse.BrandRed,
                inactiveTrackColor = PosterPulse.SurfaceRaised,
            ),
        )
        Text(
            text = "$elapsed / $duration",
            color = PosterPulse.TextSecondary,
            style = MaterialTheme.typography.labelLarge,
        )
        OutlinedButton(
            onClick = onClose,
            enabled = inputEnabled,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("Close")
        }
      }
    }
  }
}

private fun playbackTime(positionMs: Long): String {
  val seconds = positionMs.coerceAtLeast(0L) / 1000L
  return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
