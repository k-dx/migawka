package xyz.jdubiel.migawka

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun ImageGalleryScreen(modifier: Modifier = Modifier, viewModel: ImageListViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val coroutineScope = rememberCoroutineScope()

    // Determine the correct permission to request based on Android version
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // State to track if we have the permission
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permissionToRequest
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                // Permission granted, load images
                coroutineScope.launch {
                    viewModel.loadImages(context)
                }
            }
        }
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hasPermission) {
            // Permission is granted, show the button to load images
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.loadImages(context)
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Load Images from MediaStore")
            }

            // Display loading indicator or the image grid
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                ImageGrid(images = uiState.images)
            }
        } else {
            // Permission not granted, show a button to request it
            Button(onClick = { permissionLauncher.launch(permissionToRequest) }) {
                Text("Request Media Permission")
            }
        }
    }
}

@Composable
fun ImageGrid(images: List<Uri>, modifier: Modifier = Modifier) {
    if (images.isEmpty()) {
        Text("No images found.", modifier = modifier.padding(16.dp))
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(images) { imageUri ->
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Gallery Image",
                    modifier = Modifier
                        .aspectRatio(1f) // Make it square
                        .fillMaxWidth(),
                    contentScale = ContentScale.Crop // Crop to fill the square
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImageGallery() {
    ImageGalleryScreen()
}
