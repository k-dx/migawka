package xyz.jdubiel.migawka.data.database

import android.net.Uri
import java.time.Instant

interface ILocalMediaRepository {
    suspend fun getAllEntries(): List<LocalMediaEntry>
    suspend fun getEntriesBeforeTimestamp(count: Int, imagesBefore: Instant): List<LocalMediaEntry>
    suspend fun getByHash(hash: String): LocalMediaEntry?
    suspend fun insertEntry(entry: LocalMediaEntry)
    suspend fun insertEntries(entries: List<LocalMediaEntry>)
    suspend fun deleteEntry(entry: LocalMediaEntry)
    suspend fun delete(uris: List<Uri>)
    suspend fun deleteAll()

}