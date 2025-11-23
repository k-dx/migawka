package xyz.jdubiel.migawka.data.uriToSha256

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UriToSha256EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(uriToSha256Entry: UriToSha256Entry)

    @Delete
    suspend fun delete(uriToSha256Entry: UriToSha256Entry)

    @Query("SELECT * FROM uriToSha256 ORDER BY uri ASC")
    suspend fun getAll(): List<UriToSha256Entry>
}

