package xyz.jdubiel.migawka.data.database

import android.net.Uri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.jdubiel.migawka.data.Hash
import java.time.Instant

@Dao
interface LocalMediaEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(localMediaEntry: LocalMediaEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(localMediaEntries: List<LocalMediaEntry>)

    @Delete
    suspend fun delete(localMediaEntry: LocalMediaEntry)

    @Query("DELETE FROM localMedia WHERE uri IN (:uris)")
    suspend fun delete(uris: List<Uri>)

    @Query("DELETE FROM localMedia")
    suspend fun deleteAll()

    @Query("SELECT * FROM localMedia ORDER BY uri ASC")
    suspend fun getAll(): List<LocalMediaEntry>

    @Query("SELECT * FROM localMedia WHERE date < :imagesBefore ORDER BY date DESC LIMIT :count")
    suspend fun getEntriesBeforeTimestamp(count: Int, imagesBefore: Instant): List<LocalMediaEntry>

    @Query("SELECT * FROM localMedia WHERE hash = :hash")
    suspend fun getByHash(hash: Hash): LocalMediaEntry?
}

