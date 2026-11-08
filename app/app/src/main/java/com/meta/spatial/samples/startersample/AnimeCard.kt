package com.meta.spatial.samples.startersample

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.meta.spatial.core.Color4
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Vector3
import com.meta.spatial.isdk.IsdkGrabConstraints
import com.meta.spatial.isdk.IsdkGrabState
import com.meta.spatial.isdk.IsdkGrabbable
import com.meta.spatial.isdk.IsdkSystem
import com.meta.spatial.runtime.PointerEvent
import com.meta.spatial.runtime.PointerEventType
import com.meta.spatial.runtime.SemanticType
import com.meta.spatial.samples.startersample.catalog.CatalogTitle
import com.meta.spatial.samples.startersample.catalog.demoEpisodesFor
import com.meta.spatial.samples.startersample.library.LibraryEntry
import com.meta.spatial.toolkit.Box
import com.meta.spatial.toolkit.Controller
import com.meta.spatial.toolkit.Material
import com.meta.spatial.toolkit.Mesh
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.Panel
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.TransformParent

internal sealed interface CardGripDrag {
  data class Started(val center: Vector3) : CardGripDrag
  data class Moved(val center: Vector3) : CardGripDrag
  data class Released(val center: Vector3) : CardGripDrag
  data object Cancelled : CardGripDrag
}

internal class AnimeCard(
    val title: CatalogTitle,
    pose: Pose,
    private val isdk: IsdkSystem?,
    private val onGripDrag: (AnimeCard, CardGripDrag) -> Unit = { _, _ -> },
    initialSelectedEpisodeId: String? = null,
) {
  val episodes = demoEpisodesFor(title)
  var selectedEpisodeId by mutableStateOf(
      episodes.firstOrNull { it.id == initialSelectedEpisodeId && it.isSelectable }?.id,
  )
    private set
  var libraryEntry by mutableStateOf<LibraryEntry?>(null)
  var libraryLoading by mutableStateOf(true)
  var libraryBusy by mutableStateOf(false)
  var libraryError by mutableStateOf<String?>(null)

  private val gripOffset = Vector3(0f, AnimeCardHinge.leafHeight / 2f + 0.015f, 0f)
  private val gripStart = pose * Pose(gripOffset)
  private val gripHalfSize = Vector3(0.083f, 0.017f, 0.022f)
  private val grip = Entity.create(listOf(
      Transform(gripStart),
      Mesh(Box.URI, hittable = MeshCollision.LineTest),
      Box(-gripHalfSize, gripHalfSize),
      Material().apply {
        baseColor = Color4(0.82f, 0.10f, 0.16f, 1f)
        unlit = true
      },
      IsdkGrabbable(),
      IsdkGrabConstraints(maxGrabbers = 1),
  ))
  val cover = Entity.create(listOf(
      Panel(R.id.anime_card_cover),
      TransformParent(grip),
      Transform(Pose(-gripOffset)),
  ))
  var inside by mutableStateOf<Entity?>(null)
    private set
  var expanded by mutableStateOf(false)
    private set
  var gripHeld by mutableStateOf(false)
    private set
  var actionGeneration by mutableIntStateOf(0)
    private set
  private var transitionStartedAt: Long? = null
  private data class PanelPress(val panel: Entity, val source: Entity)
  private class GripDrag(val source: Entity, val pointerType: Int) {
    var lastCenter: Vector3? = null
    var releaseRequested = false
    var releaseCenter: Vector3? = null
  }

  private var press: PanelPress? = null
  private var gripDrag: GripDrag? = null
  private var loggedGripHeld = false
  private var loggedMovement = false
  private val gripObserver = isdk?.registerInteractableObserver(grip, ::onGripEvent)
  private val coverObserver = isdk?.registerInteractableObserver(cover) { onPanelEvent(cover, "cover", it) }
  private var insideObserver: IsdkSystem.InteractableObserverHandle? = null

  fun acceptsAction(generation: Int): Boolean {
    if (isGripHeld()) updateGripHeld(true)
    if (press?.source?.isTrackingInactive() == true) cancelPress("tracking_lost")
    val accepted = !gripHeld && generation == actionGeneration
    trace("card_action accepted=$accepted held=$gripHeld current=$actionGeneration callback=$generation")
    return accepted
  }

  fun selectEpisode(id: String, generation: Int) {
    if (!expanded || selectedEpisodeId == id || !acceptsAction(generation)) return
    if (episodes.none { it.id == id && it.isSelectable }) return

    selectedEpisodeId = id
    trace("episode_selected id=$id")
  }

  fun toggle() {
    if (transitionStartedAt != null) return
    expanded = !expanded
    if (expanded) {
      val leaf = Entity.create(listOf(
          Panel(R.id.anime_card_inside),
          TransformParent(cover),
          Transform(AnimeCardHinge.rightLeafPose(0f)),
      ))
      insideObserver = isdk?.registerInteractableObserver(leaf) { onPanelEvent(leaf, "inside", it) }
      inside = leaf
    } else if (press?.let { it.panel == inside } == true) {
      cancelPress("inside_folding")
    }
    transitionStartedAt = SystemClock.uptimeMillis()
    trace(if (expanded) "opening" else "closing")
  }

  fun tick() {
    val held = isGripHeld()
    updateGripHeld(held)
    updateGripDrag(held)
    if (press?.source?.isTrackingInactive() == true) cancelPress("tracking_lost")
    if (BuildConfig.DEBUG) traceMovement(held)

    val startedAt = transitionStartedAt ?: return
    val elapsed = SystemClock.uptimeMillis() - startedAt
    val progress = AnimeCardHinge.easedProgress(elapsed)
    inside?.setComponent(Transform(AnimeCardHinge.rightLeafPose(if (expanded) progress else 1f - progress)))
    if (elapsed >= AnimeCardHinge.durationMs) {
      transitionStartedAt = null
      if (!expanded) destroyInside()
      trace(if (expanded) "open" else "folded")
    }
  }

  fun destroy() {
    cancelGripDrag("card_destroyed")
    if (isdk != null) {
      gripObserver?.let(isdk::unregisterInteractableObserver)
      coverObserver?.let(isdk::unregisterInteractableObserver)
    }
    destroyInside()
    cover.destroy()
    grip.destroy()
  }

  private fun destroyInside() {
    val leaf = inside ?: return
    if (press?.panel == leaf) cancelPress("inside_removed")
    insideObserver?.let { isdk?.unregisterInteractableObserver(it) }
    insideObserver = null
    inside = null
    leaf.destroy()
  }

  private fun isGripHeld(): Boolean {
    val grabbable = grip.tryGetComponent<IsdkGrabbable>() ?: return false
    val held = grabbable.grabState == IsdkGrabState.Grabbed
    grabbable.recycle()
    return held
  }

  private fun updateGripHeld(held: Boolean) {
    if (held == gripHeld) return
    gripHeld = held
    cancelPress(if (held) "grab_started" else "grab_released")
  }

  private fun onGripEvent(event: PointerEvent) {
    if (event.semanticType != SemanticType.Grab.id) return
    when (event.type) {
      PointerEventType.Select.id -> {
        updateGripHeld(true)
        if (gripDrag?.releaseRequested == true) cancelGripDrag("grab_restarted")
        if (gripDrag == null && !event.source.isTrackingInactive()) {
          gripDrag = GripDrag(event.source, event.pointerType)
        }
        tracePointer("grip_select", event)
      }
      PointerEventType.Unselect.id -> {
        val drag = gripDrag
        if (drag != null && drag.source == event.source && drag.pointerType == event.pointerType) {
          if (drag.lastCenter == null || drag.source.isTrackingInactive()) {
            cancelGripDrag("release_unverified")
          } else {
            drag.releaseRequested = true
          }
        }
        tracePointer("grip_unselect", event)
      }
      PointerEventType.Cancel.id -> {
        val drag = gripDrag
        if (drag != null && drag.source == event.source && drag.pointerType == event.pointerType) {
          cancelGripDrag("pointer_cancel")
        }
        tracePointer("grip_cancel", event)
      }
    }
  }

  private fun updateGripDrag(held: Boolean) {
    val drag = gripDrag ?: return
    if (drag.source.isTrackingInactive()) {
      cancelGripDrag("tracking_lost")
      return
    }
    if (!held && !drag.releaseRequested) {
      cancelGripDrag("grab_unconfirmed")
      return
    }
    if (drag.releaseRequested) {
      if (held) return
      val released = drag.releaseCenter
      if (released == null) {
        // Let any tracking loss or pointer cancellation accompanying the release settle first.
        drag.releaseCenter = cardCenter()
        if (drag.releaseCenter == null) cancelGripDrag("position_unavailable")
      } else {
        gripDrag = null
        onGripDrag(this, CardGripDrag.Released(released))
      }
      return
    }
    val center = cardCenter() ?: run {
      cancelGripDrag("position_unavailable")
      return
    }
    val last = drag.lastCenter
    if (last == null || !center.isWithinDistance(last, 0.0005f)) {
      drag.lastCenter = center
      onGripDrag(this, if (last == null) CardGripDrag.Started(center) else CardGripDrag.Moved(center))
    }
  }

  private fun cardCenter(): Vector3? = coverPose()?.t

  fun coverPose(): Pose? {
    val transform = grip.tryGetComponent<Transform>() ?: return null
    val pose = transform.transform * Pose(-gripOffset)
    transform.recycle()
    return pose
  }

  private fun cancelGripDrag(reason: String) {
    val drag = gripDrag ?: return
    gripDrag = null
    if (drag.lastCenter != null) onGripDrag(this, CardGripDrag.Cancelled)
    trace("grip_drag_cancelled reason=$reason")
  }

  private fun onPanelEvent(panel: Entity, name: String, event: PointerEvent) {
    if (event.semanticType != SemanticType.Select.id) return
    if (event.type == PointerEventType.Select.id) {
      tracePointer("${name}_select held=$gripHeld", event)
      if (!gripHeld && !isGripHeld() && press == null) press = PanelPress(panel, event.source)
      return
    }
    val pending = press
    val ownsPress = pending != null && pending.panel == panel && pending.source == event.source
    if (event.type == PointerEventType.Unselect.id || event.type == PointerEventType.Cancel.id) {
      tracePointer("${name}_release type=${event.type} owner=$ownsPress", event)
    }
    if (!ownsPress) return
    when (event.type) {
      PointerEventType.Unselect.id -> {
        if (event.source.isTrackingInactive()) cancelPress("tracking_lost") else press = null
      }
      PointerEventType.Cancel.id, PointerEventType.Unhover.id -> cancelPress("pointer_lost")
    }
  }

  private fun cancelPress(reason: String) {
    press = null
    actionGeneration++
    trace("action_input_reset reason=$reason")
  }

  private fun Entity.isTrackingInactive(): Boolean {
    val controller = tryGetComponent<Controller>() ?: return true
    val inactive = !controller.isActive
    controller.recycle()
    return inactive
  }

  private fun tracePointer(action: String, event: PointerEvent) {
    if (!BuildConfig.DEBUG) return
    val controller = event.source.tryGetComponent<Controller>()
    val input = controller?.type
    val buttons = controller?.buttonState
    val changed = controller?.changedButtons
    val directTouch = controller?.directTouchButtonState
    controller?.recycle()
    val uv = event.hitInfo.textureCoordinate
    trace("$action input=$input hand=${isdk?.getHandForPointerEvent(event)} " +
        "behavior=${isdk?.getInteractionEventSourceBehavior(event)} uv=$uv " +
        "buttons=$buttons changed=$changed direct=$directTouch")
  }

  private fun traceMovement(held: Boolean) {
    val transform = grip.tryGetComponent<Transform>() ?: return
    val position = transform.transform.t
    transform.recycle()
    if (loggedGripHeld != held) {
      loggedGripHeld = held
      trace("grabbed=$held position=$position")
    }
    if (!loggedMovement && !position.isWithinDistance(gripStart.t, 0.03f)) {
      loggedMovement = true
      trace("moved initial=${gripStart.t} current=$position")
    }
  }

  private fun trace(message: String) {
    if (BuildConfig.DEBUG) Log.i("ViraCard", "$message title=${title.id}")
  }
}
