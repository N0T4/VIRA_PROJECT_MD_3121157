package com.meta.spatial.samples.startersample.catalog

import java.io.IOException
import java.text.Normalizer
import java.util.Locale

internal data class CatalogTitle(
    val id: String,
    val title: String,
    val synopsis: String,
    val genre: String,
    val year: Int,
    val format: String,
    val hasDemoPoster: Boolean = true,
    val inDemoLibrary: Boolean = false,
)

internal data class CatalogHome(
    val featured: CatalogTitle,
    val tiles: List<CatalogTitle>,
    val genres: List<String>,
    val years: List<Int>,
    val formats: List<String>,
)

internal data class CatalogPage(val items: List<CatalogTitle>, val hasMore: Boolean)

internal enum class CatalogSort { Recommended, Newest, Title }

internal class CatalogUnavailableException(cause: Throwable? = null) :
    IllegalStateException("Local catalog unavailable", cause)

private const val PAGE_SIZE = 6
private const val SEARCH_CACHE_LIMIT = 24
private val combiningMarks = Regex("\\p{M}+")
private val separators = Regex("[^\\p{L}\\p{N}]+")

internal class CatalogRepository(
    private val source: () -> List<CatalogTitle> = { demoCatalog },
) {
  private var titles: List<CatalogTitle>? = null
  private val matchesBySearch = object : LinkedHashMap<SearchKey, List<CatalogTitle>>(
      SEARCH_CACHE_LIMIT,
      0.75f,
      true,
  ) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SearchKey, List<CatalogTitle>>?) =
        size > SEARCH_CACHE_LIMIT
  }

  @get:Synchronized
  internal val cachedSearchCount: Int
    get() = matchesBySearch.size

  @Synchronized
  fun home(): CatalogHome {
    val catalog = catalog()
    return CatalogHome(
        featured = catalog.first(),
        tiles = catalog.drop(1).take(PAGE_SIZE),
        genres = catalog.map { it.genre }.distinct().sorted(),
        years = catalog.map { it.year }.distinct().sortedDescending(),
        formats = catalog.map { it.format }.distinct().sorted(),
    )
  }

  @Synchronized
  fun findById(id: String): CatalogTitle? = catalog().firstOrNull { it.id == id }

  @Synchronized
  fun search(
      query: String,
      genre: String? = null,
      year: Int? = null,
      format: String? = null,
      page: Int = 0,
      sort: CatalogSort = CatalogSort.Recommended,
  ): CatalogPage {
    require(page >= 0) { "Page must be zero or greater" }
    val key = SearchKey(
        query = normalized(query),
        genre = genre?.let(::normalized)?.takeIf { it.isNotEmpty() },
        year = year,
        format = format?.let(::normalized)?.takeIf { it.isNotEmpty() },
        sort = sort,
    )
    val matches = matchesBySearch.getOrPut(key) {
      val terms = key.query.split(' ').filter { it.isNotEmpty() }
      val filtered = catalog().filter { title ->
        (key.genre == null || normalized(title.genre) == key.genre) &&
            (key.year == null || title.year == key.year) &&
            (key.format == null || normalized(title.format) == key.format) &&
            (terms.isEmpty() || normalized(
                "${title.title} ${title.synopsis} ${title.genre} ${title.year} ${title.format}",
            ).let { text -> terms.all(text::contains) })
      }
      when (key.sort) {
        CatalogSort.Recommended -> filtered
        CatalogSort.Newest -> filtered.sortedWith(
            compareByDescending<CatalogTitle> { it.year }.thenBy { normalized(it.title) },
        )
        CatalogSort.Title -> filtered.sortedBy { normalized(it.title) }
      }
    }
    val offset = page.toLong() * PAGE_SIZE
    if (offset >= matches.size) return CatalogPage(emptyList(), hasMore = false)
    val start = offset.toInt()
    val end = minOf(offset + PAGE_SIZE, matches.size.toLong()).toInt()
    return CatalogPage(matches.subList(start, end).toList(), hasMore = end < matches.size)
  }

  private fun catalog(): List<CatalogTitle> {
    titles?.let { return it }
    val loaded = try {
      source().toList()
    } catch (failure: IOException) {
      throw CatalogUnavailableException(failure)
    }
    if (loaded.isEmpty()) throw CatalogUnavailableException()
    titles = loaded
    return loaded
  }

  private data class SearchKey(
      val query: String,
      val genre: String?,
      val year: Int?,
      val format: String?,
      val sort: CatalogSort,
  )
}

private fun normalized(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .replace(separators, " ")
        .trim()
