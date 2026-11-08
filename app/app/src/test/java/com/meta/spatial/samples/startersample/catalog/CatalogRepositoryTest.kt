package com.meta.spatial.samples.startersample.catalog

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryTest {
  @Test
  fun homeKeepsTheHeroSeparateAndIncludesAMissingPoster() {
    val home = CatalogRepository().home()

    assertEquals("lantern-observatory", home.featured.id)
    assertFalse(home.featured in home.tiles)
    assertTrue(home.tiles.any { !it.hasDemoPoster })
    assertTrue(home.tiles.any { it.inDemoLibrary })
    assertEquals(listOf("Film", "Series", "Short"), home.formats)
  }

  @Test
  fun equivalentQueriesShareAKeyWithNormalizedTextAndFilters() {
    val repository = CatalogRepository()

    val first = repository.search(" CAFÉ--beneath ", genre = " Fantasy ", year = 2023, format = "FILM")
    val second = repository.search("cafe beneath", genre = "fantasy", year = 2023, format = "film")

    assertEquals(listOf("two-moon-cafe"), first.items.map { it.id })
    assertEquals(first, second)
    assertEquals(1, repository.cachedSearchCount)
  }

  @Test
  fun incompatibleFiltersProduceAnEmptyPage() {
    val page = CatalogRepository().search("cafe", genre = "Fantasy", year = 2026)

    assertTrue(page.items.isEmpty())
    assertFalse(page.hasMore)
  }

  @Test
  fun sortingAppliesBeforePagingAndDoesNotReuseRecommendedResults() {
    val repository = CatalogRepository()
    val recommended = repository.search("")
    val pages = (0..2).map { repository.search("", page = it, sort = CatalogSort.Newest) }
    val titles = pages.flatMap { it.items }

    assertEquals("lantern-observatory", recommended.items.first().id)
    assertEquals(listOf(true, true, false), pages.map { it.hasMore })
    assertEquals(18, titles.map { it.id }.distinct().size)
    assertEquals(titles.map { it.year }.sortedDescending(), titles.map { it.year })
    assertEquals(2, repository.cachedSearchCount)
    assertEquals(CatalogPage(emptyList(), false), repository.search("", page = Int.MAX_VALUE))
  }

  @Test
  fun searchCacheStaysBoundedAndRetainsTheLoadedCatalog() {
    var loads = 0
    val repository = CatalogRepository(source = {
      loads++
      demoCatalog
    })

    repeat(50) { repository.search("search $it") }

    assertEquals(24, repository.cachedSearchCount)
    assertEquals(1, loads)
    assertEquals(listOf("northlight-archive"), repository.search("northlight").items.map { it.id })
    assertEquals(24, repository.cachedSearchCount)
  }

  @Test
  fun retryAfterLocalLoadFailureDoesNotCacheTheFailure() {
    var attempts = 0
    val repository = CatalogRepository(source = {
      if (++attempts == 1) throw IOException("Demo fixture read failed")
      demoCatalog
    })

    assertThrows(CatalogUnavailableException::class.java) { repository.search("northlight") }
    assertEquals(0, repository.cachedSearchCount)
    assertEquals(listOf("northlight-archive"), repository.search("northlight").items.map { it.id })
    assertEquals(1, repository.cachedSearchCount)
    assertEquals(2, attempts)
  }
}
