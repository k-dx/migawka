package xyz.jdubiel.migawka.ui.singleMedia

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil3.compose.AsyncImage
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import xyz.jdubiel.migawka.Utils
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.coil3.GrpcThumbnail
import xyz.jdubiel.migawka.findActivity
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

val locale: Locale = Locale.getDefault()
val zone: ZoneId = ZoneId.systemDefault()
val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(locale)
    .withZone(zone)
val timeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofLocalizedTime(FormatStyle.MEDIUM)
    .withLocale(locale)
    .withZone(zone)

@Composable
fun SingleMediaViewScreen(
    viewModel: SingleMediaViewScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = view.context.findActivity()

    val entries = viewModel.entries
    val pagerState = rememberPagerState(
        initialPage = viewModel.currentPage.value,
        pageCount = { entries.size }
    )

    val entry = entries[pagerState.currentPage]
    // When page changes, check if the new page is a remote image.
    // If it is, download the full image for that page.
    LaunchedEffect(pagerState.currentPage) {
        val index = pagerState.currentPage
        viewModel.setCurrentPage(index) // sync changes of state to ViewModel
        Log.d("SMS", "page changed to $index")
        val currentEntry = entries[index]
        if (currentEntry is TimelineEntryK.Remote) {
            viewModel.fetchFullImage(currentEntry.id, index)
        }
    }

    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var backPressHandled by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val windowInsetsController = remember(view) {
        val window = (view.context as? Activity)?.window
        window?.let { WindowCompat.getInsetsController(it, view) }
    }
    BackHandler(enabled = !backPressHandled) {
        // make system bars visible again
        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())

        backPressHandled = true // to disable this BackHandler and have the default
        coroutineScope.launch {
            awaitFrame()
            onBackPressedDispatcher?.onBackPressed()
            backPressHandled = false
        }
    }

    val autoRotateEnabled = Utils.isAutoRotateEnabled(context)
    val buttons: List<@Composable () -> Unit> = buildList {
        if (!autoRotateEnabled) {
            add {
                OutlinedIconButton(
                    onClick = {
                        if (activity != null) {
                            Utils.toggleDeviceOrientation(activity)
                        } else {
                            Log.e("SingleMediaViewScreen", "activity is null, cannot rotate")
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate")
                }
            }
        }
        if (entry is TimelineEntryK.Remote) {
            add {
                OutlinedIconButton(
                    onClick = {
                        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT)
                            .show()
                        viewModel.downloadImage(entry.id)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download")
                }
            }
        }
        add {

            OutlinedIconButton(
                onClick = {
                    when (entry) {
                        is TimelineEntryK.Local -> {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, entry.contentUri)
                                type = "image/*"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Share image via")
                            )
                        }
                        is TimelineEntryK.Remote -> {
                            when (val state = viewModel.fullImageState.value) {
                                is FullImageUiState.Success -> {
                                    val imagesDir = File(context.filesDir, "share").apply { if (!exists()) mkdirs() }
                                    val cacheFile = File(
                                        imagesDir,
                                        "share_${System.currentTimeMillis()}.jpg"
                                    ).apply {
                                        outputStream().use { it.write(state.image.bytes) }
                                    }
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }

                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share image via")
                                    )

                                    // TODO: remove the file after sharing
                                }

                                else -> {
                                    Toast.makeText(context, "Cannot share thumbnail. Please wait for the image to load.", Toast.LENGTH_LONG).show()

                                }
                            }
                        }
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }
    }

    val creationDate = entry.date
    val hash = entry.id

    val topOverlayContent = @Composable {
        Column() {
            Text("${dateFormatter.format(creationDate)} ${timeFormatter.format(creationDate)}")
            Text(hash.toString()) // TODO: remove me
        }
    }

    var showOverlay by remember { mutableStateOf(true) }

    MediaOverlay(
        topOverlayContent = topOverlayContent,
        buttons = buttons,
        showOverlay = showOverlay
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (entries.isEmpty()) {
                CircularProgressIndicator()
                Text("The list of photos is empty. This should not be possible!")
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val entryForPage = entries[pageIndex]
                    val zoomState = rememberZoomState()

                    when (entryForPage) {
                        is TimelineEntryK.Local -> {
                            AsyncImage(
                                model = entryForPage.contentUri,
                                contentDescription = "Full screen image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zoomable(
                                        zoomState = zoomState,
                                        onTap = { showOverlay = !showOverlay }
                                    ),
                                onSuccess = { state ->
                                    zoomState.setContentSize(state.painter.intrinsicSize)
                                },
                                contentScale = ContentScale.Fit
                            )
                        }

                        is TimelineEntryK.Remote -> {
                            Box() {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = GrpcThumbnail(entryForPage.id),
                                            contentDescription = "Full screen thumbnail",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.Center)
                                                .zoomable(
                                                    zoomState = zoomState,
                                                    onTap = { showOverlay = !showOverlay }
                                                ),
                                            onSuccess = { state ->
                                                zoomState.setContentSize(state.painter.intrinsicSize)
                                            },
                                            contentScale = ContentScale.Fit
                                        )

                                        when (val state = viewModel.fullImageState.value) {
                                            is FullImageUiState.Error -> {
                                                Text("Fetching error: ${state.message}")
                                            }

                                            is FullImageUiState.Loading, is FullImageUiState.Empty -> {
                                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                            }

                                            else -> {}
                                        }
                                    }
                                }

                                when (val state = viewModel.fullImageState.value) {
                                    is FullImageUiState.Success -> {
                                        AsyncImage(
                                            model = state.image.bytes,
                                            contentDescription = "Full screen image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.Center)
                                                .zoomable(
                                                    zoomState = zoomState,
                                                    onTap = { showOverlay = !showOverlay }
                                                ),
                                            onSuccess = { state ->
                                                zoomState.setContentSize(state.painter.intrinsicSize)
                                            },
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
