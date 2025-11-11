package xyz.jdubiel.migawka

import android.net.Uri
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
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.launch

@Composable
fun SingleMediaViewScreen(
    viewModel: ImageGalleryViewModel,
    initialImageUri: Uri,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val images = viewModel.imageStream.collectAsLazyPagingItems()

    val initialPage = images.itemSnapshotList.items.indexOf(initialImageUri)

    val pagerState = rememberPagerState(
        initialPage = if (initialPage != -1) initialPage else 0,
        pageCount = { images.itemCount }
    )

    LaunchedEffect(initialPage) {
        if (initialPage != -1) {
            pagerState.scrollToPage(initialPage)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val imageUri = images[pageIndex]
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Full screen image",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
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
