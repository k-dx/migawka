package xyz.jdubiel.migawka.data

import android.util.Log
import xyz.jdubiel.migawka.DirectoryEntry
import xyz.jdubiel.migawka.GetFileListRequest
import xyz.jdubiel.migawka.MigawkaGrpcKt
import xyz.jdubiel.migawka.hasher
import java.time.Instant

class RemoteFileExplorer(private val stub: MigawkaGrpcKt.MigawkaCoroutineStub) {
    private suspend fun _getDirectoryEntries(path: String): List<DirectoryEntry> {
        try {
            val request = GetFileListRequest.newBuilder()
                .setPath(path)
                .build()

            val response = stub.getFileList(request)

            Log.i(
                "gRPC",
                "Response: ${response.status}"
            )

            if (response.status.code == 200) {
                return response.entriesList
            } else {
                Log.e("gRPC", "Error: `${response.status.message}`")
            }

        } catch (e: Exception) {
            Log.e("gRPC", "Error: ${e.message}", e)
            // TODO: this probably should throw
        }
        return listOf()
    }

    suspend fun getDirectoryEntries(path: String): List<DirectoryEntryK> {
        val entries = _getDirectoryEntries(path)
        return entries.map(::convert)
    }

    private fun convert(entry: DirectoryEntry): DirectoryEntryK {
        return when (entry.type) {
            DirectoryEntry.FileType.DIRECTORY -> {
                DirectoryEntryK.Directory(entry.name)
            }

            DirectoryEntry.FileType.MEDIA -> {
                DirectoryEntryK.Image(
                    entry.name,
                    hasher.fromString(entry.media.id),
                    Instant.parse(entry.media.creationTime)
                )
            }

            DirectoryEntry.FileType.OTHER -> throw Exception("Other file type not supported")
            DirectoryEntry.FileType.UNRECOGNIZED -> throw Exception("Unknown file type")
        }
    }

}