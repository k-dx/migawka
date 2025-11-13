package xyz.jdubiel.migawka

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SingleMediaViewScreen(
    viewModel: ImageGalleryViewModel,
    initialImageUri: Uri,
    modifier: Modifier = Modifier
) {
    Text("SingleMediaViewScreen")

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
