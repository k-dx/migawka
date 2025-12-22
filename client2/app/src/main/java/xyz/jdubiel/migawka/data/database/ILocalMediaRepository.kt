package xyz.jdubiel.migawka.data.database

interface ILocalMediaRepository {
    suspend fun getAllEntries(): List<LocalMediaEntry>
    suspend fun insertEntry(entry: LocalMediaEntry)
    suspend fun deleteEntry(entry: LocalMediaEntry)
}