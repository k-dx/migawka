package xyz.jdubiel.migawka.ui.imageGallery

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.coil3.GrpcThumbnail
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

// Displays a gallery grid with images. Assumes the permission is already granted.
@Composable
fun ImageGalleryScreen(
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageGalleryViewModel = viewModel()
) {
    val entries by viewModel.entriesWithHeaders.collectAsState()
    val fetchErr by viewModel.fetchErr.collectAsState()

    Log.d(TAG, "entries.size = ${entries.size} (including headers)")

    if (fetchErr != null) {
        Box(
            modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error)
        ) {
            Text(
                text = "Connection error: ${fetchErr?.message}",
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(4.dp)
            )
        }
    }

    ImageGrid(
        entries = entries,
        onImageClick = onImageClick,
        modifier = modifier
    )
}

@Composable
fun ImageGridHeader(date: Instant, modifier: Modifier = Modifier) {
    val locale = Locale.getDefault()
    val zone = java.time.ZoneId.systemDefault()
    val formatter = DateTimeFormatter
        // LLLL gives non-conjugated month name 'listopad' instead of 'listopada'
        .ofPattern("LLLL uuuu")
        .withLocale(locale)
        .withZone(zone)

    Text(
        text = formatter.format(date),
        modifier = modifier
    )
}

@Composable
fun ImageGrid(
    entries: List<ImageGalleryTimelineEntry>,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()

    if (entries.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val stickyHeader by remember {
        derivedStateOf {
            // Logic to determine which header should be sticky
            // based on first visible item

            val firstVisibleIndex = gridState.firstVisibleItemIndex
            when (val entry = entries[firstVisibleIndex]) {
                is ImageGalleryTimelineEntry.Header -> entry.date
                is ImageGalleryTimelineEntry.ImageItem -> entry.entry.date
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(4.dp),
            state = gridState
        ) {
            items(
                count = entries.size,
                key = { index ->
                    when (val item = entries[index]) {
                        is ImageGalleryTimelineEntry.Header -> item.date.toString()
                        is ImageGalleryTimelineEntry.ImageItem -> item.entry.id.toString()
                    }
                },
                span = { index ->
                    when (entries[index]) {
                        is ImageGalleryTimelineEntry.Header -> GridItemSpan(maxLineSpan)
                        else -> GridItemSpan(1)
                    }
                }
            ) { index ->
                val uiModel = entries[index]
                when (uiModel) {
                    is ImageGalleryTimelineEntry.Header -> {
                        ImageGridHeader(
                            date = uiModel.date,
                            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    is ImageGalleryTimelineEntry.ImageItem -> {
                        when (val item = uiModel.entry) {
                            is TimelineEntryK.Local -> {
                                AsyncImage(
                                    model = item.contentUri,
                                    placeholder = ColorPainter(MaterialTheme.colorScheme.secondaryContainer),
                                    contentDescription = "Gallery Image",
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                        .clickable { onImageClick(item.id.toString()) },
                                    contentScale = ContentScale.Crop
                                )
                            }

                            is TimelineEntryK.Remote -> {
                                AsyncImage(
                                    model = GrpcThumbnail(item.id),
                                    placeholder = ColorPainter(MaterialTheme.colorScheme.secondaryContainer),
                                    contentDescription = "Gallery Image",
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                        .clickable { onImageClick(item.id.toString()) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sticky header overlay
        stickyHeader.let { date ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.padding(4.dp)) {
                    ImageGridHeader(
                        date = date,
                        modifier = Modifier.padding(start = 8.dp, top = 0.dp, bottom = 8.dp)
                    )
                }
            }
        }

        FastScroller(
            gridState = gridState,
            label = { index ->
                val locale = Locale.getDefault()
                val zone = java.time.ZoneId.systemDefault()
                val formatter = DateTimeFormatter
                    .ofPattern("d MMM uuuu")
                    .withLocale(locale)
                    .withZone(zone)

                val date = when (val entry = entries[index]) {
                    is ImageGalleryTimelineEntry.Header -> entry.date
                    is ImageGalleryTimelineEntry.ImageItem -> entry.entry.date
                }
                formatter.format(date)
            },
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        )
    }
}