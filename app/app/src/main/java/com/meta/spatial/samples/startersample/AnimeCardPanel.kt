package com.meta.spatial.samples.startersample

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.spatial.samples.startersample.catalog.CatalogEpisode
import com.meta.spatial.samples.startersample.catalog.CatalogTitle
import com.meta.spatial.samples.startersample.library.LibraryEntry
import com.meta.spatial.samples.startersample.library.LibraryStatus

@Composable
internal fun AnimeCardCover(
    title: CatalogTitle,
    expanded: Boolean,
    gripHeld: Boolean,
    actionGeneration: Int,
    onToggle: () -> Unit,
    onClose: () -> Unit,
) {
  AnimeCardLeaf(Alignment.CenterEnd) {
    Column(
        modifier = Modifier.fillMaxSize().traceCardMotion().padding(PosterPulse.Space12),
        verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("ANIME CARD", color = PosterPulse.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(
            if (gripHeld) "RELEASE GRIP" else "GRAB RED BAR",
            color = PosterPulse.TextSecondary,
            fontSize = 12.sp,
        )
      }
      CatalogTile(title, modifier = Modifier.fillMaxWidth().weight(1f), showMetadata = true)
      Text("PROTOTYPE  /  DEMO CATALOG", color = PosterPulse.TextSecondary, fontSize = 12.sp)
      key(actionGeneration) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
        ) {
          Button(onClick = onToggle, enabled = !gripHeld, modifier = Modifier.weight(1f).traceCardBounds("fold")) {
            Text(if (expanded) "Fold" else "Unfold", fontWeight = FontWeight.Bold)
          }
          OutlinedButton(onClick = onClose, enabled = !gripHeld, modifier = Modifier.traceCardBounds("close")) {
            Text("Close", fontSize = 15.sp)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.traceCardMotion(): Modifier = if (BuildConfig.DEBUG) {
  motionEventSpy { event ->
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
        Log.i("ViraCard", "cover_motion action=${event.actionMasked} x=${event.x} y=${event.y}")
    }
  }
} else this

private fun Modifier.traceCardBounds(action: String): Modifier = if (BuildConfig.DEBUG) {
  onGloballyPositioned { Log.i("ViraCard", "cover_bounds action=$action ${it.boundsInRoot()}") }
} else this

@Composable
internal fun AnimeCardInside(
    title: CatalogTitle,
    episodes: List<CatalogEpisode>,
    selectedEpisodeId: String?,
    gripHeld: Boolean,
    actionGeneration: Int,
    onEpisodeSelected: (String) -> Unit,
    onPlayDemo: (() -> Unit)?,
    libraryEntry: LibraryEntry?,
    libraryLoading: Boolean,
    libraryBusy: Boolean,
    libraryError: String?,
    onLibraryAdd: () -> Unit,
    onLibraryStatus: (LibraryStatus) -> Unit,
    onLibraryAdjustProgress: (Int) -> Unit,
    onLibraryRating: (Int?) -> Unit,
    onLibraryRemove: () -> Unit,
    onLibraryRetry: () -> Unit,
) {
  val scroll = rememberScrollState()
  val selection = episodes.firstOrNull { it.id == selectedEpisodeId }
  val fullEpisodeVideo = selection?.localVideo?.isFullEpisode == true
  var showingLibrary by remember(title.id) { mutableStateOf(false) }
  AnimeCardLeaf(Alignment.CenterStart) {
    Column(
        modifier = Modifier.fillMaxSize().padding(PosterPulse.Space12),
        verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            if (showingLibrary) "//  LOCAL LIBRARY" else "//  TITLE DETAILS",
            color = PosterPulse.FocusEdge,
            fontSize = PosterPulse.MetadataSize,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
        )
        key(actionGeneration) {
          OutlinedButton(
              onClick = { showingLibrary = !showingLibrary },
              enabled = !gripHeld,
              contentPadding = PaddingValues(horizontal = PosterPulse.Space12, vertical = PosterPulse.Space4),
          ) {
            Text(if (showingLibrary) "Details" else "Library", fontSize = 13.sp)
          }
        }
      }
      if (showingLibrary) {
        LibraryCardSection(
            entry = libraryEntry,
            loading = libraryLoading,
            busy = libraryBusy,
            error = libraryError,
            gripHeld = gripHeld,
            actionGeneration = actionGeneration,
            onAdd = onLibraryAdd,
            onStatus = onLibraryStatus,
            onAdjustProgress = onLibraryAdjustProgress,
            onRating = onLibraryRating,
            onRemove = onLibraryRemove,
            onRetry = onLibraryRetry,
        )
        return@Column
      }
      Column(
          modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .verticalScroll(scroll, enabled = !gripHeld),
          verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
      ) {
        Text(
            title.title,
            color = PosterPulse.TextPrimary,
            fontSize = 23.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${title.year}  ·  ${title.format}  ·  ${title.genre}",
            color = PosterPulse.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )
        Text(title.synopsis, color = PosterPulse.TextPrimary, fontSize = 16.sp, lineHeight = 21.sp)
        HorizontalDivider(color = PosterPulse.Outline)
        Text(
            "EPISODES",
            color = PosterPulse.FocusEdge,
            fontSize = PosterPulse.MetadataSize,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
        )
        if (episodes.isEmpty()) {
          Text("No episodes listed for this demo title.", color = PosterPulse.TextSecondary, fontSize = 15.sp)
        }
        key(actionGeneration) {
          episodes.forEach { episode ->
            key(episode.id) {
              AnimeEpisodeButton(episode, episode.id == selectedEpisodeId, gripHeld) {
                onEpisodeSelected(episode.id)
              }
            }
          }
        }
      }
      HorizontalDivider(color = PosterPulse.Outline)
      Text(
          if (selection == null) "No episode selected" else "Selected: ${selection.label}",
          color = PosterPulse.TextPrimary,
          fontSize = 14.sp,
          lineHeight = 17.sp,
          fontWeight = FontWeight.Bold,
      )
      if (selection?.hasLocalVideo == true && onPlayDemo != null) {
        key(actionGeneration) {
          Button(
              onClick = onPlayDemo,
              enabled = !gripHeld,
              modifier = Modifier.fillMaxWidth(),
              shape = PosterPulse.ControlShape,
          ) {
            Text(if (fullEpisodeVideo) "Play episode" else "Play demo video")
          }
        }
      } else {
        Text(
            when {
              fullEpisodeVideo -> "EPISODE VIDEO OPEN"
              selection?.hasLocalVideo == true -> "DEMO VIDEO OPEN"
              else -> "DEMO CATALOG · NO VIDEO SOURCE"
            },
            color = PosterPulse.TextSecondary,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
      }
    }
  }
}

@Composable
private fun LibraryCardSection(
    entry: LibraryEntry?,
    loading: Boolean,
    busy: Boolean,
    error: String?,
    gripHeld: Boolean,
    actionGeneration: Int,
    onAdd: () -> Unit,
    onStatus: (LibraryStatus) -> Unit,
    onAdjustProgress: (Int) -> Unit,
    onRating: (Int?) -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState(), enabled = !gripHeld),
      verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
  ) {
    Text("Saved on this device", color = PosterPulse.TextSecondary, fontSize = 13.sp)
    val enabled = !gripHeld && !busy && !loading
    key(actionGeneration) {
      when {
        loading -> Text("Loading Library…", color = PosterPulse.TextPrimary, fontSize = 15.sp)
        entry == null && error != null -> OutlinedButton(onClick = onRetry, enabled = enabled) {
          Text("Retry", fontWeight = FontWeight.Bold)
        }
        entry == null -> Button(onClick = onAdd, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
          Text("Add to Library", fontWeight = FontWeight.Bold)
        }
        else -> LibraryEntryControls(entry, enabled, onStatus, onAdjustProgress, onRating, onRemove)
      }
    }
    if (busy) Text("Saving…", color = PosterPulse.TextSecondary, fontSize = 13.sp)
    if (error != null) Text(error, color = PosterPulse.Error, fontSize = 13.sp)
  }
}

@Composable
private fun LibraryEntryControls(
    entry: LibraryEntry,
    enabled: Boolean,
    onStatus: (LibraryStatus) -> Unit,
    onAdjustProgress: (Int) -> Unit,
    onRating: (Int?) -> Unit,
    onRemove: () -> Unit,
) {
  var confirmingRemoval by remember(entry.titleId) { mutableStateOf(false) }
  Text("STATUS", color = PosterPulse.FocusEdge, fontSize = 13.sp, fontWeight = FontWeight.Bold)
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space4)) {
    LibraryStatus.entries.forEach { status ->
      val isSelected = status == entry.status
      val color = when (status) {
        LibraryStatus.Planning -> Color(0xFFFFE500)
        LibraryStatus.Watching -> Color(0xFF73D9E7)
        LibraryStatus.Completed -> PosterPulse.TextPrimary
      }
      OutlinedButton(
          onClick = { onStatus(status) },
          enabled = enabled && !isSelected,
          modifier = Modifier.weight(1f).semantics { selected = isSelected },
          shape = PosterPulse.ControlShape,
          border = BorderStroke(1.dp, if (isSelected) color else PosterPulse.Outline),
          colors = ButtonDefaults.outlinedButtonColors(disabledContainerColor = if (isSelected) {
            color.copy(alpha = 0.13f)
          } else {
            Color.Transparent
          }),
          contentPadding = PaddingValues(horizontal = PosterPulse.Space4, vertical = PosterPulse.Space8),
      ) {
        Text(
            status.name,
            color = if (isSelected) color else PosterPulse.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
      }
    }
  }
  Text("WATCHED EPISODES", color = PosterPulse.FocusEdge, fontSize = 13.sp, fontWeight = FontWeight.Bold)
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    ProgressButton("−", "Decrease watched episodes", enabled && entry.progress > 0) {
      onAdjustProgress(-1)
    }
    Text(entry.progress.toString(), color = PosterPulse.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    ProgressButton("+", "Increase watched episodes", enabled && entry.progress < Int.MAX_VALUE) {
      onAdjustProgress(1)
    }
  }
  LibraryRatingControls(entry.rating, enabled, onRating)
  if (confirmingRemoval) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8)) {
      OutlinedButton(onClick = { confirmingRemoval = false }, enabled = enabled, modifier = Modifier.weight(1f)) {
        Text("Cancel", fontSize = 13.sp)
      }
      OutlinedButton(onClick = onRemove, enabled = enabled, modifier = Modifier.weight(1f)) {
        Text("Remove", color = PosterPulse.Error, fontSize = 13.sp)
      }
    }
  } else {
    OutlinedButton(
        onClick = { confirmingRemoval = true },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Remove from Library", fontSize = 13.sp)
    }
  }
}

@Composable
private fun LibraryRatingControls(rating: Int?, enabled: Boolean, onRating: (Int?) -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column {
      Text("PERSONAL RATING", color = PosterPulse.FocusEdge, fontSize = 13.sp, fontWeight = FontWeight.Bold)
      Text(
          if (rating == null) "Unrated" else "$rating / 10",
          color = PosterPulse.TextPrimary,
          fontSize = 15.sp,
      )
    }
    TextButton(
        onClick = { onRating(null) },
        enabled = enabled && rating != null,
        modifier = Modifier.semantics { contentDescription = "Clear personal rating" },
    ) {
      Text("Clear", fontSize = 13.sp)
    }
  }
  for (values in listOf(1..5, 6..10)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space4)) {
      for (value in values) {
        val isSelected = rating == value
        OutlinedButton(
            onClick = { onRating(value) },
            enabled = enabled && !isSelected,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { selected = isSelected },
            shape = PosterPulse.ControlShape,
            border = BorderStroke(1.dp, if (isSelected) PosterPulse.FocusEdge else PosterPulse.Outline),
            colors = ButtonDefaults.outlinedButtonColors(
                disabledContainerColor = if (isSelected) PosterPulse.BrandRed.copy(alpha = 0.18f) else Color.Transparent,
            ),
            contentPadding = PaddingValues(0.dp),
        ) {
          Text(
              value.toString(),
              color = if (isSelected) PosterPulse.FocusEdge else PosterPulse.TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun ProgressButton(label: String, description: String, enabled: Boolean, onClick: () -> Unit) {
  OutlinedButton(
      onClick = onClick,
      enabled = enabled,
      modifier = Modifier.widthIn(min = 48.dp).semantics { contentDescription = description },
      contentPadding = PaddingValues(horizontal = PosterPulse.Space12, vertical = PosterPulse.Space4),
  ) {
    Text(label, fontSize = 22.sp)
  }
}

@Composable
private fun AnimeEpisodeButton(
    episode: CatalogEpisode,
    isSelected: Boolean,
    gripHeld: Boolean,
    onClick: () -> Unit,
) {
  val status = when {
    !episode.isSelectable -> "UNAVAILABLE"
    isSelected -> "SELECTED"
    else -> ""
  }
  val background = if (isSelected) PosterPulse.BrandRed.copy(alpha = 0.18f) else PosterPulse.SurfaceRaised
  val outline = if (isSelected) PosterPulse.FocusEdge else PosterPulse.Outline
  OutlinedButton(
      onClick = onClick,
      enabled = episode.isSelectable && !gripHeld,
      modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics { selected = isSelected },
      shape = PosterPulse.ControlShape,
      border = BorderStroke(1.dp, outline),
      colors = ButtonDefaults.outlinedButtonColors(
          containerColor = background,
          disabledContainerColor = background,
      ),
      contentPadding = PaddingValues(horizontal = PosterPulse.Space12, vertical = PosterPulse.Space8),
  ) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PosterPulse.Space4)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            episode.label,
            color = PosterPulse.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        if (status.isNotEmpty()) {
          Text(
              status,
              color = if (isSelected) PosterPulse.FocusEdge else PosterPulse.TextSecondary,
              fontSize = 12.sp,
              lineHeight = 15.sp,
              fontWeight = FontWeight.Bold,
          )
        }
      }
      Text(
          episode.title,
          color = if (episode.isSelectable && !gripHeld) PosterPulse.TextPrimary else PosterPulse.TextSecondary,
          fontSize = 15.sp,
          lineHeight = 18.sp,
      )
    }
  }
}

@Composable
private fun AnimeCardLeaf(spineAlignment: Alignment, content: @Composable () -> Unit) {
  PosterPulseTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(PosterPulse.ControlShape)
            .background(PosterPulse.Surface)
            .border(1.dp, PosterPulse.Outline, PosterPulse.ControlShape),
    ) {
      Box(
          Modifier.align(spineAlignment).fillMaxHeight().width(4.dp).background(PosterPulse.BrandRed),
      )
      content()
    }
  }
}
