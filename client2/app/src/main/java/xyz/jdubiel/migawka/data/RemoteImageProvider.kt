package xyz.jdubiel.migawka.data

import android.util.Log
import xyz.jdubiel.migawka.GetMediaItemRequest
import xyz.jdubiel.migawka.MigawkaGrpcKt
import xyz.jdubiel.migawka.ThumbnailsTimestampRequest
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
    suspend fun getThumbnailsBeforeTimestamp(timestamp: Instant, count: Int): List<RemoteImage> {
        val remoteImages = mutableListOf<RemoteImage>()

        try {
            val request = ThumbnailsTimestampRequest.newBuilder()
                .setTimestamp(timestamp.toString())
                .setCount(count)
                .build()

            val response = stub.getThumbnailsBeforeTimestamp(request)

            // Update the UI with the response on the main thread
            Log.i(
                "gRPC",
                "Response: ${response.status}"
            )

            response.thumbnailsList.forEach {
                val date = Instant.parse(it.creationTime)
                Log.i("gRPC", "Thumbnail: ${it.creationTime} $date ${it.id}")

                remoteImages.add(
                    RemoteImage(
                        hash = hasher.fromHex(it.id),
                        bytes = it.content.toByteArray(),
                        date = date
                    )
                )
            }

        } catch (e: Exception) {
            Log.e("gRPC", "Error: ${e.message}", e)
            // TODO: this probably should throw
        }
        return remoteImages
    }

    suspend fun getOptimizedImage(id: Hash): RemoteImage {
        val request = GetMediaItemRequest.newBuilder()
            .setId(id.toHex())
            .build()

        val response = stub.getOptimizedMediaItem(request)

        if (response.status.code != 200) {
            Log.e("gRPC", "Error: `${response.status.message}`")
            throw Exception("Error: `${response.status.message}`")
        }

        if (response.mediaItem.id != id.toHex()) {
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
            .setId(id.toHex())
            .build()

        val response = stub.getFullMediaItem(request)

        if (response.status.code != 200) {
            Log.e("gRPC", "Error: `${response.status.message}`")
            throw Exception("Error: `${response.status.message}`")
        }

        if (response.mediaItem.id != id.toHex()) {
            Log.e("gRPC", "getFullImage: returned MediaItemID is different from requested!")
        }

        return RemoteFullImage(
            hash = id,
            bytes = response.mediaItem.content.toByteArray(),
            date = Instant.parse(response.mediaItem.creationTime),
            path = response.mediaItem.path
        )
    }
}