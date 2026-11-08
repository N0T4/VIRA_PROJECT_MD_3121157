package com.meta.spatial.samples.startersample

/** Tracks the continuously watched prefix of one playback run. */
internal class EpisodeCompletionPolicy(private val isFullEpisode: Boolean) {
  private var watchedThroughMs = 0
  private var connected = true
  private var seeking = false
  private var completed = false

  fun observePlayback(positionMs: Int) {
    if (connected && !seeking) watchedThroughMs = maxOf(watchedThroughMs, positionMs)
  }

  fun beginSeek() {
    seeking = true
  }

  fun finishSeek(actualPositionMs: Int) {
    // A seek may reconnect to an already watched prefix, but it cannot extend it.
    connected = actualPositionMs in 0..watchedThroughMs
    seeking = false
  }

  fun naturalCompletion(): Boolean {
    if (!isFullEpisode || seeking || !connected || completed) return false
    completed = true
    return true
  }

  fun replay() {
    watchedThroughMs = 0
    connected = true
    seeking = false
    completed = false
  }
}
