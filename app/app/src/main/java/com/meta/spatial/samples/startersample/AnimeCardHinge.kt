package com.meta.spatial.samples.startersample

import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.Vector3

internal object AnimeCardHinge {
  const val leafWidth = 0.26f
  const val leafHeight = 0.37f
  const val durationMs = 360L

  private const val foldedYaw = -180f
  private const val openYaw = 10f

  // A small spine depth keeps the folded leaf behind the cover without z-fighting.
  private val spine = Vector3(leafWidth / 2f, 0f, 0.01f)
  private val leafCenter = Pose(Vector3(leafWidth / 2f, 0f, 0f))

  fun rightLeafPose(progress: Float): Pose {
    val yaw = foldedYaw + (openYaw - foldedYaw) * progress.coerceIn(0f, 1f)
    val rotation = Quaternion.fromAxisAngle(Vector3.Up, yaw)
    return Pose(spine, rotation) * leafCenter
  }

  fun easedProgress(elapsedMs: Long): Float {
    val fraction = (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)
    return fraction * fraction * (3f - 2f * fraction)
  }
}
