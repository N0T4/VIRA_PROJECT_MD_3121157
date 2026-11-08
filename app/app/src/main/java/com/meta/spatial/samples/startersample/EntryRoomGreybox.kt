package com.meta.spatial.samples.startersample

import com.meta.spatial.core.Color4
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Vector3
import com.meta.spatial.toolkit.Box
import com.meta.spatial.toolkit.Material
import com.meta.spatial.toolkit.Mesh
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.Transform

private val Concrete = Color4(0.16f, 0.15f, 0.16f, 1f)
private val Ceiling = Color4(0.055f, 0.049f, 0.053f, 1f)
private val Steel = Color4(0.027f, 0.028f, 0.036f, 1f)
private val Amber = Color4(0.68f, 0.29f, 0.10f, 1f)
private val Red = Color4(0.62f, 0.036f, 0.043f, 1f)
private val Dusk = Color4(0.24f, 0.19f, 0.31f, 1f)
private val SkylineFar = Color4(0.11f, 0.095f, 0.16f, 1f)
private val SkylineNear = Color4(0.067f, 0.063f, 0.115f, 1f)

// Replace these blockout volumes when the owner supplies the room model.
internal fun createEntryRoomGreybox() {
  box(Concrete, 0f, -0.10f, 0.15f, 7.6f, 0.2f, 7.3f)
  box(Ceiling, 0f, 3.47f, 0.15f, 7.6f, 0.16f, 7.3f)
  box(Concrete, 0f, 1.7f, 3.75f, 7.6f, 3.4f, 0.14f)
  for (x in listOf(-3.74f, 3.74f)) {
    box(Concrete, x, 1.7f, 0.15f, 0.12f, 3.4f, 7.2f)
    box(Amber, x * 0.98f, 2.74f, 0.1f, 0.04f, 0.035f, 6.5f)
  }

  // Large loft windows face a blockout dusk skyline beyond the room.
  for (x in listOf(-3.7f, -2.5f, -1.25f, 0f, 1.25f, 2.5f, 3.7f)) {
    box(Steel, x, 1.7f, -3.45f, 0.09f, 3.4f, 0.14f)
  }
  for (y in listOf(0.18f, 1.24f, 2.38f, 3.36f)) {
    box(Steel, 0f, y, -3.45f, 7.5f, 0.085f, 0.14f)
  }
  box(Dusk, 0f, 4f, -17f, 44f, 16f, 0.25f)
  for ((index, height) in listOf(2.2f, 4.6f, 3.3f, 5.2f, 2.9f, 4.4f, 3.7f).withIndex()) {
    box(SkylineFar, (index - 3) * 2.8f, height / 2f, -12f, 2.4f, height, 1f)
  }
  for ((index, height) in listOf(1.5f, 2.7f, 1.9f, 3.5f, 2.1f, 2.8f).withIndex()) {
    box(SkylineNear, (index - 2.5f) * 2.25f, height / 2f, -8.8f, 1.65f, height, 0.9f)
  }

  // The screen itself is the fixed 16:9 panel in Composition/Main.scene.
  box(Steel, 0f, 1.55f, -2.86f, 3.34f, 1.94f, 0.16f)
  box(Steel, 0f, 0.23f, -2.93f, 3.6f, 0.46f, 0.48f)
  box(Red, 0f, 0.24f, -2.68f, 2.85f, 0.024f, 0.025f)
  for (z in listOf(-2.9f, -0.6f, 1.7f)) {
    box(Steel, 0f, 3.28f, z, 7.5f, 0.25f, 0.17f)
    box(Amber, 0f, 3.14f, z, 2.5f, 0.026f, 0.05f)
  }
}

private fun box(
    color: Color4,
    x: Float,
    y: Float,
    z: Float,
    width: Float,
    height: Float,
    depth: Float,
) {
  val halfSize = Vector3(width / 2f, height / 2f, depth / 2f)
  Entity.create(
      listOf(
          Mesh(Box.URI, hittable = MeshCollision.NoCollision),
          Box(-halfSize, halfSize),
          Material().apply {
            baseColor = color
            unlit = true
          },
          Transform(Pose(Vector3(x, y, z))),
      ),
  )
}
