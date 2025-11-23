package xyz.jdubiel.migawka

import android.util.Log
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.time.Instant

data class RemoteImage(
    var bytes: ByteArray,
    val date: Instant,
    val sha256: Sha256
)

data class IPEndpoint(
    val ip: String,
    val port: Int
) {
    init {
        require(ip.isNotBlank()) { "ip must not be blank" }
        require(port in 0..65535) { "port must be in 0..65535" }
    }

    override fun toString(): String = "$ip:$port"
}


class RemoteImageProvider(private val endpoint: IPEndpoint) { // TODO: make it a singleton (?)

    // TODO: gracefully shutdown the channel when the instance gets removed?
    private val channel: ManagedChannel =
        ManagedChannelBuilder.forAddress(endpoint.ip, endpoint.port)
            .usePlaintext() // TODO: don't use plaintext!
            .build()
    private val stub: MigawkaGrpcKt.MigawkaCoroutineStub =
        MigawkaGrpcKt.MigawkaCoroutineStub(channel)

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
                        sha256 = Sha256.fromHex(it.id),
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

    suspend fun getImage(id: Sha256): RemoteImage {
        val request = GetMediaItemRequest.newBuilder()
            .setId(id.toHex())
            .build()

        val response = stub.getOptimizedMediaItem(request)

        if (response.status.code != 200) {
            Log.e("gRPC", "Error: `${response.status.message}`")
            throw Exception("Error: `${response.status.message}`")
        }

        if (response.mediaItem.id != id.toHex()) {
            Log.e("gRPC", "getImage: returned MediaItemID is different from requested!")
        }

        return RemoteImage(
            sha256 = id,
            bytes = response.mediaItem.content.toByteArray(),
            date = Instant.parse(response.mediaItem.creationTime)
        )
    }
}