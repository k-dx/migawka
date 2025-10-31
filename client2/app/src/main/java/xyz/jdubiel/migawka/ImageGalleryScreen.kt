package xyz.jdubiel.migawka

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

// Displays a gallery grid with images. Assumes the permission is already granted.
@Composable
fun ImageGalleryScreen(
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageListViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            ImageGrid(
                images = uiState.images,
                onImageClick = onImageClick
            )
        }
    }
}

@Composable
fun ImageGrid(
    images: List<Uri>,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
            items(images.size) { index ->
                val imageUri = images[index]
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Gallery Image",
                    modifier = Modifier
                        .aspectRatio(1f) // Make it square
                        .fillMaxWidth()
                        .clickable { onImageClick(index) },
                    contentScale = ContentScale.Crop // Crop to fill the square
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImageGallery() {
    ImageGalleryScreen(onImageClick = {})
}
