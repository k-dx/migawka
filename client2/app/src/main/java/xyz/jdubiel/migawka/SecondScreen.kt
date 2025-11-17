package xyz.jdubiel.migawka

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

fun shutdownChannel(channel: ManagedChannel) {
    try {
        channel.shutdown()
    } catch (e: InterruptedException) {
        Log.e("gRPC__", "Error shutting down channel: ${e.message}")
        channel.shutdownNow();
        Thread.currentThread().interrupt();
    }
}

@Composable
fun SecondScreen(content: String) {

    val serverAddress = "192.168.5.158"
    Log.d("serverAddress", serverAddress)
    val channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
        .usePlaintext()
        .build()


    Text(content)
    GrpcImage(
        imageId = "0b8512120df51731b619a06b537668d9b58625904f1f780abdf585a0d8863ee6",
        stubProvider = {
            MigawkaGrpcKt.MigawkaCoroutineStub(channel)
        },
        shutdownChannel = {
            shutdownChannel(channel)
        }
    )
}


/**
 * Suspend helper to fetch image bytes via gRPC stub.
 * - stub: generated coroutine stub (ImageServiceCoroutineStub or Kotlin coroutine stub).
 * - imageId: identifier for the image request.
 * - ioDispatcher: optional dispatcher for blocking IO/decoding.
 */
suspend fun fetchImageBytesGrpc(
    stub: MigawkaGrpcKt.MigawkaCoroutineStub,
    imageId: String,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): ByteArray = withContext(ioDispatcher) {
    val request = GetMediaItemRequest.newBuilder()
        .setId(imageId)
        .build()

    val response = stub.getOptimizedMediaItem(request) // suspend RPC
    response.mediaItem.content.toByteArray() // ByteString -> ByteArray
}

/**
 * Decode PNG/JPEG bytes to ImageBitmap on IO dispatcher.
 */
suspend fun decodeToImageBitmap(bytes: ByteArray, ioDispatcher: CoroutineDispatcher = Dispatchers.IO): ImageBitmap? =
    withContext(ioDispatcher) {
        val bmp = BitmapFactory.decodeStream(ByteArrayInputStream(bytes)) ?: return@withContext null
        bmp.asImageBitmap()
    }

/**
 * Composable that fetches image via gRPC and displays a loading indicator.
 * - stubProvider: lambda that returns a coroutine stub instance (avoid creating channel/stub per composition).
 */
@Composable
fun GrpcImage(
    imageId: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 200.dp,
    stubProvider: () -> MigawkaGrpcKt.MigawkaCoroutineStub,
    contentScale: ContentScale = ContentScale.Crop,
    shutdownChannel: () -> Unit = {}
) {
    var loading by remember { mutableStateOf(true) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Trigger fetch once per imageId
    LaunchedEffect(imageId) {
        loading = true
        error = null
        bitmap = null
        try {
            val stub = stubProvider()
            val bytes = fetchImageBytesGrpc(stub, imageId)
            shutdownChannel()
            val decoded = decodeToImageBitmap(bytes)
            if (decoded == null) {
                error = "Failed to decode image"
            } else {
                bitmap = decoded
            }
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    Box(
//        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading -> {
                CircularProgressIndicator()
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            else -> {
                // Simple error UI with retry
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error ?: "Failed to load image")
                    IconButton(onClick = {
                        // Retry by triggering LaunchedEffect: change key via remembered counter
                        // Simple approach: call a mutable state to force re-run
                        // But we don't ask user; use side-effect: increment a counter remembered outside.
                    }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                            contentDescription = "Retry"
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SecondScreenPreview() {
    SecondScreen("Second screen!")
}