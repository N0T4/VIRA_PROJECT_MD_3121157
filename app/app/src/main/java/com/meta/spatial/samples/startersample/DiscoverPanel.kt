package com.meta.spatial.samples.startersample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.spatial.samples.startersample.catalog.CatalogHome
import com.meta.spatial.samples.startersample.catalog.CatalogRepository
import com.meta.spatial.samples.startersample.catalog.CatalogTitle
import com.meta.spatial.samples.startersample.catalog.CatalogUnavailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun DiscoverPanel(
    repository: CatalogRepository,
    preservingSearchEditor: Boolean,
    onRecenter: () -> Unit,
    onClose: () -> Unit,
    onTitleSelected: (CatalogTitle) -> Unit,
    onSearchFocusChanged: (Boolean) -> Unit = {},
) {
  var searching by remember { mutableStateOf(false) }
  var criteria by remember { mutableStateOf(SearchCriteria()) }
  var home by remember(repository) { mutableStateOf<CatalogHome?>(null) }
  var homeFailed by remember(repository) { mutableStateOf(false) }
  var homeAttempt by remember(repository) { mutableIntStateOf(0) }
  val focusManager = LocalFocusManager.current
  val reportSearchFocus by rememberUpdatedState(onSearchFocusChanged)

  LaunchedEffect(preservingSearchEditor) {
    if (!preservingSearchEditor) focusManager.clearFocus(force = true)
  }

  LaunchedEffect(repository, homeAttempt) {
    homeFailed = false
    try {
      home = withContext(Dispatchers.Default) { repository.home() }
    } catch (_: CatalogUnavailableException) {
      homeFailed = true
    }
  }

  PosterPulseTheme {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(PosterPulse.PanelShape)
            .background(PosterPulse.Background)
            .border(1.dp, PosterPulse.Outline, PosterPulse.PanelShape)
            .padding(PosterPulse.Space12),
        verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
    ) {
      DiscoverNavigation(
          searching = searching,
          onDiscover = {
            focusManager.clearFocus(force = true)
            searching = false
          },
          onSearch = { searching = true },
          onRecenter = {
            focusManager.clearFocus(force = true)
            onRecenter()
          },
          onClose = {
            focusManager.clearFocus(force = true)
            onClose()
          },
      )
      Box(Modifier.weight(1f).fillMaxWidth()) {
        val currentHome = home
        when {
          currentHome == null && homeFailed -> CatalogStatus(
              title = "Demo catalog unavailable",
              detail = "Try loading the local collection again.",
              action = "Retry",
              onAction = { homeAttempt++ },
          )
          currentHome == null -> CatalogStatus(
              title = "Loading Discover",
              detail = "Reading the local demo catalog.",
              loading = true,
          )
          searching -> {
            DisposableEffect(Unit) {
              onDispose { reportSearchFocus(false) }
            }
            SearchCatalog(
                repository = repository,
                home = currentHome,
                criteria = criteria,
                onCriteriaChange = { criteria = it },
                onSearchFocusChanged = { reportSearchFocus(it) },
                onTitleSelected = onTitleSelected,
            )
          }
          else -> FeaturedDiscover(currentHome, onBrowse = { searching = true }, onTitleSelected)
        }
      }
      Text(
          text = "OFFLINE DEMO  /  FICTIONAL TITLES  /  PLACEHOLDER ART",
          color = PosterPulse.TextSecondary,
          fontSize = PosterPulse.MetadataSize,
          letterSpacing = 1.sp,
          maxLines = 1,
          modifier = Modifier.padding(start = PosterPulse.Space4),
      )
    }
  }
}

@Composable
private fun DiscoverNavigation(
    searching: Boolean,
    onDiscover: () -> Unit,
    onSearch: () -> Unit,
    onRecenter: () -> Unit,
    onClose: () -> Unit,
) {
  Row(
      modifier = Modifier
          .fillMaxWidth()
          .clip(PosterPulse.ControlShape)
          .background(PosterPulse.Surface)
          .border(1.dp, PosterPulse.Outline, PosterPulse.ControlShape)
          .padding(horizontal = PosterPulse.Space8),
      horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
        modifier = Modifier.padding(horizontal = PosterPulse.Space4),
        horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space4),
    ) {
      Text("VIRA", color = PosterPulse.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
      Text("//", color = PosterPulse.BrandRed, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
    CatalogButton("Discover", onDiscover, selected = !searching)
    CatalogButton("Search", onSearch, selected = searching)
    Box(Modifier.weight(1f))
    CatalogButton(stringResource(R.string.recenter_tools), onRecenter)
    CatalogButton(stringResource(R.string.close_tools), onClose)
  }
}

@Composable
internal fun CatalogButton(label: String, onClick: () -> Unit, selected: Boolean = false) {
  if (selected) {
    Button(
        onClick = onClick,
        shape = PosterPulse.ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = PosterPulse.BrandRed,
            contentColor = PosterPulse.Background,
        ),
        contentPadding = PaddingValues(horizontal = PosterPulse.Space12),
    ) {
      Text(label, fontSize = PosterPulse.MetadataSize, fontWeight = FontWeight.Bold)
    }
  } else {
    OutlinedButton(
        onClick = onClick,
        shape = PosterPulse.ControlShape,
        border = BorderStroke(1.dp, PosterPulse.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PosterPulse.TextPrimary),
        contentPadding = PaddingValues(horizontal = PosterPulse.Space12),
    ) {
      Text(label, fontSize = PosterPulse.MetadataSize, fontWeight = FontWeight.Medium)
    }
  }
}

@Composable
internal fun CatalogStatus(
    title: String,
    detail: String,
    loading: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {},
) {
  Column(
      modifier = Modifier
          .fillMaxSize()
          .clip(PosterPulse.PanelShape)
          .background(PosterPulse.Surface)
          .padding(PosterPulse.Space16),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8, Alignment.CenterVertically),
  ) {
    if (loading) CircularProgressIndicator(color = PosterPulse.BrandRed)
    Text(title, color = PosterPulse.TextPrimary, fontSize = PosterPulse.BodySize, fontWeight = FontWeight.Bold)
    Text(detail, color = PosterPulse.TextSecondary, fontSize = PosterPulse.MetadataSize)
    if (action != null) CatalogButton(action, onAction)
  }
}
