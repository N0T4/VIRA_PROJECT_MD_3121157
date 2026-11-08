package com.meta.spatial.samples.startersample

import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimeCardHingeTest {
  @Test
  fun leafUnfoldsAroundTheSameSpineAfterMovingTheCard() {
    val halfWidth = AnimeCardHinge.leafWidth / 2f
    val leftEdge = Vector3(-halfWidth, 0f, 0f)
    val movedCover = Pose(Vector3(0.55f, 1.15f, -0.30f), Quaternion.fromAxisAngle(Vector3.Up, 33f))
    val expectedSpine = movedCover * Vector3(halfWidth, 0f, 0.01f)

    for (sample in 0..20) {
      val elapsed = AnimeCardHinge.durationMs * sample / 20
      val progress = AnimeCardHinge.easedProgress(elapsed)
      val actualSpine = (movedCover * AnimeCardHinge.rightLeafPose(progress)) * leftEdge
      assertTrue("Spine moved at ${elapsed}ms: $actualSpine", expectedSpine.isWithinDistance(actualSpine, 0.00001f))
    }

    val folded = AnimeCardHinge.rightLeafPose(AnimeCardHinge.easedProgress(0L))
    val open = AnimeCardHinge.rightLeafPose(AnimeCardHinge.easedProgress(AnimeCardHinge.durationMs))
    assertEquals(0f, folded.t.x, 0.00001f)
    assertEquals(0.01f, folded.t.z, 0.00001f)
    assertTrue(open.t.x > halfWidth * 1.9f)
    assertTrue((open.q * Vector3(0f, 0f, -1f)).z < -0.95f)
  }
}
