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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import xyz.jdubiel.migawka.Utils
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.coil3.GrpcThumbnail
import xyz.jdubiel.migawka.findActivity
import java.io.File


@Composable
fun SingleMediaViewScreenForTimeline(
    galleryViewModel: SingleMediaViewModelForTimelineI,
    viewModel: SingleMediaViewScreenForTimelineViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = view.context.findActivity()

    val entries = viewModel.entries

    // we want pagerState to survive configuration changes, so it also is stored in viewModel
    val pagerState = rememberPagerState(
        initialPage = viewModel.currentPage.value,
        pageCount = { entries.size }
    )
    // Sync changes back to ViewModel
    LaunchedEffect(pagerState) {
        viewModel.setCurrentPage(pagerState.currentPage)
    }

    val entry = entries[pagerState.currentPage]
    // When page changes, check if the new page is a remote image.
    // If it is, download the full image for that page.
    LaunchedEffect(pagerState.currentPage) {
        val index = pagerState.currentPage
        Log.d("SMS", "page changed to $index")
        if (entries[index] is TimelineEntryK.Remote) {
            viewModel.fetchFullImage(entry.id, index)
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
                        galleryViewModel.downloadImage(entry.id)
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
                                                .align(Alignment.Center),
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
