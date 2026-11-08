/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.spatial.samples.startersample

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import com.meta.spatial.castinputforward.CastInputForwardFeature
import com.meta.spatial.compose.ComposeFeature
import com.meta.spatial.compose.ComposeViewPanelRegistration
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.SpatialFeature
import com.meta.spatial.core.Vector3
import com.meta.spatial.datamodelinspector.DataModelInspectorFeature
import com.meta.spatial.debugtools.HotReloadFeature
import com.meta.spatial.isdk.IsdkSystem
import com.meta.spatial.okhttp3.OkHttpAssetFetcher
import com.meta.spatial.ovrmetrics.OVRMetricsDataModel
import com.meta.spatial.ovrmetrics.OVRMetricsFeature
import com.meta.spatial.runtime.ButtonBits
import com.meta.spatial.runtime.NetworkedAssetLoader
import com.meta.spatial.runtime.ReferenceSpace
import com.meta.spatial.runtime.SessionState
import com.meta.spatial.samples.startersample.catalog.CatalogRepository
import com.meta.spatial.samples.startersample.catalog.CatalogTitle
import com.meta.spatial.samples.startersample.catalog.CatalogUnavailableException
import com.meta.spatial.samples.startersample.library.LibraryDatabase
import com.meta.spatial.samples.startersample.library.LibraryRepository
import com.meta.spatial.samples.startersample.library.LibraryStatus
import com.meta.spatial.toolkit.AppSystemActivity
import com.meta.spatial.toolkit.Controller
import com.meta.spatial.toolkit.ControllerType
import com.meta.spatial.toolkit.DpPerMeterDisplayOptions
import com.meta.spatial.toolkit.MediaPanelRenderOptions
import com.meta.spatial.toolkit.MediaPanelSettings
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.Panel
import com.meta.spatial.toolkit.PanelRegistration
import com.meta.spatial.toolkit.PanelStyleOptions
import com.meta.spatial.toolkit.PixelDisplayOptions
import com.meta.spatial.toolkit.PlayerBodyAttachmentSystem
import com.meta.spatial.toolkit.QuadShapeOptions
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.UIPanelSettings
import com.meta.spatial.toolkit.VideoSurfacePanelRegistration
import com.meta.spatial.toolkit.Visible
import com.meta.spatial.vr.VRFeature
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val MAIN_SCREEN_WIDTH = 3.2f
private const val MAIN_SCREEN_HEIGHT = 1.8f

class StarterSampleActivity : AppSystemActivity() {
  private data class VisibleLibraryDrop(val panel: Entity, val target: LibraryDropTarget)
  private data class DemoPlaybackCard(
      val title: CatalogTitle,
      val selectedEpisodeId: String?,
      val originalPose: Pose,
      val expanded: Boolean,
  )

  private val activityScope = CoroutineScope(Dispatchers.Main)
  private val catalogRepository = CatalogRepository()
  private val demoVideoPlayer by lazy { DemoVideoPlayer(applicationContext) }
  private var entryScreen: Entity? = null
  private var demoVideo by mutableStateOf<Entity?>(null)
  private var demoControls: Entity? = null
  private var demoCard: DemoPlaybackCard? = null
  private val libraryRepository by lazy { LibraryRepository(LibraryDatabase.getInstance(applicationContext).entries()) }
  private var libraryReadJob: Job? = null
  private var libraryPanel: Entity? = null
  private var libraryStatus by mutableStateOf(LibraryStatus.Planning)
  private var libraryRows by mutableStateOf<List<LibraryPanelRow>?>(null)
  private var libraryFailed by mutableStateOf(false)
  private var libraryCategoryJob: Job? = null
  private var libraryDrop: VisibleLibraryDrop? = null
  private var libraryDropState by mutableStateOf<LibraryDropState?>(null)
  private var libraryDropFeedback: Job? = null
  private var toolsPanel: Entity? = null
  private var animeCard by mutableStateOf<AnimeCard?>(null)
  private var sceneReadyForTools by mutableStateOf(false)
  private var activityResumed by mutableStateOf(false)
  private var sessionFocused by mutableStateOf(false)
  private var searchFieldFocused = false
  private var preservingSearchEditor by mutableStateOf(false)
  private var controllerButtonWasDown = true
  private var entryInputGeneration by mutableIntStateOf(0)

  private val toolsInputAvailable: Boolean
    get() = sceneReadyForTools && activityResumed && sessionFocused

  override fun registerFeatures(): List<SpatialFeature> {
    val features = mutableListOf<SpatialFeature>(
        VRFeature(this),
        ComposeFeature(),
    )
    if (BuildConfig.DEBUG) {
      features.add(CastInputForwardFeature(this))
      features.add(HotReloadFeature(this))
      features.add(OVRMetricsFeature(this, OVRMetricsDataModel() { numberOfMeshes() }))
      features.add(DataModelInspectorFeature(spatial, this.componentManager))
    }
    return features
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    NetworkedAssetLoader.init(
        File(applicationContext.getCacheDir().canonicalPath),
        OkHttpAssetFetcher(),
    )

    val compositionRoot = Entity.create()
    activityScope.launch {
      glXFManager.inflateGLXF("apk:///scenes/Composition.glxf".toUri(), rootEntity = compositionRoot)
    }
  }

  override fun onSceneReady() {
    super.onSceneReady()

    // set the reference space to enable recentering
    scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)

    scene.setViewOrigin(0.0f, 0.0f, 2.0f, 180.0f)
    createEntryRoomGreybox()
    createFixedLibrary()
    sceneReadyForTools = true
  }

  override fun onSessionStateChanged(state: SessionState) {
    super.onSessionStateChanged(state)
    runOnUiThread {
      val hadFocus = sessionFocused
      sessionFocused = state == SessionState.FOCUSED
      if (hadFocus && !sessionFocused) {
        // Quest's system keyboard takes XR focus while the search editor stays visible.
        preservingSearchEditor = state == SessionState.VISIBLE && searchFieldFocused
        suspendToolsInput("focus_lost", preservingSearchEditor)
        if (BuildConfig.DEBUG && preservingSearchEditor) Log.i("ViraTools", "kept_search_editor")
      } else if (sessionFocused) {
        preservingSearchEditor = false
      } else if (state != SessionState.VISIBLE && toolsPanel != null) {
        suspendToolsInput("session_hidden")
      }
    }
  }

  override fun onResume() {
    super.onResume()
    activityResumed = true
  }

  override fun onPause() {
    activityResumed = false
    suspendToolsInput("activity_pause")
    super.onPause()
  }

  override fun onSceneTick() {
    super.onSceneTick()
    if (!toolsInputAvailable) return

    animeCard?.tick()
    if (demoVideo != null) demoVideoPlayer.updatePosition()
    val down = rightControllerButtonDown()
    if (down == null) {
      controllerButtonWasDown = true
      return
    }

    if (down && !controllerButtonWasDown) {
      if (demoVideo != null) {
        toggleDemoCard("controller_b")
      } else if (toolsPanel == null) {
        showTools("controller_b")
      } else {
        hideTools("controller_b")
      }
    }
    controllerButtonWasDown = down
  }

  private fun rightControllerButtonDown(): Boolean? {
    val body = systemManager.tryFindSystem<PlayerBodyAttachmentSystem>()
        ?.tryGetLocalPlayerAvatarBody() ?: return null
    val rightHand = body.rightHand
    body.recycle()
    val controller = rightHand.tryGetComponent<Controller>() ?: return null
    val down = if (controller.isActive && controller.type == ControllerType.CONTROLLER) {
      (controller.buttonState and ButtonBits.ButtonB) != 0
    } else {
      null
    }
    controller.recycle()
    return down
  }

  override fun onSpatialShutdown() {
    sceneReadyForTools = false
    sessionFocused = false
    suspendToolsInput("shutdown")
    stopDemoPlayback(restoreCard = false)
    entryScreen = null
    libraryCategoryJob?.cancel()
    libraryPanel?.destroy()
    libraryPanel = null
    activityScope.cancel()
    super.onSpatialShutdown()
  }

  private fun suspendToolsInput(source: String, keepSearchEditor: Boolean = false) {
    controllerButtonWasDown = true
    // Recreate the fixed screen's input node so an in-progress click cannot cross focus loss.
    entryInputGeneration++
    if (demoVideo != null) demoVideoPlayer.pause()
    hideAnimeCard(source)
    if (!keepSearchEditor) hideTools(source)
  }

  private fun showTools(source: String) {
    if (!toolsInputAvailable || toolsPanel != null || demoVideo != null) return

    toolsPanel = Entity.create(listOf(Panel(R.id.tools_panel), Transform(toolsPose())))
    if (BuildConfig.DEBUG) Log.i("ViraTools", "shown source=$source")
  }

  private fun recenterTools(panel: Entity) {
    if (!toolsInputAvailable || toolsPanel != panel) return

    panel.setComponent(Transform(toolsPose()))
    if (BuildConfig.DEBUG) Log.i("ViraTools", "recentered source=panel_recenter")
  }

  private fun toolsPose(): Pose {
    val offset = Vector3(0f, -0.23f, 0.46f)
    // The panel's front is local -Z, so point its +Z axis from the viewer to its center.
    return scene.getViewerPose().removePitchAndRoll() * Pose(offset, Quaternion.lookRotation(offset))
  }

  private fun hideTools(source: String) {
    val panel = toolsPanel ?: return
    toolsPanel = null
    searchFieldFocused = false
    preservingSearchEditor = false
    panel.destroy()
    if (BuildConfig.DEBUG) Log.i("ViraTools", "hidden source=$source")
  }

  private fun showAnimeCard(title: CatalogTitle, sourcePanel: Entity) {
    if (!toolsInputAvailable || demoVideo != null ||
        (toolsPanel != sourcePanel && libraryPanel != sourcePanel)) return

    val current = animeCard
    if (current != null && (current.title.id == title.id || !current.acceptsAction(current.actionGeneration))) return

    val fromLibrary = sourcePanel == libraryPanel
    val anchor = if (fromLibrary) {
      // Keep the movable card within reach when it is opened from the fixed shelf.
      toolsPose()
    } else {
      val panelTransform = sourcePanel.tryGetComponent<Transform>() ?: return
      panelTransform.transform.also { panelTransform.recycle() }
    }
    val offset = if (fromLibrary) Vector3(0.20f, 0f, 0f) else Vector3(0.50f, 0f, -0.10f)
    val cardPosition = (anchor * Pose(offset)).t
    val cardOrientation = Quaternion.lookRotation(cardPosition - scene.getViewerPose().t)
    hideAnimeCard("title_changed")
    createAnimeCard(title, Pose(cardPosition, cardOrientation))
    if (BuildConfig.DEBUG) Log.i("ViraCard", "shown folded title=${title.id} tools_visible=${toolsPanel != null}")
  }

  private fun createAnimeCard(title: CatalogTitle, pose: Pose, selectedEpisodeId: String? = null): AnimeCard {
    val card = AnimeCard(
        title,
        pose,
        systemManager.tryFindSystem<IsdkSystem>(),
        onGripDrag = ::onCardGripDrag,
        initialSelectedEpisodeId = selectedEpisodeId,
    )
    animeCard = card
    watchCardLibrary(card)
    return card
  }

  private fun startDemoPlayback(card: AnimeCard, generation: Int) {
    if (!toolsInputAvailable || animeCard !== card || demoVideo != null ||
        !card.expanded || !card.acceptsAction(generation)) return
    val episode = card.episodes.firstOrNull { it.id == card.selectedEpisodeId && it.hasLocalVideo } ?: return
    val media = episode.localVideo ?: return
    val titleId = card.title.id
    val screen = entryScreen ?: return
    val screenTransform = screen.tryGetComponent<Transform>() ?: return
    val screenPose = screenTransform.transform.also { screenTransform.recycle() }
    val cardPose = card.coverPose() ?: return
    demoCard = DemoPlaybackCard(card.title, episode.id, cardPose, card.expanded)
    hideAnimeCard("demo_playback")
    hideTools("demo_playback")
    demoVideoPlayer.start(media.resourceId, media.isFullEpisode) {
      recordCompletedEpisode(titleId, episode.id)
    }
    screen.setComponent(Visible(false))
    val video = Entity.create(listOf(Transform(screenPose)))
    demoVideo = video
    video.setComponent(Panel(R.id.demo_video_panel, MeshCollision.NoCollision))
    val offset = Vector3(0f, -0.33f, 0.57f)
    val controlsPose = scene.getViewerPose().removePitchAndRoll() * Pose(offset, Quaternion.lookRotation(offset))
    demoControls = Entity.create(listOf(Panel(R.id.demo_video_controls), Transform(controlsPose)))
    if (BuildConfig.DEBUG) Log.i("ViraVideo", "opened episode=${episode.id} screen=${screenPose.t}")
  }

  private fun recordCompletedEpisode(titleId: String, episodeId: String) {
    activityScope.launch {
      try {
        val counted = libraryRepository.recordCompletedEpisode(titleId, episodeId)
        if (BuildConfig.DEBUG) Log.i("ViraLibrary", "episode_completed id=$episodeId counted=$counted")
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: Exception) {
        if (BuildConfig.DEBUG) Log.w("ViraLibrary", "episode_progress_failed type=${failure.javaClass.simpleName}")
      }
    }
  }

  private fun stopDemoPlayback(restoreCard: Boolean) {
    val video = demoVideo ?: return
    demoVideo = null
    demoVideoPlayer.stop()
    demoControls?.destroy()
    demoControls = null
    video.destroy()
    val snapshot = demoCard
    demoCard = null
    if (restoreCard) {
      entryScreen?.setComponent(Visible(true))
      if (animeCard == null && snapshot != null) restoreDemoCard(snapshot, snapshot.originalPose)
    }
    if (BuildConfig.DEBUG) Log.i("ViraVideo", "closed restored_card=$restoreCard")
  }

  private fun toggleDemoCard(source: String) {
    if (!toolsInputAvailable || demoVideo == null) return
    val snapshot = demoCard ?: return
    val current = animeCard
    if (current != null) {
      if (current.acceptsAction(current.actionGeneration)) hideAnimeCard(source)
      return
    }

    // Leave the transport below center and the fixed Library on the other side.
    val offset = Vector3(-0.49f, -0.09f, 0.54f)
    val pose = scene.getViewerPose().removePitchAndRoll() * Pose(offset, Quaternion.lookRotation(offset))
    val card = restoreDemoCard(snapshot, pose)
    traceDemoCard("card_shown", card, source)
  }

  private fun restoreDemoCard(snapshot: DemoPlaybackCard, pose: Pose): AnimeCard {
    val card = createAnimeCard(snapshot.title, pose, snapshot.selectedEpisodeId)
    if (snapshot.expanded) card.toggle()
    return card
  }

  private fun traceDemoCard(action: String, card: AnimeCard, source: String) {
    if (!BuildConfig.DEBUG) return
    val state = demoVideoPlayer.state
    Log.i("ViraVideo", "$action source=$source title=${card.title.id} selected=${card.selectedEpisodeId} " +
        "expanded=${card.expanded} phase=${state.phase} position_ms=${state.positionMs}")
  }

  private fun hideAnimeCard(source: String) {
    hideLibraryDrop()
    val card = animeCard ?: return
    val playing = demoVideo != null
    if (playing) {
      demoCard = demoCard?.copy(selectedEpisodeId = card.selectedEpisodeId, expanded = card.expanded)
    }
    animeCard = null
    libraryReadJob?.cancel()
    libraryReadJob = null
    card.destroy()
    if (playing) traceDemoCard("card_hidden", card, source)
    if (BuildConfig.DEBUG) Log.i("ViraCard", "hidden source=$source")
  }

  private fun watchCardLibrary(card: AnimeCard) {
    libraryReadJob?.cancel()
    card.libraryLoading = true
    card.libraryError = null
    libraryReadJob = activityScope.launch {
      try {
        libraryRepository.observe(card.title.id).distinctUntilChanged().collect { entry ->
          card.libraryEntry = entry
          card.libraryLoading = false
          if (BuildConfig.DEBUG) Log.i("ViraLibrary", "read title=${card.title.id} present=${entry != null}")
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: Exception) {
        card.libraryEntry = null
        card.libraryLoading = false
        card.libraryError = "Library could not be loaded."
        if (BuildConfig.DEBUG) Log.w("ViraLibrary", "read_failed type=${failure.javaClass.simpleName}")
      }
    }
  }

  private fun changeCardLibrary(
      card: AnimeCard,
      generation: Int,
      operation: String,
      change: suspend LibraryRepository.(String) -> Boolean,
  ) {
    if (!toolsInputAvailable || animeCard !== card || !card.expanded || card.libraryLoading ||
        card.libraryBusy || !card.acceptsAction(generation)) return

    writeCardLibrary(card, operation, change)
  }

  private fun writeCardLibrary(
      card: AnimeCard,
      operation: String,
      change: suspend LibraryRepository.(String) -> Boolean,
      drop: VisibleLibraryDrop? = null,
  ) {
    card.libraryBusy = true
    card.libraryError = null
    activityScope.launch {
      val changed = try {
        val updated = libraryRepository.change(card.title.id)
        if (!updated && operation != "add") card.libraryError = "Library entry no longer exists."
        if (BuildConfig.DEBUG) Log.i("ViraLibrary", "$operation title=${card.title.id} changed=$updated")
        updated
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: Exception) {
        card.libraryError = "Library change could not be saved."
        if (BuildConfig.DEBUG) Log.w("ViraLibrary", "write_failed type=${failure.javaClass.simpleName}")
        null
      } finally {
        card.libraryBusy = false
      }
      if (drop != null && libraryDrop === drop) {
        val result = when (changed) {
          true -> LibraryDropState.Added
          false -> LibraryDropState.AlreadySaved
          null -> LibraryDropState.Error
        }
        libraryDropState = result
        if (BuildConfig.DEBUG) Log.i("ViraDrop", "result=$result title=${card.title.id}")
        libraryDropFeedback = activityScope.launch {
          delay(1800L)
          libraryDropFeedback = null
          if (libraryDrop === drop) hideLibraryDrop()
        }
      }
    }
  }

  private fun onCardGripDrag(card: AnimeCard, event: CardGripDrag) {
    if (!toolsInputAvailable || animeCard !== card) return
    if (event !is CardGripDrag.Started && libraryDropState != LibraryDropState.Available &&
        libraryDropState != LibraryDropState.Active) return
    when (event) {
      is CardGripDrag.Started -> {
        if (card.libraryBusy) return
        hideLibraryDrop()
        val offset = Vector3(-0.28f, -0.36f, 0.48f)
        val pose = scene.getViewerPose().removePitchAndRoll() * Pose(offset, Quaternion.lookRotation(offset))
        val target = LibraryDropTarget(pose)
        libraryDropState = if (target.contains(event.center)) LibraryDropState.Active else LibraryDropState.Available
        val panel = Entity.create(listOf(Panel(R.id.library_drop_panel, MeshCollision.NoCollision), Transform(pose)))
        libraryDrop = VisibleLibraryDrop(panel, target)
        if (BuildConfig.DEBUG) Log.i("ViraDrop", "shown title=${card.title.id}")
      }
      is CardGripDrag.Moved -> {
        val drop = libraryDrop ?: return
        val state = if (drop.target.contains(event.center)) LibraryDropState.Active else LibraryDropState.Available
        if (libraryDropState != state) {
          libraryDropState = state
          if (BuildConfig.DEBUG) Log.i("ViraDrop", "hover inside=${state == LibraryDropState.Active}")
        }
      }
      is CardGripDrag.Released -> {
        val drop = libraryDrop ?: return
        val inside = drop.target.contains(event.center)
        if (BuildConfig.DEBUG) Log.i("ViraDrop", "released inside=$inside title=${card.title.id}")
        if (inside && !card.libraryBusy) {
          libraryDropState = LibraryDropState.Saving
          // Offset the feedback so the released card does not cover it.
          drop.panel.setComponent(Transform(drop.target.pose * Pose(Vector3(-0.28f, 0.12f, -0.06f))))
          writeCardLibrary(card, "add", { add(it) }, drop)
        } else {
          hideLibraryDrop()
        }
      }
      CardGripDrag.Cancelled -> {
        if (BuildConfig.DEBUG) Log.i("ViraDrop", "cancelled title=${card.title.id}")
        hideLibraryDrop()
      }
    }
  }

  private fun hideLibraryDrop() {
    libraryDropFeedback?.cancel()
    libraryDropFeedback = null
    val drop = libraryDrop ?: return
    libraryDrop = null
    libraryDropState = null
    drop.panel.destroy()
  }

  private fun animeCardSettings(): UIPanelSettings = UIPanelSettings(
      shape = QuadShapeOptions(width = AnimeCardHinge.leafWidth, height = AnimeCardHinge.leafHeight),
      style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
      display = DpPerMeterDisplayOptions(dpPerMeter = 1000f),
  )

  private fun createFixedLibrary() {
    if (libraryPanel != null) return
    // Keep the shelf close to the fixed entry point, on the right and slightly tilted upward.
    val position = Vector3(-0.59f, 1.32f, 1.62f)
    val facing = Quaternion.lookRotation(position - Vector3(0f, 1.55f, 2f))
    libraryPanel = Entity.create(listOf(Panel(R.id.library_panel), Transform(Pose(position, facing))))
    watchLibraryCategory(libraryStatus)
  }

  private fun watchLibraryCategory(status: LibraryStatus) {
    libraryCategoryJob?.cancel()
    libraryStatus = status
    libraryRows = null
    libraryFailed = false
    libraryCategoryJob = activityScope.launch {
      try {
        libraryRepository.observeByStatus(status).distinctUntilChanged().collect { entries ->
          libraryRows = entries.map { entry ->
            val title = try {
              catalogRepository.findById(entry.titleId)
            } catch (_: CatalogUnavailableException) {
              null
            }
            LibraryPanelRow(entry, title)
          }
          if (BuildConfig.DEBUG) Log.i("ViraLibrary", "display category=$status rows=${entries.size}")
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: Exception) {
        libraryFailed = true
        if (BuildConfig.DEBUG) Log.w("ViraLibrary", "display_failed type=${failure.javaClass.simpleName}")
      }
    }
  }

  override fun registerPanels(): List<PanelRegistration> {
    return listOf(
        ComposeViewPanelRegistration(
            R.id.panel,
            composeViewCreator = { entity, ctx ->
              entryScreen = entity
              ComposeView(ctx).apply {
                setContent {
                  val generation = entryInputGeneration
                  key(generation) {
                    PosterPulseTheme {
                      EntryLogoPanel {
                        if (generation == entryInputGeneration) showTools("screen_tools")
                      }
                    }
                  }
                }
              }
            },
            settingsCreator = {
              UIPanelSettings(
                   shape = QuadShapeOptions(width = MAIN_SCREEN_WIDTH, height = MAIN_SCREEN_HEIGHT),
                  style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                  display = DpPerMeterDisplayOptions(),
              )
            },
        ),
        VideoSurfacePanelRegistration(
            R.id.demo_video_panel,
            surfaceConsumer = { entity, surface ->
              runOnUiThread {
                if (demoVideo == entity) demoVideoPlayer.attachSurface(surface)
              }
            },
            settingsCreator = {
              MediaPanelSettings(
                  shape = QuadShapeOptions(width = MAIN_SCREEN_WIDTH, height = MAIN_SCREEN_HEIGHT),
                  display = PixelDisplayOptions(width = 1280, height = 720),
                  rendering = MediaPanelRenderOptions(zIndex = -1),
              )
            },
        ),
        ComposeViewPanelRegistration(
            R.id.demo_video_controls,
            composeViewCreator = { entity, ctx ->
              ComposeView(ctx).apply {
                setContent {
                  val state = demoVideoPlayer.state
                  val generation = entryInputGeneration
                  fun acceptsInput() = toolsInputAvailable && demoControls == entity &&
                      generation == entryInputGeneration
                  key(generation) {
                    DemoPlaybackControls(
                        isPlaying = state.phase == DemoVideoPhase.Playing,
                        isEnded = state.phase == DemoVideoPhase.Ended,
                        positionMs = state.positionMs.toLong(),
                        durationMs = state.durationMs.toLong(),
                        isLoading = state.phase == DemoVideoPhase.Loading,
                        error = if (state.phase == DemoVideoPhase.Error) "Could not play the local clip" else null,
                        inputEnabled = toolsInputAvailable,
                        isCardVisible = animeCard != null,
                        onPlayPause = { if (acceptsInput()) demoVideoPlayer.togglePlayback() },
                        onSeek = { position -> if (acceptsInput()) demoVideoPlayer.seekTo(position.toInt()) },
                        onToggleCard = { if (acceptsInput()) toggleDemoCard("transport_card") },
                        onClose = { if (acceptsInput()) stopDemoPlayback(restoreCard = true) },
                    )
                  }
                }
              }
            },
            settingsCreator = {
              UIPanelSettings(
                  shape = QuadShapeOptions(width = 0.68f, height = 0.18f),
                  style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                  display = DpPerMeterDisplayOptions(dpPerMeter = 1100f),
              )
            },
        ),
        ComposeViewPanelRegistration(
            R.id.tools_panel,
            composeViewCreator = { entity, ctx ->
              ComposeView(ctx).apply {
                setContent {
                  DiscoverPanel(
                      repository = catalogRepository,
                      preservingSearchEditor = preservingSearchEditor,
                      onRecenter = { recenterTools(entity) },
                      onClose = {
                        if (toolsInputAvailable && toolsPanel == entity) hideTools("panel_close")
                      },
                      onTitleSelected = { title -> showAnimeCard(title, entity) },
                      onSearchFocusChanged = { focused ->
                        if (toolsPanel == entity) {
                          searchFieldFocused = focused
                          if (BuildConfig.DEBUG) Log.i("ViraTools", "search_focus=$focused")
                        }
                      },
                  )
                }
              }
            },
            settingsCreator = {
              UIPanelSettings(
                  shape = QuadShapeOptions(width = 0.66f, height = 0.41f),
                  style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                  display = DpPerMeterDisplayOptions(dpPerMeter = 1100f),
              )
            },
        ),
        ComposeViewPanelRegistration(
            R.id.library_panel,
            composeViewCreator = { entity, ctx ->
              ComposeView(ctx).apply {
                setContent {
                  val generation = entryInputGeneration
                  key(generation) {
                    LibraryPanel(
                        selectedStatus = libraryStatus,
                        rows = libraryRows,
                        failed = libraryFailed,
                        inputEnabled = toolsInputAvailable,
                        onSelectStatus = { status ->
                          if (toolsInputAvailable && libraryPanel == entity &&
                              generation == entryInputGeneration && libraryStatus != status) {
                            watchLibraryCategory(status)
                          }
                        },
                        onRetry = {
                          if (toolsInputAvailable && libraryPanel == entity &&
                              generation == entryInputGeneration && libraryFailed) {
                            watchLibraryCategory(libraryStatus)
                          }
                        },
                        onTitleSelected = { title ->
                          if (generation == entryInputGeneration &&
                              libraryRows?.any { it.entry.titleId == title.id } == true) {
                            showAnimeCard(title, entity)
                          }
                        },
                    )
                  }
                }
              }
            },
            settingsCreator = {
              UIPanelSettings(
                  shape = QuadShapeOptions(width = 0.57f, height = 0.36f),
                  style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                  display = DpPerMeterDisplayOptions(dpPerMeter = 1450f),
              )
            },
        ),
        ComposeViewPanelRegistration(
            R.id.library_drop_panel,
            composeViewCreator = { _, ctx ->
              ComposeView(ctx).apply {
                setContent { libraryDropState?.let { LibraryDropPanel(it) } }
              }
            },
            settingsCreator = {
              UIPanelSettings(
                  shape = QuadShapeOptions(width = LibraryDropTarget.width, height = LibraryDropTarget.height),
                  style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                  display = DpPerMeterDisplayOptions(dpPerMeter = 1200f),
              )
            },
        ),
        ComposeViewPanelRegistration(
            R.id.anime_card_cover,
            composeViewCreator = { entity, ctx ->
              ComposeView(ctx).apply {
                setContent {
                  val card = animeCard
                  if (card != null && card.cover == entity) {
                    val generation = card.actionGeneration
                    AnimeCardCover(
                        title = card.title,
                        expanded = card.expanded,
                        gripHeld = card.gripHeld,
                        actionGeneration = generation,
                        onToggle = {
                          if (toolsInputAvailable && animeCard === card && card.acceptsAction(generation)) {
                            card.toggle()
                          }
                        },
                        onClose = {
                          if (toolsInputAvailable && animeCard === card && card.acceptsAction(generation)) {
                            hideAnimeCard("card_close")
                          }
                        },
                    )
                  }
                }
              }
            },
            settingsCreator = { animeCardSettings() },
        ),
        ComposeViewPanelRegistration(
            R.id.anime_card_inside,
            composeViewCreator = { entity, ctx ->
              ComposeView(ctx).apply {
                setContent {
                  val card = animeCard
                  if (card != null && card.inside == entity) {
                    val generation = card.actionGeneration
                    AnimeCardInside(
                        title = card.title,
                        episodes = card.episodes,
                        selectedEpisodeId = card.selectedEpisodeId,
                        gripHeld = card.gripHeld,
                        actionGeneration = generation,
                        onEpisodeSelected = { id ->
                          if (toolsInputAvailable && animeCard === card && card.inside == entity) {
                            card.selectEpisode(id, generation)
                          }
                        },
                        onPlayDemo = if (demoVideo == null) {
                          { startDemoPlayback(card, generation) }
                        } else {
                          null
                        },
                        libraryEntry = card.libraryEntry,
                        libraryLoading = card.libraryLoading,
                        libraryBusy = card.libraryBusy,
                        libraryError = card.libraryError,
                        onLibraryAdd = { changeCardLibrary(card, generation, "add") { add(it) } },
                        onLibraryStatus = { status ->
                          changeCardLibrary(card, generation, "status") { setStatus(it, status) }
                        },
                        onLibraryAdjustProgress = { delta ->
                          changeCardLibrary(card, generation, "progress") { adjustProgress(it, delta) }
                        },
                        onLibraryRating = { rating ->
                          changeCardLibrary(card, generation, "rating") { setRating(it, rating) }
                        },
                        onLibraryRemove = { changeCardLibrary(card, generation, "remove") { remove(it) } },
                        onLibraryRetry = {
                          if (toolsInputAvailable && animeCard === card && card.expanded &&
                              !card.libraryBusy && card.acceptsAction(generation)) watchCardLibrary(card)
                        },
                    )
                  }
                }
              }
            },
            settingsCreator = { animeCardSettings() },
        ),
    )
  }
}
