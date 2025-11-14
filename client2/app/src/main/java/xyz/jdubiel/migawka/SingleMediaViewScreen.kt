package xyz.jdubiel.migawka

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage

@Composable
fun SingleMediaViewScreen(
    viewModel: ImageGalleryViewModel,
    initialImageId: Sha256,
    modifier: Modifier = Modifier
) {
    // 1. Trigger the data load when the screen is first composed
    LaunchedEffect(initialImageId) {
        viewModel.loadImageById(initialImageId)
    }

    // 2. Collect the state from the ViewModel
    val imageDetails by viewModel.selectedImage.collectAsState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 3. Display loading, error, or image based on the state
        if (imageDetails == null) {
            // You might want a more nuanced check for loading vs. error
            CircularProgressIndicator()
        } else {
            AsyncImage(
                model = imageDetails!!.contentUri,
                contentDescription = "Full-screen image",
                modifier = Modifier
            )
            // You can also display other info like the SHA256 hash
            Text(text = "SHA256: ${imageDetails!!.sha256.toHex()}")
            Row() {
                Button(onClick = {}) {
                    Text("Previous")
                }
                Button(onClick = {}) {
                    Text("Next")
                }
            }
        }
    }


    // TODO: change this to accommodate for remote images

//    val coroutineScope = rememberCoroutineScope()
//
//    val images = viewModel.imageStream.collectAsLazyPagingItems()
//
//    val initialPage = images.itemSnapshotList.items.indexOfFirst{ initialImageUri == it.contentUri }
//
//    val pagerState = rememberPagerState(
//        initialPage = if (initialPage != -1) initialPage else 0,
//        pageCount = { images.itemCount }
//    )
//
//    LaunchedEffect(initialPage) {
//        if (initialPage != -1) {
//            pagerState.scrollToPage(initialPage)
//        }
//    }
//
//    Column(
//        modifier = modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Bottom
//    ) {
//        HorizontalPager(
//            state = pagerState,
//            modifier = Modifier.weight(1f)
//        ) { pageIndex ->
//            val imageUri: Uri? = images[pageIndex]?.contentUri
//            if (imageUri != null) {
//                AsyncImage(
//                    model = imageUri,
//                    contentDescription = "Full screen image",
//                    modifier = Modifier.fillMaxWidth(),
//                    contentScale = ContentScale.Fit
//                )
//            }
//        }
//
//        Button(onClick = {
//            val serverAddress = "192.168.5.158"
//            Log.d("serverAddress", serverAddress)
//            val channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
//                .usePlaintext()
//                .build()
//
//            val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)
//
//            coroutineScope.launch {
//                try {
//                    val request = FileUploadRequest.newBuilder()
//                        .setFilename("test.jpg")
//                        .setContent(ByteString.copyFrom(byteArrayOf(1, 2, 3)))
//                        .build()
//
//                    val response = stub.uploadFile(request)
//
//                    Log.i("gRPC", "Response: ${response.message}")
//
//                } catch (e: Exception) {
//                    Log.e("gRPC", "Error: ${e.message}", e)
//                }
//            }
//
//        }) {
//            Text("Upload")
//        }
//    }
}
