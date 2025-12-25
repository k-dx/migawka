package xyz.jdubiel.migawka.ui.imageGallery

import android.util.Log
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.coil3.GrpcThumbnail

// Displays a gallery grid with images. Assumes the permission is already granted.
@Composable
fun ImageGalleryScreen(
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageGalleryViewModel = viewModel()
) {
    val entries by viewModel.entriesWithHeaders.collectAsState()

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
    if (entries.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    }

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
                    is ImageGalleryTimelineEntry.Header -> GridItemSpan(maxLineSpan)
                    else -> GridItemSpan(1)
                }
            }
        ) { index ->
            val uiModel = entries[index]
            if (uiModel != null) {
                when (uiModel) {
                    is ImageGalleryTimelineEntry.Header -> {
                        Text(
                            text = uiModel.monthYear,
                            modifier = Modifier
                                .padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                    is ImageGalleryTimelineEntry.ImageItem -> {
                        when (val item = uiModel.entry) {
                            is TimelineEntryK.Local -> {
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
                            is TimelineEntryK.Remote -> {
                                AsyncImage(
                                    model = GrpcThumbnail(item.id),
                                    contentDescription = "Gallery Image",
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                        .clickable { onImageClick(item.id.toHex()) },
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

@Preview(showBackground = true)
@Composable
fun ImageGallery() {
    // This preview will be empty as it doesn't have access to a real ViewModel
    // You can create a fake ViewModel for preview purposes if needed.
    ImageGalleryScreen(onImageClick = {})
}
