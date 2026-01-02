package xyz.jdubiel.migawka.data

import android.util.Log
import xyz.jdubiel.migawka.DirectoryEntry
import xyz.jdubiel.migawka.GetFileListRequest
import xyz.jdubiel.migawka.MigawkaGrpcKt
import xyz.jdubiel.migawka.data.network.GrpcResult
import xyz.jdubiel.migawka.hasher
import java.time.Instant

class RemoteFileExplorer(private val stub: MigawkaGrpcKt.MigawkaCoroutineStub) {
    private suspend fun _getDirectoryEntries(path: String): GrpcResult<List<DirectoryEntry>> {
        try {
            val request = GetFileListRequest.newBuilder()
                .setPath(path)
                .build()

            val response = stub.getFileList(request)

            if (response.status.code != 200) {
                val message = response.status.message
                Log.e("gRPC", "_getDirectoryEntries: `$message`")
                return GrpcResult.Error(message = message)
            }

            return GrpcResult.Success(response.entriesList)
        } catch (e: Exception) {
            Log.e("gRPC", "_getDirectoryEntries: ${e.message}", e)
            return GrpcResult.Error(message = e.message ?: "Unknown error", throwable = e)
        }
    }

    suspend fun getDirectoryEntries(path: String): GrpcResult<List<DirectoryEntryK>> {
        return when (val result = _getDirectoryEntries(path)) {
            is GrpcResult.Success -> GrpcResult.Success(result.data.map(::convert))
            is GrpcResult.Error -> result
        }
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