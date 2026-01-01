package xyz.jdubiel.migawka.data.database

import android.net.Uri
import xyz.jdubiel.migawka.data.Hash
import java.time.Instant

class LocalMediaRepository(
    private val localMediaDao: LocalMediaEntryDao
) : ILocalMediaRepository {
    override suspend fun getAllEntries() = localMediaDao.getAll()

    override suspend fun getEntriesBeforeTimestamp(count: Int, imagesBefore: Instant) =
        localMediaDao.getEntriesBeforeTimestamp(count, imagesBefore)

    override suspend fun getByHash(hash: Hash) = localMediaDao.getByHash(hash)

    override suspend fun insertEntry(entry: LocalMediaEntry) = localMediaDao.insert(entry)

    override suspend fun insertEntries(entries: List<LocalMediaEntry>) =
        localMediaDao.insert(entries)

    override suspend fun deleteEntry(entry: LocalMediaEntry) = localMediaDao.delete(entry)

    override suspend fun delete(uris: List<Uri>) = localMediaDao.delete(uris)

    override suspend fun deleteAll() = localMediaDao.deleteAll()


}
