package xyz.jdubiel.migawka.data.database

class LocalMediaRepository(
    private val localMediaDao: LocalMediaEntryDao
) : ILocalMediaRepository {
    override suspend fun getAllEntries() = localMediaDao.getAll()

    override suspend fun insertEntry(entry: LocalMediaEntry) = localMediaDao.insert(entry)

    override suspend fun deleteEntry(entry: LocalMediaEntry) = localMediaDao.delete(entry)
}
