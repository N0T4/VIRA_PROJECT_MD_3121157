package com.meta.spatial.samples.startersample

import com.meta.spatial.core.Pose
import com.meta.spatial.core.Vector3
import kotlin.math.abs

internal class LibraryDropTarget(val pose: Pose) {
  private val worldToTarget = pose.inverse()

  fun contains(cardPosition: Vector3): Boolean {
    // Local X and Y lie in the panel plane. All distances are in meters.
    val local = worldToTarget * cardPosition
    return abs(local.x) <= width / 2f + edgeTolerance &&
        abs(local.y) <= height / 2f + edgeTolerance &&
        local.z in -frontDepth..backDepth
  }

  companion object {
    const val width = 0.34f
    const val height = 0.22f

    private const val edgeTolerance = 0.02f
    // Local -Z faces the viewer. Allow the card to stop just in front of the panel.
    private const val frontDepth = 0.12f
    private const val backDepth = 0.05f
  }
}
