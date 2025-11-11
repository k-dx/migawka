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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage

// Displays a gallery grid with images. Assumes the permission is already granted.
@Composable
fun ImageGalleryScreen(
    onImageClick: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageGalleryViewModel = viewModel()
) {
    val images = viewModel.imageStream.collectAsLazyPagingItems()

    ImageGrid(
        images = images,
        onImageClick = onImageClick,
        modifier = modifier
    )
}

@Composable
fun ImageGrid(
    images: LazyPagingItems<Uri>,
    onImageClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(images.itemCount) { index ->
            val imageUri = images[index]
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Gallery Image",
                    modifier = Modifier
                        .aspectRatio(1f) // Make it square
                        .fillMaxWidth()
                        .clickable { onImageClick(imageUri) },
                    contentScale = ContentScale.Crop // Crop to fill the square
                )
            }
        }
    }

    when (images.loadState.refresh) {
        is LoadState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is LoadState.Error -> {
            Text("Error loading images.", modifier = modifier.padding(16.dp))
        }
        else -> {}
    }
}

@Preview(showBackground = true)
@Composable
fun ImageGallery() {
    // This preview will be empty as it doesn't have access to a real ViewModel
    // You can create a fake ViewModel for preview purposes if needed.
    ImageGalleryScreen(onImageClick = {})
}
