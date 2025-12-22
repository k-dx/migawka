package xyz.jdubiel.migawka.data.database

//import kotlin.jvm.java

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Database class with a singleton Instance object.
 */
@Database(entities = [LocalMediaEntry::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LocalMediaDatabase : RoomDatabase() {
    abstract fun localMediaDao(): LocalMediaEntryDao

    companion object {
        @Volatile
        private var Instance: LocalMediaDatabase? = null

        fun getDatabase(context: Context): LocalMediaDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    LocalMediaDatabase::class.java,
                    "local_media_database"
                )
                    // when database schema changes, drop all tables
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also {Instance = it}}
        }
    }
}