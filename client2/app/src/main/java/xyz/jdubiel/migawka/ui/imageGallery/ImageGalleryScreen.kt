package xyz.jdubiel.migawka.ui.imageGallery

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.data.PagedImage
import xyz.jdubiel.migawka.data.TimelineEntry
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface ImageGalleryTimelineEntry {
    data class ImageItem(val entry: TimelineEntry) : ImageGalleryTimelineEntry
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
    // TODO: insert month-year headers (separators)
//    val entries = remember(viewModel.timelineEntries) {
//        viewModel.timelineEntries.map { ImageGalleryTimelineEntry.ImageItem(it) }
//    }

    val entries = viewModel.entries.collectAsState().value.map { ImageGalleryTimelineEntry.ImageItem(it) }

    Log.d(TAG, "entries.size = ${entries.size}")

    ImageGrid(
        entries = entries,
        onImageClick = onImageClick,
        modifier = modifier
    )
}

@Composable
fun ImageGrid(
    entries: List<ImageGalleryTimelineEntry>,
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
        items(
            count = entries.size,
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
                when (entries[index]) {
                    is ImageGalleryTimelineEntry.MonthHeader -> GridItemSpan(maxLineSpan)
                    else -> GridItemSpan(1)
                }
            }
        ) { index ->
            val uiModel = entries[index]
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
                        when (val item = uiModel.entry) {
                            is TimelineEntry.Local -> {
                                AsyncImage(
                                    model = item.contentUri,
                                    contentDescription = "Gallery Image",
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                        .clickable { onImageClick(item.id.toHex()) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                            is TimelineEntry.Remote -> {
                                // TODO: download the image and show it
//                                AsyncImage(
//                                    model = image.bytes,
//                                    contentDescription = "Gallery Image",
//                                    modifier = Modifier
//                                        .aspectRatio(1f)
//                                        .fillMaxWidth()
//                                        .clickable { onImageClick(image.id.toHex()) },
//                                    contentScale = ContentScale.Crop
//                                )
                                Box(modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(color = Color(0xFFA89B32))) {
                                    Box(modifier = Modifier.padding(2.dp)) {
                                        Text("${item.id}")
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
