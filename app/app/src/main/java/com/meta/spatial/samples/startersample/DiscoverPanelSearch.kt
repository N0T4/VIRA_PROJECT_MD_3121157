package com.meta.spatial.samples.startersample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.meta.spatial.samples.startersample.catalog.CatalogHome
import com.meta.spatial.samples.startersample.catalog.CatalogRepository
import com.meta.spatial.samples.startersample.catalog.CatalogSort
import com.meta.spatial.samples.startersample.catalog.CatalogTitle
import com.meta.spatial.samples.startersample.catalog.CatalogUnavailableException
import com.meta.spatial.uiset.input.SpatialSearchBar
import com.meta.spatial.uiset.theme.SpatialTheme
import com.meta.spatial.uiset.theme.darkSpatialColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal data class SearchCriteria(
    val query: String = "",
    val genre: String? = null,
    val year: Int? = null,
    val format: String? = null,
    val sort: CatalogSort = CatalogSort.Recommended,
)

private data class SearchResult(
    val titles: List<CatalogTitle> = emptyList(),
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val failed: Boolean = false,
)

private enum class Filter { Genre, Year, Format, Sort }

@Composable
internal fun SearchCatalog(
    repository: CatalogRepository,
    home: CatalogHome,
    criteria: SearchCriteria,
    onCriteriaChange: (SearchCriteria) -> Unit,
    onSearchFocusChanged: (Boolean) -> Unit,
    onTitleSelected: (CatalogTitle) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  var openFilter by remember { mutableStateOf<Filter?>(null) }
  var result by remember(repository, criteria) { mutableStateOf(SearchResult()) }
  var page by remember(repository, criteria) { mutableIntStateOf(0) }
  var attempt by remember(repository, criteria) { mutableIntStateOf(0) }

  // Results belong to a single query/filter key. Replacing it also cancels the old effect.
  LaunchedEffect(repository, criteria, page, attempt) {
    result = result.copy(loading = true, failed = false)
    if (page == 0 && criteria.query.isNotBlank()) delay(180)
    try {
      val response = withContext(Dispatchers.Default) {
        repository.search(
            query = criteria.query,
            genre = criteria.genre,
            year = criteria.year,
            format = criteria.format,
            page = page,
            sort = criteria.sort,
        )
      }
      result = SearchResult(
          titles = if (page == 0) response.items else result.titles + response.items,
          hasMore = response.hasMore,
          loading = false,
      )
    } catch (_: CatalogUnavailableException) {
      result = result.copy(loading = false, failed = true)
    }
  }

  fun resetSearch() {
    focusManager.clearFocus(force = true)
    openFilter = null
    onCriteriaChange(SearchCriteria())
  }

  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
  ) {
    SpatialTheme(colorScheme = darkSpatialColorScheme()) {
      SpatialSearchBar(
          query = criteria.query,
          onQueryChange = { onCriteriaChange(criteria.copy(query = it)) },
          onQuerySubmit = { focusManager.clearFocus(force = true) },
          placeholder = "Search demo titles",
          modifier = Modifier.fillMaxWidth().onFocusChanged { onSearchFocusChanged(it.hasFocus) },
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8)) {
      fun select(filter: Filter) {
        focusManager.clearFocus(force = true)
        openFilter = if (openFilter == filter) null else filter
      }
      CatalogButton(criteria.genre ?: "Genre", { select(Filter.Genre) }, openFilter == Filter.Genre)
      CatalogButton(criteria.year?.toString() ?: "Year", { select(Filter.Year) }, openFilter == Filter.Year)
      CatalogButton(criteria.format ?: "Format", { select(Filter.Format) }, openFilter == Filter.Format)
      CatalogButton(
          if (criteria.sort == CatalogSort.Recommended) "Sort" else criteria.sort.name,
          { select(Filter.Sort) },
          openFilter == Filter.Sort,
      )
      if (criteria != SearchCriteria()) CatalogButton("Reset", ::resetSearch)
    }
    val filter = openFilter
    if (filter != null) {
      key(filter) {
        FilterOptions(
            filter = filter,
            home = home,
            criteria = criteria,
            onChoose = {
              onCriteriaChange(it)
              openFilter = null
            },
        )
      }
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
      when {
        result.titles.isNotEmpty() -> SearchGrid(
            criteria = criteria,
            result = result,
            onTitleSelected = onTitleSelected,
            onLoadMore = {
              if (!result.loading && !result.failed && result.hasMore) {
                result = result.copy(loading = true)
                page++
              }
            },
            onRetry = {
              if (!result.loading) {
                result = result.copy(loading = true, failed = false)
                attempt++
              }
            },
        )
        result.loading -> CatalogStatus(
            title = "Searching",
            detail = "Looking through the local demo catalog.",
            loading = true,
        )
        result.failed -> CatalogStatus(
            title = "Search unavailable",
            detail = "The local results could not be loaded.",
            action = "Retry",
            onAction = { attempt++ },
        )
        else -> CatalogStatus(
            title = "No matching titles",
            detail = "Try a different search or reset the filters.",
            action = "Reset search",
            onAction = ::resetSearch,
        )
      }
    }
  }
}

@Composable
private fun FilterOptions(
    filter: Filter,
    home: CatalogHome,
    criteria: SearchCriteria,
    onChoose: (SearchCriteria) -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
  ) {
    when (filter) {
      Filter.Genre -> {
        CatalogButton("All genres", { onChoose(criteria.copy(genre = null)) }, criteria.genre == null)
        for (genre in home.genres) {
          CatalogButton(genre, { onChoose(criteria.copy(genre = genre)) }, criteria.genre == genre)
        }
      }
      Filter.Year -> {
        CatalogButton("All years", { onChoose(criteria.copy(year = null)) }, criteria.year == null)
        for (year in home.years) {
          CatalogButton(year.toString(), { onChoose(criteria.copy(year = year)) }, criteria.year == year)
        }
      }
      Filter.Format -> {
        CatalogButton("All formats", { onChoose(criteria.copy(format = null)) }, criteria.format == null)
        for (format in home.formats) {
          CatalogButton(format, { onChoose(criteria.copy(format = format)) }, criteria.format == format)
        }
      }
      Filter.Sort -> {
        for (sort in CatalogSort.entries) {
          CatalogButton(sort.name, { onChoose(criteria.copy(sort = sort)) }, criteria.sort == sort)
        }
      }
    }
  }
}

@Composable
private fun SearchGrid(
    criteria: SearchCriteria,
    result: SearchResult,
    onTitleSelected: (CatalogTitle) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
  key(criteria) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(185.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
        verticalArrangement = Arrangement.spacedBy(PosterPulse.Space8),
    ) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        val countLabel = if (result.titles.size == 1) "result" else "results"
        Text(
            text = "${result.titles.size} $countLabel shown",
            color = PosterPulse.TextSecondary,
            fontSize = PosterPulse.MetadataSize,
        )
      }
      items(result.titles, key = { it.id }) { title ->
        CatalogTile(title, Modifier.height(118.dp).clickable { onTitleSelected(title) }, showMetadata = true)
      }
      item(span = { GridItemSpan(maxLineSpan) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosterPulse.Space8, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          when {
            result.loading -> Text("Loading more titles…", color = PosterPulse.TextSecondary)
            result.failed -> {
              Text("Could not load the next page", color = PosterPulse.Error)
              CatalogButton("Retry", onRetry)
            }
            result.hasMore -> CatalogButton("Load more", onLoadMore)
          }
        }
      }
    }
  }
}
