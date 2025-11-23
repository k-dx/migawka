package xyz.jdubiel.migawka.data.uriToSha256

class UriToSha256Repository(
    private val uriToSha256Dao: UriToSha256EntryDao
) : IUriToSha256Repository {
    override suspend fun getAllEntries() = uriToSha256Dao.getAll()

    override suspend fun insertEntry(entry: UriToSha256Entry) = uriToSha256Dao.insert(entry)

    override suspend fun deleteEntry(entry: UriToSha256Entry) = uriToSha256Dao.delete(entry)
}
