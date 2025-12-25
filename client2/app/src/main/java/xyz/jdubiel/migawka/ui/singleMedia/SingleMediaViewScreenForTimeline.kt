package xyz.jdubiel.migawka.ui.singleMedia

import android.content.Intent
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.Utils
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.RemoteImage
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.coil3.GrpcThumbnail
import xyz.jdubiel.migawka.findActivity
import java.io.File


@Composable
fun SingleMediaViewScreenForTimeline(
    viewModel: SingleMediaViewModelForTimelineI,
    initialImageId: Hash,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = view.context.findActivity()
    val scope = rememberCoroutineScope()
    // TODO: fix a bug where going back after browsing photos left/right changes the scroll
    // position in the gallery

    val entries by viewModel.entries.collectAsState()

    val initialPage = entries.indexOfFirst{ it.id == initialImageId }

    if (initialPage == -1) {
        // This accounts for the fact that initialPage might not be found during the first (few)
        // compositions since collecting from flow is done asynchronously. Since we know that the
        // photo should be found eventually, we just display loading screen if data is not yet
        // there.

        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { entries.size }
    )

    LaunchedEffect(initialPage) {
        pagerState.scrollToPage(initialPage)
    }

    val entry = entries[pagerState.currentPage]

    // single full-image slot and metadata about which page it belongs to
    var fullImage by remember { mutableStateOf<RemoteImage?>(null) }
    var fullImagePage by remember { mutableStateOf<Int?>(null) }
    var fullImageError by remember { mutableStateOf<String?>(null) }
    var lastRequestId by remember { mutableIntStateOf(0) } // to cancel/stale-guard

    // When page changes, (re)download the full image for that page.
    LaunchedEffect(pagerState.currentPage) {
        val index = pagerState.currentPage
        // clear or keep previous full image while loading — here we clear to show thumbnail immediately
        fullImage = null
        fullImagePage = null
        fullImageError = null

        if (entries[index] is TimelineEntryK.Remote) {
            // increment request id so earlier downloads don't override newer ones
            val requestId = ++lastRequestId

            // launch download on IO dispatcher
            scope.launch(Dispatchers.IO) {
                try {
                    val downloaded = viewModel.getRemoteOptimizedImage(entry.id)
                    withContext(Dispatchers.Main) {
                        // only set if this is the latest request
                        if (requestId == lastRequestId) {
                            fullImage = downloaded
                            fullImagePage = index
                        }
                    }
                } catch (e: Exception) {
                    // keep thumbnail on failure; optionally set an error image
                    withContext(Dispatchers.Main) {
                        if (requestId == lastRequestId) {
                            fullImageError = e.message
                        }
                    }
                }
            }
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
                            if (fullImage != null) {
                                val imagesDir = File(context.filesDir, "share").apply { if (!exists()) mkdirs() }
                                val cacheFile = File(imagesDir, "share_${System.currentTimeMillis()}.jpg").apply {
                                    outputStream().use { it.write(fullImage!!.bytes) }
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
                            } else {
                                Toast.makeText(context, "Cannot share thumbnail. Please wait for the image to load.", Toast.LENGTH_LONG).show()
                            }
                        }
                        else -> null
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
            Text("${hash.toHex()}") // TODO: remove me
        }
    }

    MediaOverlay(topOverlayContent = topOverlayContent, buttons = buttons) {
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
                    when (entry) {
                        is TimelineEntryK.Local -> {
                            AsyncImage(
                                model = entry.contentUri,
                                contentDescription = "Full screen image",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        is TimelineEntryK.Remote -> {
                            Box() {
                                val loadingState = when {
                                    fullImage != null && fullImagePage == pageIndex -> "ok"
                                    fullImageError != null -> "error"
                                    else -> "loading"
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = GrpcThumbnail(entry.id),
                                            contentDescription = "Full screen thumbnail",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.Center),
                                            contentScale = ContentScale.Fit
                                        )
                                        if (loadingState == "error") {
                                            Text("Fetching error: $fullImageError")
                                        } else if (loadingState == "loading") {
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                }

                                if (loadingState == "ok") {
                                    AsyncImage(
                                        model = fullImage!!.bytes,
                                        contentDescription = "Full screen image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.Center),
                                        contentScale = ContentScale.Fit
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
