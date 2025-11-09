package xyz.jdubiel.migawka

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.launch

@Composable
fun SingleMediaViewScreen(
    viewModel: ImageListViewModel,
    initialIndex: Int,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Get the list of images from the ViewModel's state
    val images = viewModel.uiState.images

    // Create a PagerState. It controls the pager and knows the current page.
    val pagerState = rememberPagerState(
        initialPage = if (initialIndex in images.indices) initialIndex else 0,
        pageCount = { images.size }
    )

    // A one-time effect to scroll to the initial page.
    // This is good practice if the initialIndex could change.
    LaunchedEffect(initialIndex) {
        pagerState.scrollToPage(initialIndex)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // The HorizontalPager composable, which provides the swipe functionality.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            // This is the content for a single page. It gets called for each visible page.
            // We get the URI for the current page and display it.
            val imageUri = images[pageIndex]
            AsyncImage(
                model = imageUri,
                contentDescription = "Full screen image",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit // Fit ensures the whole image is visible
            )
        }

        Button(onClick = {
            val serverAddress = "192.168.5.158"
            Log.d("serverAddress", serverAddress)
            val channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
                .usePlaintext()
                .build()

            val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

            coroutineScope.launch {
                try {
                    val request = FileUploadRequest.newBuilder()
                        .setFilename("test.jpg")
                        .setContent(ByteString.copyFrom(byteArrayOf(1, 2, 3)))
                        .build()

                    val response = stub.uploadFile(request)

                    // Update the UI with the response on the main thread
                    Log.i("gRPC", "Response: ${response.message}")

                } catch (e: Exception) {
                    Log.e("gRPC", "Error: ${e.message}", e)
                }
            }

        }) {
            Text("Upload")
        }
    }
}




