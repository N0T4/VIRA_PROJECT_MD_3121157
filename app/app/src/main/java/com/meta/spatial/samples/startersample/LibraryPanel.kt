package com.meta.spatial.samples.startersample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.spatial.samples.startersample.catalog.CatalogTitle
import com.meta.spatial.samples.startersample.library.LibraryEntry
import com.meta.spatial.samples.startersample.library.LibraryStatus

internal data class LibraryPanelRow(val entry: LibraryEntry, val title: CatalogTitle?)

@Composable
internal fun LibraryPanel(
    selectedStatus: LibraryStatus,
    rows: List<LibraryPanelRow>?,
    failed: Boolean,
    onSelectStatus: (LibraryStatus) -> Unit,
    onRetry: () -> Unit,
    onTitleSelected: (CatalogTitle) -> Unit,
    inputEnabled: Boolean = true,
) {
  PosterPulseTheme {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(PosterPulse.PanelShape)
            .background(PosterPulse.Background)
            .border(1.dp, PosterPulse.Outline, PosterPulse.PanelShape)
            .padding(PosterPulse.Space16),
        verticalArrangement = Arrangement.spacedBy(PosterPulse.Space12),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8)) {
          Text("LIBRARY", color = PosterPulse.TextPrimary, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black)
          Text("//", color = PosterPulse.BrandRed, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black)
        }
        Text("SAVED ON THIS DEVICE", color = PosterPulse.TextSecondary, fontSize = PosterPulse.MetadataSize)
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8)) {
        LibraryStatus.entries.forEach { status ->
          LibraryCategoryButton(
              status = status,
              isSelected = status == selectedStatus,
              enabled = inputEnabled,
              onClick = { onSelectStatus(status) },
              modifier = Modifier.weight(1f),
          )
        }
      }

      Box(Modifier.weight(1f).fillMaxWidth()) {
        when {
          failed -> CatalogStatus(
              title = "Could not open Library",
              detail = "Try loading the saved titles again.",
              action = if (inputEnabled) "Retry" else null,
              onAction = onRetry,
          )
          rows == null -> CatalogStatus(
              title = "Loading Library",
              detail = "Reading the titles saved on this device.",
              loading = true,
          )
          rows.isEmpty() -> CatalogStatus(
              title = "No titles in ${selectedStatus.name}",
              detail = "Use a title’s Anime Card to add it or change its status.",
          )
          else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8)) {
            Text(
                if (rows.size == 1) "1 saved title" else "${rows.size} saved titles",
                color = PosterPulse.TextSecondary,
                fontSize = PosterPulse.MetadataSize,
            )
            key(selectedStatus) {
              LazyColumn(
                  modifier = Modifier.fillMaxSize(),
                  verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
                  userScrollEnabled = inputEnabled,
              ) {
                items(rows, key = { it.entry.titleId }) { row ->
                  SavedTitleRow(row, inputEnabled, onTitleSelected)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LibraryCategoryButton(
    status: LibraryStatus,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
  val accent = status.libraryAccent
  OutlinedButton(
      onClick = onClick,
      enabled = enabled && !isSelected,
      modifier = modifier.heightIn(min = 60.dp).semantics { selected = isSelected },
      shape = PosterPulse.ControlShape,
      border = BorderStroke(1.dp, if (isSelected) accent else PosterPulse.Outline),
      colors = ButtonDefaults.outlinedButtonColors(
          disabledContainerColor = if (isSelected) accent.copy(alpha = 0.13f) else Color.Transparent,
      ),
      contentPadding = PaddingValues(horizontal = PosterPulse.Space8, vertical = PosterPulse.Space8),
  ) {
    Box(Modifier.size(width = 4.dp, height = 18.dp).background(accent))
    Text(
        status.name,
        modifier = Modifier.padding(start = PosterPulse.Space8),
        color = if (enabled || isSelected) accent else PosterPulse.TextSecondary,
        fontSize = 20.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
    )
  }
}

@Composable
private fun SavedTitleRow(
    row: LibraryPanelRow,
    inputEnabled: Boolean,
    onTitleSelected: (CatalogTitle) -> Unit,
) {
  val title = row.title
  Row(
      modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 90.dp)
          .clip(PosterPulse.ControlShape)
          .background(PosterPulse.Surface)
          .border(1.dp, PosterPulse.Outline, PosterPulse.ControlShape)
          .clickable(enabled = inputEnabled && title != null, role = Role.Button) {
            if (title != null) onTitleSelected(title)
          }
          .padding(PosterPulse.Space8),
      horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
        modifier = Modifier.size(width = 88.dp, height = 66.dp)
            .clip(PosterPulse.ControlShape)
            .background(PosterPulse.SurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
      if (title?.hasDemoPoster == true) {
        CatalogArtwork(title)
      } else {
        Text("NO POSTER", color = PosterPulse.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PosterPulse.Space4)) {
      Text(
          title?.title ?: "Title details unavailable",
          color = PosterPulse.TextPrimary,
          fontSize = 23.sp,
          lineHeight = 25.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
      )
      Text(
          if (title != null) "${title.year}  ·  ${title.format}  ·  ${title.genre}" else "Saved entry",
          color = PosterPulse.TextSecondary,
          fontSize = PosterPulse.MetadataSize,
      )
    }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(PosterPulse.Space4)) {
      Text("${row.entry.progress} watched", color = PosterPulse.TextPrimary, fontSize = 18.sp)
      if (title != null) {
        Text(
            "OPEN CARD  ›",
            color = if (inputEnabled) PosterPulse.FocusEdge else PosterPulse.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

private val LibraryStatus.libraryAccent: Color
  get() = when (this) {
    LibraryStatus.Planning -> Color(0xFFFFE500)
    LibraryStatus.Watching -> Color(0xFF73D9E7)
    LibraryStatus.Completed -> PosterPulse.TextPrimary
  }
