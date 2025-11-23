package xyz.jdubiel.migawka

import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.launch
import xyz.jdubiel.migawka.ui.navigation.MigawkaNavHost
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme


@Composable
fun MigawkaApp(
    navController: NavHostController = rememberNavController(),
    imageGalleryViewModel: ImageGalleryViewModel = viewModel()
) {
    // TODO: probably MikawkaNavHost should be moved outside Scaffold (?)
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        MigawkaNavHost(
            navController = navController,
            imageGalleryViewModel = imageGalleryViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun Migawka(
    viewModel: ImageGalleryViewModel,
    onImageClick: (String) -> Unit,
    onSettingsButtonClick: () -> Unit,
    onSecondScreenButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
//            .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { onSettingsButtonClick() }) {
                Text(text = stringResource(R.string.settings))
            }
            Button(onClick = { onSecondScreenButtonClick() }) {
                Text(text = "Second screen")
            }
            Button(onClick = {
                val serverAddress = "192.168.5.158"
                Log.d("serverAddress", serverAddress)
                val channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
                    .usePlaintext()
                    .build()

                val stub = MigawkaGrpcKt.MigawkaCoroutineStub(channel)

                coroutineScope.launch {
                    try {
                        val request = FileDownloadRequest.newBuilder()
                            .setFilename("test.jpg")
                            .build()

                        val response = stub.downloadFile(request)

                        // Update the UI with the response on the main thread
                        Log.i(
                            "gRPC",
                            "Response: ${response.filename} ${response.message} ${response.content}"
                        )

                    } catch (e: Exception) {
                        Log.e("gRPC", "Error: ${e.message}", e)
                    }
                }
            }) {
                Text(text = "Download")
            }
            Button(onClick = {
                val serverAddress = "192.168.5.158"
                Log.d("serverAddress", serverAddress)
                val channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
                    .usePlaintext()
                    .build()

                val stub = MigawkaGrpcKt.MigawkaCoroutineStub(channel)

                coroutineScope.launch {
                    try {
                        val request = ThumbnailsTimestampRequest.newBuilder()
                            .setTimestamp("2026-01-01T00:00:00Z")
                            .setCount(10)
                            .build()

                        val response = stub.getThumbnailsBeforeTimestamp(request)

                        // Update the UI with the response on the main thread
                        Log.i(
                            "gRPC",
                            "Response: ${response.status}"
                        )

                        response.thumbnailsList.forEach {
                            Log.i("gRPC", "Thumbnail: ${it.creationTime} ${it.id}")
                        }

                    } catch (e: Exception) {
                        Log.e("gRPC", "Error: ${e.message}", e)
                    }
                }
            }) {
                Text(text = "Get thumbnails")
            }
        }
        GalleryPermissionWrapper(
            viewModel = viewModel,
            onImageClick = onImageClick
        )
    }

}

@Preview(showBackground = true)
@Composable
fun MigawkaPreview() {
    MigawkaTheme {
        Migawka(
            viewModel = viewModel<ImageGalleryViewModel>(),
            onSettingsButtonClick = {},
            onSecondScreenButtonClick = {},
            onImageClick = {}
        )
    }
}