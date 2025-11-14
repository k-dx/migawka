package xyz.jdubiel.migawka

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.launch
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme

enum class MigawkaScreen {
    Second,
    Gallery,
    SingleMediaView
}


@Composable
fun MigawkaApp(
    navController: NavHostController = rememberNavController(),
    imageGalleryViewModel: ImageGalleryViewModel = viewModel()
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val initialImageIdArg = "initialImageId"

        NavHost(
            navController = navController,
            startDestination = MigawkaScreen.Gallery.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = MigawkaScreen.Gallery.name) {
                Migawka(
                    onSettingsButtonClick = { navController.navigate(MigawkaScreen.Second.name) },
                    viewModel = imageGalleryViewModel,
                    onImageClick = { imageId: String ->
                        Log.d(TAG, "onImageClick, imageId = $imageId")
                        navController.navigate("${MigawkaScreen.SingleMediaView.name}/$imageId")
                    }
                )
            }

            composable(route = MigawkaScreen.Second.name) {
                SecondScreen(content = "Second screen! Yay!")
            }

            composable(
                route = "${MigawkaScreen.SingleMediaView.name}/{$initialImageIdArg}",
                arguments = listOf(navArgument(initialImageIdArg) { type = NavType.StringType })
            ) { backStackEntry ->
                val initialImageId = backStackEntry.arguments
                    ?.getString(initialImageIdArg)
                if (initialImageId != null) {
                    SingleMediaViewScreen(
                        viewModel = imageGalleryViewModel,
                        initialImageId = Sha256.fromHex(initialImageId)
                    )
                } else {
                    Log.e("SingleMediaViewScreen", "initialImageId is null")
                    Text("Error: initialImageId is null. The image could not be displayed.")
                }
            }
        }
    }
}

@Composable
fun Migawka(
    viewModel: ImageGalleryViewModel,
    onImageClick: (String) -> Unit,
    onSettingsButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onSettingsButtonClick() }) {
                Text(text = stringResource(R.string.settings))
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

                val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

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
            onImageClick = {}
        )
    }
}