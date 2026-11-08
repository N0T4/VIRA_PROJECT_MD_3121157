package com.meta.spatial.samples.startersample.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class LibraryDao {
  @Query("SELECT * FROM library_entries WHERE title_id = :titleId")
  abstract fun observe(titleId: String): Flow<LibraryEntry?>

  @Query("SELECT * FROM library_entries WHERE status = :status ORDER BY title_id")
  abstract fun observeByStatus(status: LibraryStatus): Flow<List<LibraryEntry>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  abstract suspend fun insert(entry: LibraryEntry): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  protected abstract suspend fun insertCompletedEpisode(episode: CompletedEpisode): Long

  @Query("UPDATE library_entries SET status = :status WHERE title_id = :titleId")
  abstract suspend fun updateStatus(titleId: String, status: LibraryStatus): Int

  @Query("UPDATE library_entries SET rating = :rating WHERE title_id = :titleId")
  abstract suspend fun updateRating(titleId: String, rating: Int?): Int

  @Query("UPDATE library_entries SET progress = MAX(0, MIN(2147483647, progress + :delta)) WHERE title_id = :titleId")
  abstract suspend fun adjustProgress(titleId: String, delta: Int): Int

  @Query("DELETE FROM library_entries WHERE title_id = :titleId")
  abstract suspend fun delete(titleId: String): Int

  @Transaction
  open suspend fun recordCompletedEpisode(titleId: String, episodeId: String): Boolean {
    insert(LibraryEntry(titleId, LibraryStatus.Watching))
    if (insertCompletedEpisode(CompletedEpisode(titleId, episodeId)) == -1L) return false
    check(adjustProgress(titleId, 1) == 1)
    return true
  }
}

internal class LibraryRepository(private val dao: LibraryDao) {
  fun observe(titleId: String): Flow<LibraryEntry?> = dao.observe(titleId)

  fun observeByStatus(status: LibraryStatus): Flow<List<LibraryEntry>> = dao.observeByStatus(status)

  suspend fun add(titleId: String): Boolean {
    require(titleId.isNotBlank()) { "Library title ID must not be blank" }
    return dao.insert(LibraryEntry(titleId)) != -1L
  }

  suspend fun setStatus(titleId: String, status: LibraryStatus): Boolean =
      dao.updateStatus(titleId, status) > 0

  suspend fun setRating(titleId: String, rating: Int?): Boolean {
    require(rating == null || rating in 1..10) { "Rating must be between 1 and 10" }
    return dao.updateRating(titleId, rating) > 0
  }

  suspend fun recordCompletedEpisode(titleId: String, episodeId: String): Boolean {
    require(titleId.isNotBlank()) { "Library title ID must not be blank" }
    require(episodeId.isNotBlank()) { "Episode ID must not be blank" }
    return dao.recordCompletedEpisode(titleId, episodeId)
  }

  suspend fun adjustProgress(titleId: String, delta: Int): Boolean =
      dao.adjustProgress(titleId, delta) > 0

  suspend fun remove(titleId: String): Boolean = dao.delete(titleId) > 0
}
