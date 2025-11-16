package xyz.jdubiel.migawka

import android.util.Log
import io.grpc.ManagedChannelBuilder

class Utils {
    companion object {
        suspend fun fetchImageBytesGrpc(id: Sha256): MediaItem {
            val serverAddress = "192.168.5.158"
            Log.d("serverAddress", serverAddress)
            val channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
                    .usePlaintext()
                    .build()

            try {
                val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)
                val request = GetMediaItemRequest.newBuilder()
                        .setId(id.toHex())
                        .build()

                val response = stub.getMediaItem(request)

                // Update the UI with the response on the main thread
                Log.i(
                        "gRPC__",
                        "Response for full image: ${response.status}"
                )

                return response.mediaItem

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch(e: Exception) {
                Log.e("gRPC__", "Error: ${e.message}", e)
                throw e
            } finally {
                try {
                    channel.shutdown()
                } catch (e: InterruptedException) {
                    Log.e("gRPC__", "Error shutting down channel: ${e.message}")
                    channel.shutdownNow()
                    Thread.currentThread().interrupt()
                }
            }
        }
    }
}