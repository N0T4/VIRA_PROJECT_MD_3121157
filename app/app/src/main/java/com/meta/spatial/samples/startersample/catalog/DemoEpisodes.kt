package com.meta.spatial.samples.startersample.catalog

import androidx.annotation.RawRes
import com.meta.spatial.samples.startersample.R

internal data class LocalEpisodeVideo(@RawRes val resourceId: Int, val isFullEpisode: Boolean)

internal data class CatalogEpisode(
    val id: String,
    val label: String,
    val title: String,
    val isSelectable: Boolean = true,
    val localVideo: LocalEpisodeVideo? = null,
) {
  val hasLocalVideo: Boolean
    get() = localVideo != null
}

// Local selection fixtures. One episode has a bundled test clip.
internal fun demoEpisodesFor(title: CatalogTitle): List<CatalogEpisode> = when (title.format) {
  "Series" -> {
    val names = when (title.id) {
      "lantern-observatory" -> listOf(
          "A Light on the Rooftop", "Lanterns Over the Viaduct", "Beyond the Weather Dial",
      )
      "northlight-archive" -> listOf(
          "Static at Dawn", "The Weather Index", "A Signal for Tomorrow",
      )
      else -> listOf("Episode 1", "Episode 2", "Episode 3")
    }
    names.mapIndexed { index, name ->
      val number = index + 1
      val video = if (title.id == "lantern-observatory" && number == 1) {
        LocalEpisodeVideo(R.raw.vira_playback_demo, isFullEpisode = false)
      } else {
        null
      }
      CatalogEpisode("demo:${title.id}:s1e$number", "S1 · E$number", name, localVideo = video)
    } + CatalogEpisode("demo:${title.id}:s1e4", "S1 · E4", "Episode 4", isSelectable = false)
  }
  "Film" -> listOf(CatalogEpisode("demo:${title.id}:feature", "Feature", title.title))
  "Short" -> listOf(CatalogEpisode("demo:${title.id}:short", "Short", title.title))
  else -> emptyList()
}
