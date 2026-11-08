package com.meta.spatial.samples.startersample

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.annotation.MainThread
import androidx.annotation.RawRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.IOException

internal enum class DemoVideoPhase { Idle, Loading, Playing, Paused, Ended, Error }

internal data class DemoVideoState(
    val phase: DemoVideoPhase = DemoVideoPhase.Idle,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
)

@MainThread
internal class DemoVideoPlayer(private val context: Context) {
  var state by mutableStateOf(DemoVideoState())
    private set

  private var mediaPlayer: MediaPlayer? = null
  private var pendingVideo: Int? = null
  private var playOnReady = false
  private var lastPositionUpdate = 0L
  private var episodeCompletion: EpisodeCompletionPolicy? = null
  private var onEpisodeCompleted: (() -> Unit)? = null
  private var activeSeek: Int? = null
  private var queuedSeek: Int? = null

  fun start(@RawRes videoRes: Int, isFullEpisode: Boolean, onEpisodeCompleted: () -> Unit) {
    stop()
    pendingVideo = videoRes
    playOnReady = true
    episodeCompletion = EpisodeCompletionPolicy(isFullEpisode)
    this.onEpisodeCompleted = onEpisodeCompleted
    state = DemoVideoState(phase = DemoVideoPhase.Loading)
  }

  fun attachSurface(surface: Surface) {
    val video = pendingVideo ?: return
    safely {
      // The Spatial SDK owns this surface. MediaPlayer only borrows it.
      val existingPlayer = mediaPlayer
      if (existingPlayer != null) {
        existingPlayer.setSurface(surface)
        return@safely
      }
      val player = MediaPlayer()
      mediaPlayer = player
      player.setSurface(surface)
      player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
      player.setOnPreparedListener { readyPlayer ->
        if (mediaPlayer !== readyPlayer) return@setOnPreparedListener
        safely {
          val duration = readyPlayer.duration.coerceAtLeast(0)
          if (playOnReady) readyPlayer.start()
          state = DemoVideoState(
              phase = if (playOnReady) DemoVideoPhase.Playing else DemoVideoPhase.Paused,
              durationMs = duration,
          )
          trace("prepared duration_ms=$duration width=${readyPlayer.videoWidth} height=${readyPlayer.videoHeight}")
          if (playOnReady) trace("playing position_ms=0")
        }
      }
      player.setOnInfoListener { source, what, _ ->
        if (mediaPlayer === source && what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
          trace("first_frame")
        }
        false
      }
      player.setOnCompletionListener { completedPlayer ->
        if (mediaPlayer !== completedPlayer) return@setOnCompletionListener
        val credit = state.phase == DemoVideoPhase.Playing && episodeCompletion?.naturalCompletion() == true
        state = state.copy(phase = DemoVideoPhase.Ended, positionMs = state.durationMs)
        trace("ended position_ms=${state.positionMs}")
        if (credit) onEpisodeCompleted?.invoke()
      }
      player.setOnSeekCompleteListener { seekedPlayer ->
        if (mediaPlayer !== seekedPlayer || activeSeek == null) return@setOnSeekCompleteListener
        safely {
          val position = seekedPlayer.currentPosition
          activeSeek = null
          episodeCompletion?.finishSeek(position)
          state = state.copy(positionMs = position)
          trace("seek_completed position_ms=${state.positionMs} phase=${state.phase}")
          val next = queuedSeek
          queuedSeek = null
          if (next != null) beginSeek(seekedPlayer, next)
        }
      }
      player.setOnErrorListener { failedPlayer, what, extra ->
        if (mediaPlayer === failedPlayer) fail("player_error what=$what extra=$extra")
        true
      }
      context.resources.openRawResourceFd(video).use { asset ->
        requireNotNull(asset) { "Demo video is unavailable" }
        player.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
      }
      player.prepareAsync()
    }
  }

  fun togglePlayback() {
    when (state.phase) {
      DemoVideoPhase.Playing -> pause()
      DemoVideoPhase.Paused, DemoVideoPhase.Ended -> {
        val player = mediaPlayer ?: return
        safely {
          val replay = state.phase == DemoVideoPhase.Ended
          if (replay && activeSeek != null) return@safely
          if (replay) episodeCompletion?.replay()
          player.start()
          val position = if (replay) 0 else player.currentPosition
          state = state.copy(phase = DemoVideoPhase.Playing, positionMs = position)
          trace("playing position_ms=$position")
        }
      }
      else -> Unit
    }
  }

  fun pause() {
    playOnReady = false
    if (state.phase != DemoVideoPhase.Playing) return
    val player = mediaPlayer ?: return
    safely {
      player.pause()
      val position = player.currentPosition
      episodeCompletion?.observePlayback(position)
      state = state.copy(phase = DemoVideoPhase.Paused, positionMs = position)
      trace("paused position_ms=$position")
    }
  }

  fun seekTo(positionMs: Int) {
    if (state.phase != DemoVideoPhase.Playing && state.phase != DemoVideoPhase.Paused &&
        state.phase != DemoVideoPhase.Ended) return
    val player = mediaPlayer ?: return
    val destination = positionMs.coerceIn(0, state.durationMs)
    val inFlight = activeSeek
    if (inFlight != null) {
      // Serialize seeks so each callback describes the destination being credited.
      queuedSeek = destination.takeUnless { it == inFlight }
      return
    }
    safely {
      beginSeek(player, destination)
    }
  }

  private fun beginSeek(player: MediaPlayer, destination: Int) {
    if (state.phase == DemoVideoPhase.Playing) episodeCompletion?.observePlayback(player.currentPosition)
    episodeCompletion?.beginSeek()
    activeSeek = destination
    player.seekTo(destination.toLong(), MediaPlayer.SEEK_CLOSEST)
    if (state.phase == DemoVideoPhase.Ended) state = state.copy(phase = DemoVideoPhase.Paused)
    trace("seek_requested position_ms=$destination phase=${state.phase}")
  }

  fun updatePosition() {
    if (state.phase != DemoVideoPhase.Playing) return
    val now = SystemClock.uptimeMillis()
    if (now - lastPositionUpdate < 250L) return
    lastPositionUpdate = now
    val player = mediaPlayer ?: return
    safely {
      val position = player.currentPosition
      episodeCompletion?.observePlayback(position)
      state = state.copy(positionMs = position)
    }
  }

  fun stop() {
    val player = mediaPlayer
    mediaPlayer = null
    pendingVideo = null
    playOnReady = false
    episodeCompletion = null
    onEpisodeCompleted = null
    activeSeek = null
    queuedSeek = null
    lastPositionUpdate = 0L
    player?.release()
    state = DemoVideoState()
  }

  private inline fun safely(action: () -> Unit) {
    try {
      action()
    } catch (failure: RuntimeException) {
      fail(failure.javaClass.simpleName)
    } catch (failure: IOException) {
      fail(failure.javaClass.simpleName)
    }
  }

  private fun fail(reason: String) {
    stop()
    state = DemoVideoState(phase = DemoVideoPhase.Error)
    if (BuildConfig.DEBUG) Log.w("ViraVideo", "error reason=$reason")
  }

  private fun trace(message: String) {
    if (BuildConfig.DEBUG) Log.i("ViraVideo", message)
  }
}
