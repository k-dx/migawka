package xyz.jdubiel.migawka.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import xyz.jdubiel.migawka.data.uriToSha256.UriToSha256Entry
import xyz.jdubiel.migawka.data.uriToSha256.UriToSha256EntryDao

/**
 * Database class with a singleton Instance object.
 */
@Database(entities = [UriToSha256Entry::class], version = 1, exportSchema = false)
abstract class UriToSha256Database : RoomDatabase() {
    abstract fun uriToSha256Dao(): UriToSha256EntryDao

    companion object {
        @Volatile
        private var Instance: UriToSha256Database? = null

        fun getDatabase(context: Context): UriToSha256Database {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    UriToSha256Database::class.java,
                    "uri_to_sha256_database"
                )
                    // when database schema changes, drop all tables
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also {Instance = it}}

            }
    }
}