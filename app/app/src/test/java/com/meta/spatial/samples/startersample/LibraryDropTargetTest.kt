package com.meta.spatial.samples.startersample

import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.Vector3
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryDropTargetTest {
  private val pose = Pose(
      Vector3(1.1f, 1.3f, -0.8f),
      Quaternion.lookRotation(Vector3(0.6f, -0.3f, 0.5f)),
  )
  private val target = LibraryDropTarget(pose)

  @Test
  fun acceptsNearCenterAndEdgeOfRotatedTarget() {
    assertTrue(target.contains(pose.t))
    val nearCorner = Vector3(
        LibraryDropTarget.width / 2f + 0.015f,
        LibraryDropTarget.height / 2f + 0.015f,
        -0.10f,
    )
    assertTrue(target.contains(pose * nearCorner))
  }

  @Test
  fun rejectsPositionsOutsideThePanelFootprintOrDepth() {
    assertFalse(target.contains(pose * Vector3(-LibraryDropTarget.width / 2f - 0.03f, 0f, 0f)))
    assertFalse(target.contains(pose * Vector3(0f, -LibraryDropTarget.height / 2f - 0.03f, 0f)))
    assertFalse(target.contains(pose * Vector3(0f, 0f, -0.14f)))
    assertFalse(target.contains(pose * Vector3(0f, 0f, 0.07f)))
  }

  @Test
  fun rejectsNonfinitePositions() {
    assertFalse(target.contains(Vector3(Float.NaN, pose.t.y, pose.t.z)))
    assertFalse(target.contains(Vector3(pose.t.x, Float.POSITIVE_INFINITY, pose.t.z)))
  }
}
