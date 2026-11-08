package com.meta.spatial.samples.startersample.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

internal enum class LibraryStatus(val storageId: String) {
  Planning("planning"),
  Watching("watching"),
  Completed("completed"),
}

@Entity(tableName = "library_entries")
internal data class LibraryEntry(
    @PrimaryKey @ColumnInfo(name = "title_id") val titleId: String,
    val status: LibraryStatus = LibraryStatus.Planning,
    val progress: Int = 0,
    val rating: Int? = null,
)

@Entity(
    tableName = "completed_episodes",
    primaryKeys = ["title_id", "episode_id"],
    foreignKeys = [
      ForeignKey(
          entity = LibraryEntry::class,
          parentColumns = ["title_id"],
          childColumns = ["title_id"],
          onDelete = ForeignKey.CASCADE,
      ),
    ],
)
internal data class CompletedEpisode(
    @ColumnInfo(name = "title_id") val titleId: String,
    @ColumnInfo(name = "episode_id") val episodeId: String,
)

internal class LibraryStatusConverter {
  @TypeConverter
  fun toStorage(status: LibraryStatus): String = status.storageId

  @TypeConverter
  fun fromStorage(id: String): LibraryStatus = LibraryStatus.entries.single { it.storageId == id }
}
