package com.meta.spatial.samples.startersample.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LibraryEntry::class, CompletedEpisode::class], version = 2)
@TypeConverters(LibraryStatusConverter::class)
internal abstract class LibraryDatabase : RoomDatabase() {
  abstract fun entries(): LibraryDao

  companion object {
    @Volatile private var instance: LibraryDatabase? = null

    private val migration1To2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE library_entries ADD COLUMN rating INTEGER")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS completed_episodes (
                title_id TEXT NOT NULL,
                episode_id TEXT NOT NULL,
                PRIMARY KEY(title_id, episode_id),
                FOREIGN KEY(title_id) REFERENCES library_entries(title_id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
      }
    }

    fun getInstance(context: Context): LibraryDatabase = instance ?: synchronized(this) {
      instance ?: open(context, "vira-library.db").also { instance = it }
    }

    fun open(context: Context, name: String): LibraryDatabase =
        Room.databaseBuilder(context.applicationContext, LibraryDatabase::class.java, name)
            .addMigrations(migration1To2)
            .build()
  }
}
