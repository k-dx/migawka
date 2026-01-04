package xyz.jdubiel.migawka.ui.imageGallery

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import xyz.jdubiel.migawka.Constants
import xyz.jdubiel.migawka.R
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.data.IndexingState
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.coil3.GrpcThumbnail
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Displays a gallery grid with images. Assumes the permission is already granted.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGalleryScreen(
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageGalleryViewModel
) {
    val entries by viewModel.entriesWithHeaders.collectAsState()
    val fetchErr by viewModel.fetchErr.collectAsState()
    val indexingState by viewModel.localMediaIndexingState.collectAsStateWithLifecycle()

    val columnCount by viewModel.galleryColumnCount.collectAsState()
    val showOverlayIcons by viewModel.showOverlayIcons.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }

    Log.d(TAG, "entries.size = ${entries.size} (including headers)")

    // Bar showing potential connection error
    if (fetchErr != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error)
        ) {
            Text(
                text = stringResource(R.string.connection_error, fetchErr?.message ?: ""),
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(4.dp)
            )
        }
    }

    when (val state = indexingState) {
        is IndexingState.Idle -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                Text(text = stringResource(R.string.indexing_local))
            }
        }
        is IndexingState.Indexing -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                Text(text = "${stringResource(R.string.indexing_local)} : ${state.processedCount * 100 / state.totalCount}%")
            }
        }
        is IndexingState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                Text(text = "${stringResource(R.string.error)} : ${state.message}%")
            }
        }
        is IndexingState.Finished -> {
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
            } else {
                ImageGrid(
                    entries = entries,
                    onImageClick = onImageClick,
                    onGallerySettingsClick = { showBottomSheet = true },
                    columnCount = columnCount.toInt(),
                    showOverlayIcons = showOverlayIcons,
                    modifier = modifier
                )
            }
        }
    }

    if (showBottomSheet) {
        ImageGallerySettingsBottomSheet(
            columnOptions = Constants.columnGalleryViewOptions,
            onDismiss = { showBottomSheet = false },
            columnCount = columnCount,
            setColumnCount = { viewModel.setGalleryColumnCount(it) },
            showOverlayIcons = showOverlayIcons,
            onShowOverlayIconsChange = { viewModel.setShowOverlayIcons(it) }
        )
    }
}

@Composable
fun ImageGridHeader(date: Instant, modifier: Modifier = Modifier) {
    val locale = Locale.getDefault()
    val zone = ZoneId.systemDefault()
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
    onGallerySettingsClick: () -> Unit,
    showOverlayIcons: Boolean,
    modifier: Modifier = Modifier,
    columnCount: Int = 3,
) {
    val gridState = rememberLazyGridState()

    val maxIconSize = 24.dp
    var iconSize by remember { mutableStateOf(maxIconSize) }


    val stickyHeader: Instant? by remember {
        derivedStateOf {
            // Logic to determine which header should be sticky
            // based on first visible item

            val firstVisibleIndex = gridState.firstVisibleItemIndex
            if (firstVisibleIndex < 0 || firstVisibleIndex >= entries.size) {
                null
            } else {
                when (val entry = entries[firstVisibleIndex]) {
                    is ImageGalleryTimelineEntry.Header -> entry.date
                    is ImageGalleryTimelineEntry.ImageItem -> entry.entry.date
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
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
                        is ImageGalleryTimelineEntry.ImageItem -> when (item.entry) {
                            is TimelineEntryK.Local -> "${item.entry.id}#${item.entry.contentUri}"
                            is TimelineEntryK.Remote -> "${item.entry.id}"
                        }
                    }
                },
                span = { index ->
                    when (entries[index]) {
                        is ImageGalleryTimelineEntry.Header -> GridItemSpan(maxLineSpan)
                        else -> GridItemSpan(1)
                    }
                }
            ) { index ->
                when (val uiModel = entries[index]) {
                    is ImageGalleryTimelineEntry.Header -> {
                        ImageGridHeader(
                            date = uiModel.date,
                            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    is ImageGalleryTimelineEntry.ImageItem -> {
                        when (val item = uiModel.entry) {
                            is TimelineEntryK.Local -> {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                        .clickable { onImageClick(item.id.toString()) }
                                        .onSizeChanged { size ->
                                            val twentyPercent = (size.width * 0.2f).dp
                                            iconSize = minOf(maxIconSize, twentyPercent)
                                        },
                                ) {
                                    AsyncImage(
                                        model = item.contentUri,
                                        placeholder = ColorPainter(MaterialTheme.colorScheme.secondaryContainer),
                                        error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
                                        contentDescription = stringResource(R.string.gallery_image),
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )

                                    if (showOverlayIcons && item.onRemote) {
                                        Icon(
                                            imageVector = Icons.Outlined.CloudDone,
                                            contentDescription = stringResource(R.string.media_on_remote_server),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .size(iconSize)
                                                .padding(2.dp),
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                            }

                            is TimelineEntryK.Remote -> {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                        .clickable { onImageClick(item.id.toString()) }
                                        .onSizeChanged { size ->
                                            val twentyPercent = (size.width * 0.2f).dp
                                            iconSize = minOf(maxIconSize, twentyPercent)
                                        },
                                ) {
                                    AsyncImage(
                                        model = GrpcThumbnail(item.id),
                                        placeholder = ColorPainter(MaterialTheme.colorScheme.secondaryContainer),
                                        error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
                                        contentDescription = stringResource(R.string.gallery_image),
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )

                                    if (showOverlayIcons) {
                                        Icon(
                                            imageVector = Icons.Outlined.CloudDone,
                                            contentDescription = stringResource(R.string.media_on_remote_server),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .size(iconSize)
                                                .padding(2.dp),
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky header overlay
        stickyHeader?.let { date ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier.padding(
                        start = 4.dp,
                        top = 4.dp,
                        end = 16.dp,
                        bottom = 4.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        ImageGridHeader(
                            date = date,
                            modifier = Modifier.padding(start = 8.dp, top = 0.dp, bottom = 8.dp)
                        )
                        IconButton(onClick = { onGallerySettingsClick() }) {
                            Icon(
                                Icons.Outlined.GridView,
                                contentDescription = stringResource(R.string.open_grid_settings)
                            )
                        }
                    }
                }
            }
        }

        FastScroller(
            gridState = gridState,
            label = { index ->
                val locale = Locale.getDefault()
                val zone = ZoneId.systemDefault()
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
