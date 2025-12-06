package xyz.jdubiel.migawka.data

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.DirectoryEntry
import xyz.jdubiel.migawka.GetFileListPageRequest
import xyz.jdubiel.migawka.MigawkaGrpcKt


class RemoteFileExplorer { // TODO: make it a singleton

    // TODO: gracefully shutdown the channel when the instance gets removed?
    // TODO: move channel management to another class created at app startup
    private val channel: ManagedChannel
    private val stub: MigawkaGrpcKt.MigawkaCoroutineStub


    init {
        // TODO: don't hardcode IP or PORT
        val serverAddress = "192.168.5.158"
        channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
            .usePlaintext() // TODO: don't use plaintext!
            .build()

        stub = MigawkaGrpcKt.MigawkaCoroutineStub(channel)
    }

    suspend fun getFileList(path: String, pageNumber: Int, pageSize: Int): List<DirectoryEntry> {
        try {
            val request = GetFileListPageRequest.newBuilder()
                .setPath(path)
                .setPageNumber(pageNumber)
                .setPageSize(pageSize)
                .build()

            val response = stub.getFileListPage(request)

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
        return listOf<DirectoryEntry>()
    }
}

const val FILE_PAGING_TAG = "FilePaging"

class FolderEntriesPagingSource(val path: String, private val pageSize: Int = 30) : PagingSource<Int, DirectoryEntry>() {
    private val fileExplorer = RemoteFileExplorer()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DirectoryEntry> =
        withContext(Dispatchers.IO) {
            Log.d(FILE_PAGING_TAG, "load with params: loadSize = ${params.loadSize}, key = ${params.key}")

            val pageNumber = params.key?: 0

            val remoteDirectoryEntries = async {
                fileExplorer.getFileList(path, pageNumber, pageSize)
            }

            val entries = remoteDirectoryEntries.await()

            val prevKey = if (pageNumber > 0) pageNumber - 1 else null
            val nextKey = if (entries.isEmpty()) null else pageNumber + 1


            LoadResult.Page(
                data = entries,
                prevKey = prevKey,
                nextKey = nextKey
            )
        }

    // from https://developer.android.com/reference/kotlin/androidx/paging/PagingSource
    override fun getRefreshKey(state: PagingState<Int, DirectoryEntry>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }
}
