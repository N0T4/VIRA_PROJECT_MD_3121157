package com.meta.spatial.samples.startersample.library

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryPersistenceTest {
  @Test
  fun duplicateAddAndProgressSurviveDiskReopen() = runBlocking<Unit> {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val databaseName = "t040-library-${UUID.randomUUID()}.db"
    val titleId = "demo-t040-persistence"
    val expected = LibraryEntry(titleId, LibraryStatus.Watching, 2)
    try {
      val database = LibraryDatabase.open(context, databaseName)
      try {
        val repository = LibraryRepository(database.entries())
        assertNull(repository.observe(titleId).first())
        val additions = listOf(async { repository.add(titleId) }, async { repository.add(titleId) }).awaitAll()
        assertEquals(1, additions.count { it })
        assertTrue(repository.setStatus(titleId, LibraryStatus.Watching))
        assertTrue(repository.adjustProgress(titleId, -1))
        assertEquals(0, repository.observe(titleId).first()?.progress)
        assertTrue(repository.adjustProgress(titleId, Int.MAX_VALUE))
        assertTrue(repository.adjustProgress(titleId, 1))
        assertEquals(Int.MAX_VALUE, repository.observe(titleId).first()?.progress)
        assertTrue(repository.adjustProgress(titleId, -Int.MAX_VALUE))
        val adjustments = listOf(
            async { repository.adjustProgress(titleId, 1) },
            async { repository.adjustProgress(titleId, 1) },
        ).awaitAll()
        assertTrue(adjustments.all { it })
        assertFalse(repository.add(titleId))
        assertEquals(expected, repository.observe(titleId).first())
      } finally {
        database.close()
      }

      val reopened = LibraryDatabase.open(context, databaseName)
      try {
        val repository = LibraryRepository(reopened.entries())
        assertEquals(expected, repository.observe(titleId).first())
        assertFalse(repository.add(titleId))
        assertEquals(expected, repository.observe(titleId).first())
        assertTrue(repository.remove(titleId))
      } finally {
        reopened.close()
      }

      val afterRemoval = LibraryDatabase.open(context, databaseName)
      try {
        assertNull(LibraryRepository(afterRemoval.entries()).observe(titleId).first())
      } finally {
        afterRemoval.close()
      }
    } finally {
      context.deleteDatabase(databaseName)
    }
  }

  @Test
  fun statusChangeUpdatesBothCategoryObservers() = runBlocking<Unit> {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val databaseName = "t041-library-${UUID.randomUUID()}.db"
    val database = LibraryDatabase.open(context, databaseName)
    val titleId = "demo-t041-categories"
    try {
      val repository = LibraryRepository(database.entries())
      assertTrue(repository.add(titleId))
      assertTrue(repository.adjustProgress(titleId, 2))

      withTimeout(10_000) {
        val initial = CompletableDeferred<Pair<List<LibraryEntry>, List<LibraryEntry>>>()
        val moved = async {
          combine(
              repository.observeByStatus(LibraryStatus.Planning),
              repository.observeByStatus(LibraryStatus.Watching),
              ::Pair,
          ).onEach { initial.complete(it) }
              .first { (planning, watching) -> planning.isEmpty() && watching.isNotEmpty() }
        }

        val (initialPlanning, initialWatching) = initial.await()
        assertEquals(listOf(LibraryEntry(titleId, LibraryStatus.Planning, 2)), initialPlanning)
        assertTrue(initialWatching.isEmpty())
        assertTrue(repository.setStatus(titleId, LibraryStatus.Watching))
        assertEquals(listOf(LibraryEntry(titleId, LibraryStatus.Watching, 2)), moved.await().second)
        assertTrue(repository.observeByStatus(LibraryStatus.Completed).first().isEmpty())
      }
    } finally {
      database.close()
      context.deleteDatabase(databaseName)
    }
  }

  @Test
  fun versionOneMigrationPreservesStatusAndProgressAndAddsClearableRating() = runBlocking<Unit> {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val databaseName = "t052-migration-${UUID.randomUUID()}.db"
    val titleId = "legacy-title"
    val legacy = LibraryEntry(titleId, LibraryStatus.Completed, 8)
    try {
      // Recreate the shipped v1 schema and Room identity from schemas/.../1.json.
      context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
        database.execSQL(
            "CREATE TABLE library_entries (title_id TEXT NOT NULL, status TEXT NOT NULL, " +
                "progress INTEGER NOT NULL, PRIMARY KEY(title_id))",
        )
        database.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        database.execSQL(
            "INSERT INTO room_master_table (id, identity_hash) VALUES (42, 'd364e0457cbc01eadb6829494066953e')",
        )
        database.execSQL(
            "INSERT INTO library_entries (title_id, status, progress) VALUES (?, ?, ?)",
            arrayOf<Any>(titleId, legacy.status.storageId, legacy.progress),
        )
        database.version = 1
      }

      withLibrary(context, databaseName) { repository ->
        assertEquals(legacy, repository.observe(titleId).first())
        assertTrue(repository.setRating(titleId, 1))
        assertTrue(repository.setRating(titleId, 10))
        for (invalid in listOf(0, 11)) {
          assertTrue(runCatching { repository.setRating(titleId, invalid) }.exceptionOrNull() is IllegalArgumentException)
        }
        assertEquals(legacy.copy(rating = 10), repository.observe(titleId).first())
      }

      withLibrary(context, databaseName) { repository ->
        assertEquals(legacy.copy(rating = 10), repository.observe(titleId).first())
        assertTrue(repository.setRating(titleId, null))
        assertEquals(legacy, repository.observe(titleId).first())
      }
    } finally {
      context.deleteDatabase(databaseName)
    }
  }

  @Test
  fun completedEpisodeCountsOnceAcrossConcurrentWritesAndReopen() = runBlocking<Unit> {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val databaseName = "t052-completion-${UUID.randomUUID()}.db"
    val titleId = "rated-title"
    val episodeId = "source:season1:episode1"
    val newTitleId = "unsaved-title"
    val updated = LibraryEntry(titleId, LibraryStatus.Completed, 9, rating = 7)
    try {
      withLibrary(context, databaseName) { repository ->
        assertTrue(repository.add(titleId))
        assertTrue(repository.setStatus(titleId, LibraryStatus.Completed))
        assertTrue(repository.adjustProgress(titleId, 8))
        assertTrue(repository.setRating(titleId, 7))
        val completions = List(4) { async { repository.recordCompletedEpisode(titleId, episodeId) } }.awaitAll()
        assertEquals(1, completions.count { it })
        assertEquals(updated, repository.observe(titleId).first())

        assertTrue(repository.recordCompletedEpisode(newTitleId, episodeId))
        assertEquals(LibraryEntry(newTitleId, LibraryStatus.Watching, 1), repository.observe(newTitleId).first())
      }

      withLibrary(context, databaseName) { repository ->
        assertFalse(repository.recordCompletedEpisode(titleId, episodeId))
        assertEquals(updated, repository.observe(titleId).first())
        assertTrue(repository.remove(titleId))
        assertTrue(repository.recordCompletedEpisode(titleId, episodeId))
        assertEquals(LibraryEntry(titleId, LibraryStatus.Watching, 1), repository.observe(titleId).first())
      }
    } finally {
      context.deleteDatabase(databaseName)
    }
  }

  private suspend fun withLibrary(
      context: Context,
      databaseName: String,
      action: suspend (LibraryRepository) -> Unit,
  ) {
    val database = LibraryDatabase.open(context, databaseName)
    try {
      action(LibraryRepository(database.entries()))
    } finally {
      database.close()
    }
  }
}
