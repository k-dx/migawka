package xyz.jdubiel.migawka.data

import android.util.Log
import xyz.jdubiel.migawka.GetMediaItemRequest
import xyz.jdubiel.migawka.MigawkaGrpcKt
import xyz.jdubiel.migawka.TimelineEntriesRequest
import xyz.jdubiel.migawka.data.network.GrpcResult
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

class RemoteImageProvider(private val stub: MigawkaGrpcKt.MigawkaCoroutineStub) {
    suspend fun getThumbnailImage(id: Hash): GrpcResult<RemoteImage> {
        try {
            val request = GetMediaItemRequest.newBuilder()
                .setId(id.toString())
                .build()

            val response = stub.getThumbnail(request)

            if (response.status.code != 200) {
                val message = response.status.message
                Log.e("gRPC", "getThumbnailImage: `$message`")
                return GrpcResult.Error(message = message)
            }

            if (response.mediaItem.id != id.toString()) {
                val message = "Returned MediaItemID is different from requested!"
                Log.e("gRPC", "getThumbnailImage: $message")
                return GrpcResult.Error(message = message)
            }

            return GrpcResult.Success(RemoteImage(
                hash = id,
                bytes = response.mediaItem.content.toByteArray(),
                date = Instant.parse(response.mediaItem.creationTime)
            ))
        } catch (e: Exception) {
            Log.e("gRPC", "getThumbnailImage: ${e.message}", e)
            return GrpcResult.Error(message = e.message ?: "Unknown error", throwable = e)
        }
    }

    suspend fun getOptimizedImage(id: Hash): GrpcResult<RemoteImage> {
        try {
            val request = GetMediaItemRequest.newBuilder()
                .setId(id.toString())
                .build()

            val response = stub.getOptimizedMediaItem(request)

            if (response.status.code != 200) {
                val message = response.status.message
                Log.e("gRPC", "getOptimizedImage: `$message`")
                return GrpcResult.Error(message = message)
            }

            if (response.mediaItem.id != id.toString()) {
                val message = "Returned MediaItemID is different from requested!"
                Log.e("gRPC", "getOptimizedImage: $message")
                return GrpcResult.Error(message = message)
            }

            return GrpcResult.Success(RemoteImage(
                hash = id,
                bytes = response.mediaItem.content.toByteArray(),
                date = Instant.parse(response.mediaItem.creationTime)
            ))
        } catch (e: Exception) {
            Log.e("gRPC", "getOptimizedImage: ${e.message}", e)
            return GrpcResult.Error(message = e.message ?: "Unknown error", throwable = e)
        }
    }

    suspend fun getFullImage(id: Hash): GrpcResult<RemoteFullImage> {
        try {
            val request = GetMediaItemRequest.newBuilder()
                .setId(id.toString())
                .build()

            val response = stub.getFullMediaItem(request)

            if (response.status.code != 200) {
                val message = response.status.message
                Log.e("gRPC", "getFullImage: `$message`")
                return GrpcResult.Error(message = message)
            }

            if (response.mediaItem.id != id.toString()) {
                val message = "Returned MediaItemID is different from requested!"
                Log.e("gRPC", "getFullImage: $message")
                return GrpcResult.Error(message = message)
            }

            return GrpcResult.Success(RemoteFullImage(
                hash = id,
                bytes = response.mediaItem.content.toByteArray(),
                date = Instant.parse(response.mediaItem.creationTime),
                path = response.mediaItem.path
            ))
        } catch (e: Exception) {
            Log.e("gRPC", "getFullImage: ${e.message}", e)
            return GrpcResult.Error(message = e.message ?: "Unknown error", throwable = e)
        }

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
                Log.e("gRPC", "getEntries: $message")
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
            Log.e("gRPC", "getEntries: ${e.message}", e)
            return GrpcResult.Error(message = e.message ?: "Unknown error", throwable = e)
        }
    }
}