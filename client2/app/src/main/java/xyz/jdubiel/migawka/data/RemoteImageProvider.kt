package xyz.jdubiel.migawka.data

import android.util.Log
import xyz.jdubiel.migawka.GetMediaItemRequest
import xyz.jdubiel.migawka.MigawkaGrpcKt
import xyz.jdubiel.migawka.TimelineEntriesRequest
import xyz.jdubiel.migawka.hasher
import java.time.Instant

data class RemoteImage(
    var bytes: ByteArray,
    val date: Instant,
    val hash: Hash,
)

data class RemoteFullImage(
    var bytes: ByteArray,
    val date: Instant,
    val hash: Hash,
    val path: String
)

sealed interface GrpcResult<out T> {
    data class Success<out T>(val data: T) : GrpcResult<T>
    data class Error(val message: String, val throwable: Throwable? = null) : GrpcResult<Nothing>
}

class RemoteImageProvider(private val stub: MigawkaGrpcKt.MigawkaCoroutineStub) {
    suspend fun getThumbnailImage(id: Hash): RemoteImage {
        val request = GetMediaItemRequest.newBuilder()
            .setId(id.toString())
            .build()

        val response = stub.getThumbnail(request)

        if (response.status.code != 200) {
            Log.e("gRPC", "Error: `${response.status.message}`")
            throw Exception("Error: `${response.status.message}`")
        }

        if (response.mediaItem.id != id.toString()) {
            Log.e("gRPC", "getThumbnailImage: returned MediaItemID is different from requested!")
        }

        return RemoteImage(
            hash = id,
            bytes = response.mediaItem.content.toByteArray(),
            date = Instant.parse(response.mediaItem.creationTime)
        )
    }

    suspend fun getOptimizedImage(id: Hash): RemoteImage {
        val request = GetMediaItemRequest.newBuilder()
            .setId(id.toString())
            .build()

        val response = stub.getOptimizedMediaItem(request)

        if (response.status.code != 200) {
            Log.e("gRPC", "Error: `${response.status.message}`")
            throw Exception("Error: `${response.status.message}`")
        }

        if (response.mediaItem.id != id.toString()) {
            Log.e("gRPC", "getOptimizedImage: returned MediaItemID is different from requested!")
        }

        return RemoteImage(
            hash = id,
            bytes = response.mediaItem.content.toByteArray(),
            date = Instant.parse(response.mediaItem.creationTime)
        )
    }

    suspend fun getFullImage(id: Hash): RemoteFullImage {
        val request = GetMediaItemRequest.newBuilder()
            .setId(id.toString())
            .build()

        val response = stub.getFullMediaItem(request)

        if (response.status.code != 200) {
            Log.e("gRPC", "Error: `${response.status.message}`")
            throw Exception("Error: `${response.status.message}`")
        }

        if (response.mediaItem.id != id.toString()) {
            Log.e("gRPC", "getFullImage: returned MediaItemID is different from requested!")
        }

        return RemoteFullImage(
            hash = id,
            bytes = response.mediaItem.content.toByteArray(),
            date = Instant.parse(response.mediaItem.creationTime),
            path = response.mediaItem.path
        )
    }

    suspend fun getEntries(): GrpcResult<List<TimelineEntryK>> {
        try {
            val results = mutableListOf<TimelineEntryK.Remote>()
            val request = TimelineEntriesRequest.newBuilder()
                .build()

            val response = stub.getTimelineEntries(request)

            Log.i("gRPC","Response: ${response.status}")

            if (response.status.code != 200) {
                val message = response.status.message
                Log.e("gRPC", "Error in response: $message")
                return GrpcResult.Error(message = message)
            }

            response.entriesList.forEach {
                val date = Instant.parse(it.creationTime)
                Log.i("gRPC", "TimelineEntry: ${it.creationTime} $date ${it.id}")

                results.add(
                    TimelineEntryK.Remote(
                        id = hasher.fromString(it.id),
                        date = date
                    )
                )
            }
            return GrpcResult.Success(results)

        } catch (e: Exception) {
            Log.e("gRPC", "Error: ${e.message}", e)
            return GrpcResult.Error(message = e.message ?: "Unknown error", throwable = e)
        }
    }
}