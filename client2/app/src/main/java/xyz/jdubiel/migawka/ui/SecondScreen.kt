package xyz.jdubiel.migawka.ui

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import xyz.jdubiel.migawka.GetMediaItemRequest
import xyz.jdubiel.migawka.MigawkaGrpcKt

suspend fun get(stub: MigawkaGrpcKt.MigawkaCoroutineStub, request: GetMediaItemRequest) {
    val response = stub.getThumbnail(request)

}
@Composable
fun SecondScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                val id = "0a6d07f8121e5aca"
                val serverAddress = "192.168.5.158"
                Log.d("serverAddress", serverAddress)
                val channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
                    .usePlaintext()
                    .build()
                try {
                    val stub = MigawkaGrpcKt.MigawkaCoroutineStub(channel)

                    val request = GetMediaItemRequest.newBuilder()
                        .setId(id)
                        .build()

                    val deferreds = (1..10).map {
                        async {
                            try {
                                val response = stub.getThumbnail(request)
                                Log.d("gRPC", "response $it: $response")
                                response
                            } catch (e: Exception) {
                                Log.e("gRPC", "request $it failed: ${e.message}")
                                null
                            }
                    }
                }
                    val responses = deferreds.awaitAll()
                    Log.d(
                        "gRPC",
                        "All done. Successful responses: ${responses.count { it != null }}"
                    )
                    responses.forEachIndexed { index, response ->
                        assert(responses[0] == response)

                    }

                } catch (e: Exception) {
                    Log.e("SecondScreen", e.message ?: "Unknown error")
                } finally {
                    channel.shutdown()
            }
        }
        }) {
            Text(text = "send requests")
    }
    }
}

