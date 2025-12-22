package xyz.jdubiel.migawka.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalMediaEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(localMediaEntry: LocalMediaEntry)

    @Delete
    suspend fun delete(localMediaEntry: LocalMediaEntry)

    @Query("SELECT * FROM localMedia ORDER BY uri ASC")
    suspend fun getAll(): List<LocalMediaEntry>
}

