package xyz.jdubiel.migawka.ui.imageGallery

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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.paging.insertSeparators
import androidx.paging.map
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.data.PagedImage
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface ImageGalleryTimelineEntry {
    data class ImageItem(val image: PagedImage) : ImageGalleryTimelineEntry
    data class MonthHeader(val monthYear: String) : ImageGalleryTimelineEntry
}

private fun getMonthYearHeaderIfNeeded(
    before: PagedImage?,
    after: PagedImage?
): ImageGalleryTimelineEntry.MonthHeader? {
    val locale = Locale.getDefault()
    val zone = java.time.ZoneId.systemDefault()
    val monthYearFormatter = DateTimeFormatter
        .ofPattern("LLLL uuuu") // LLLL gives non-conjugated month name 'listopad' instead of 'listopada'
        .withLocale(locale)
        .withZone(zone)

    if (after == null) {
        // No item after, so no header needed
        return null
    }
    if (before == null) {
        // First item in the list, always show a header
        val header = monthYearFormatter.format(after.date)
        return ImageGalleryTimelineEntry.MonthHeader(header)

    }

    val beforeMonthYear = monthYearFormatter.format(before.date)
    val afterMonthYear = monthYearFormatter.format(after.date)

    if (beforeMonthYear != afterMonthYear) {
        return ImageGalleryTimelineEntry.MonthHeader(afterMonthYear)
    }

    return null
}

// Displays a gallery grid with images. Assumes the permission is already granted.
@Composable
fun ImageGalleryScreen(
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageGalleryViewModel = viewModel()
) {
    // collectAsLazyPagingItems is an extension function from the
    // androidx.paging:paging-compose library. Its job is to collect the
    // PagingData from the imageStream and convert it into a
    // LazyPagingItems<Uri> object. This object is what connects the Paging
    // library's data loading mechanism with Jetpack Compose's lazy layouts.
    val images = remember(viewModel.imageStream) {
        viewModel.imageStream
            .map { pagingData ->
                pagingData.map { pagedImage ->
                    ImageGalleryTimelineEntry.ImageItem(pagedImage)
                }
            }
            .map { pagingData ->
                pagingData.insertSeparators { before: ImageGalleryTimelineEntry.ImageItem?, after: ImageGalleryTimelineEntry.ImageItem? ->
                    getMonthYearHeaderIfNeeded(before?.image, after?.image)
                }
            }
    }.collectAsLazyPagingItems() // Now this collects from a stable Flow
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
//    Log.d(TAG, "JpgFromBytes, jpgBytes.size = ${jpgBytes.size}, jpgBytes = $jpgBytes")

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
    images: LazyPagingItems<ImageGalleryTimelineEntry>,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Error loading images.", modifier = modifier.padding(16.dp))
            }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(
                    count = images.itemCount,
                    // TODO: use hash as key but make sure they are unique first
                    // also having hash collision should be handled gracefully
//                    key = { index ->
//                        when (val item = images.peek(index)) {
//                            is ImageGalleryTimelineEntry.ImageItem -> {
//                                when(item.image) {
//                                    is PagedImage.FromUri -> item.image.id.toHex()
//                                    is PagedImage.FromBytes -> item.image.id.toHex()
//                                }
//                            }
//                            is ImageGalleryTimelineEntry.MonthHeader -> item.monthYear
//                            null -> "placeholder_$index"
//                        }
//                    },
                    span = { index ->
                        when (images.peek(index)) {
                            is ImageGalleryTimelineEntry.MonthHeader -> GridItemSpan(maxLineSpan)
                            else -> GridItemSpan(1)
                        }
                    }
                ) { index ->
                    val uiModel = images[index]
                    if (uiModel != null) {
                        when (uiModel) {
                            is ImageGalleryTimelineEntry.MonthHeader -> {
                                Text(
                                    text = uiModel.monthYear,
                                    modifier = Modifier
                                        .padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
                                )
                            }
                            is ImageGalleryTimelineEntry.ImageItem -> {
                                when (val image = uiModel.image) {
                                    is PagedImage.FromUri -> {
                                        AsyncImage(
                                            model = image.contentUri,
                                            contentDescription = "Gallery Image",
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .fillMaxWidth()
                                                .clickable { onImageClick(image.id.toHex()) },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    is PagedImage.FromBytes -> {
                                        AsyncImage(
                                            model = image.bytes,
                                            contentDescription = "Gallery Image",
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .fillMaxWidth()
                                                .clickable { onImageClick(image.id.toHex()) },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImageGallery() {
    // This preview will be empty as it doesn't have access to a real ViewModel
    // You can create a fake ViewModel for preview purposes if needed.
    ImageGalleryScreen(onImageClick = {})
}
