package xyz.jdubiel.migawka.data.uriToSha256

interface IUriToSha256Repository {
    suspend fun getAllEntries(): List<UriToSha256Entry>
    suspend fun insertEntry(entry: UriToSha256Entry)
    suspend fun deleteEntry(entry: UriToSha256Entry)
}