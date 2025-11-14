package xyz.jdubiel.migawka

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Displays a gallery grid with images. Assumes the permission is already granted.
@Composable
fun ImageGalleryScreen(
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageGalleryViewModel = viewModel()
) {
    // This is an extension function from the androidx.paging:paging-compose
    // library. Its job is to collect the PagingData from the imageStream and
    // convert it into a LazyPagingItems<Uri> object. This object is what
    // connects the Paging library's data loading mechanism with Jetpack
    // Compose's lazy layouts.
    val images = viewModel.imageStream.collectAsLazyPagingItems()
    Log.d(TAG, "images.itemCount = ${images.itemCount}")

    ImageGrid(
        images = images,
        onImageClick = onImageClick,
        modifier = modifier
    )
}



// TODO: consider changing this to Coil library
@Composable
fun JpgFromBytes(jpgBytes: ByteArray, modifier: Modifier = Modifier) {
    Log.d(TAG, "JpgFromBytes, jpgBytes.size = ${jpgBytes.size}, jpgBytes = $jpgBytes")

//    val bitmap = remember(jpgBytes) {
//        BitmapFactory.decodeByteArray(jpgBytes, 0, jpgBytes.size)
//    }
//    if (bitmap != null) {
//        Image(
//            bitmap = bitmap.asImageBitmap(),
//            contentDescription = null,
//            modifier = modifier,
//            contentScale = ContentScale.Crop
//        )
//    }

    val bitmap by produceState<Bitmap?>(initialValue = null, jpgBytes) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(jpgBytes, 0, jpgBytes.size)
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun ImageGrid(
    images: LazyPagingItems<PagedImage>,
    onImageClick: (String) -> Unit,
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
            val image = images[index]

            when (image) {
                is PagedImage.FromUri -> {
                    val imageUri = image.contentUri
                    val imageId = image.id
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Gallery Image",
                        modifier = Modifier
                            .aspectRatio(1f) // Make it square
                            .fillMaxWidth()
                            .clickable { onImageClick(imageId.toHex()) },
                        contentScale = ContentScale.Crop // Crop to fill the square
                    )
                }
                is PagedImage.FromBytes -> {
                    val imageId = image.id
                    JpgFromBytes(image.bytes, modifier = Modifier
                        .aspectRatio(1f) // Make it square
                        .fillMaxWidth()
                        .clickable { onImageClick(imageId.toHex()) }
                    )
                }
                null -> {
                    Log.e(TAG, "ImageGrid: images[$index] == null")
                }
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
