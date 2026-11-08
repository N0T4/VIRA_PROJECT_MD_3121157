package com.meta.spatial.samples.startersample

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeCompletionPolicyTest {
  @Test
  fun previewNeverCountsAndFullEpisodeCountsOncePerRun() {
    assertFalse(EpisodeCompletionPolicy(isFullEpisode = false).naturalCompletion())

    val full = EpisodeCompletionPolicy(isFullEpisode = true)
    assertTrue(full.naturalCompletion())
    assertFalse(full.naturalCompletion())
  }

  @Test
  fun unfinishedSeekAndUnwatchedGapCannotCount() {
    val full = EpisodeCompletionPolicy(isFullEpisode = true)
    full.observePlayback(1_000)
    full.beginSeek()
    full.observePlayback(9_000)
    assertFalse(full.naturalCompletion())
    full.finishSeek(9_000)
    full.observePlayback(10_000)
    assertFalse(full.naturalCompletion())
  }

  @Test
  fun rewindingToWatchedPrefixAllowsCompletingTheMissingPart() {
    val full = EpisodeCompletionPolicy(isFullEpisode = true)
    full.observePlayback(3_000)
    full.beginSeek()
    full.finishSeek(9_000)
    full.observePlayback(10_000)
    full.beginSeek()
    full.finishSeek(2_000)
    full.observePlayback(10_000)
    assertTrue(full.naturalCompletion())
  }

  @Test
  fun replayStartsASeparateRunFromZero() {
    val full = EpisodeCompletionPolicy(isFullEpisode = true)
    full.observePlayback(1_000)
    full.beginSeek()
    full.finishSeek(9_000)
    assertFalse(full.naturalCompletion())
    full.replay()
    assertTrue(full.naturalCompletion())
  }
}
