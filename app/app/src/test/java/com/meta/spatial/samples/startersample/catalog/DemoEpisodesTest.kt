package com.meta.spatial.samples.startersample.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoEpisodesTest {
  @Test
  fun episodeIdsAreStableAndUniqueAcrossTitles() {
    val allEpisodes = demoCatalog.flatMap(::demoEpisodesFor)
    assertEquals(allEpisodes, demoCatalog.flatMap(::demoEpisodesFor))
    assertEquals(allEpisodes.size, allEpisodes.map { it.id }.distinct().size)
    assertEquals(listOf("demo:lantern-observatory:s1e1"), allEpisodes.filter { it.hasLocalVideo }.map { it.id })
    assertEquals(false, allEpisodes.single { it.hasLocalVideo }.localVideo?.isFullEpisode)
  }

  @Test
  fun demoEntriesRespectTitleFormatAndSelectionAvailability() {
    val series = demoEpisodesFor(demoCatalog.first { it.format == "Series" })
    assertEquals(listOf("S1 · E1", "S1 · E2", "S1 · E3", "S1 · E4"), series.map { it.label })
    assertTrue(series.take(3).all { it.isSelectable })
    assertFalse(series.last().isSelectable)

    val feature = demoEpisodesFor(demoCatalog.first { it.format == "Film" }).single()
    val short = demoEpisodesFor(demoCatalog.first { it.format == "Short" }).single()
    assertEquals("Feature", feature.label)
    assertEquals("Short", short.label)
    assertTrue(feature.isSelectable && short.isSelectable)
  }
}
