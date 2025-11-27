package xyz.jdubiel.migawka.ui.singleMedia

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import xyz.jdubiel.migawka.Utils
import xyz.jdubiel.migawka.Utils.Companion.ToggleSystemBars
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.PagedImage
import xyz.jdubiel.migawka.data.RemoteImage
import xyz.jdubiel.migawka.findActivity
import xyz.jdubiel.migawka.ui.imageGallery.ImageGalleryViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OverlayPreview() {
    MediaOverlay(
        topOverlayContent = { Text("2022/01/02") },
        buttons = listOf(
            { Button(onClick = {}) { Text("Button 1") } },
            { Button(onClick = {}) { Text("Bu 2") } }
        )
    ) {
        Text("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse vel venenatis nulla. Proin sed luctus tellus, eu elementum nisl. Duis iaculis arcu a interdum ultricies. Aliquam viverra urna egestas nulla sodales, porta venenatis neque placerat. Nulla convallis elit vel diam facilisis, at elementum nibh pellentesque. Etiam lobortis pharetra mauris at interdum. Phasellus id ipsum lobortis, ultrices massa nec, elementum turpis. Aliquam vitae condimentum nunc. Proin tempus erat gravida nisi viverra, sed elementum nunc ornare. Aliquam venenatis tincidunt sodales. Nunc ut ipsum imperdiet, interdum ante vitae, fermentum orci. Pellentesque eget scelerisque turpis. Suspendisse vitae pulvinar mauris. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Integer dignissim sodales lacus, a mattis arcu aliquet eget. ")
    }
}

@Composable
fun MediaOverlay(
    topOverlayContent: @Composable () -> Unit,
    buttons: List<@Composable () -> Unit>,
    content: @Composable () -> Unit
) {
    val overlayColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    // TODO: this should be remembered when gone back to image gallery then chose another photo
    var showOverlay by rememberSaveable { mutableStateOf(true) }

    ToggleSystemBars(visible = showOverlay)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showOverlay = !showOverlay }
    ) {
        content()

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
        ) {
            Box(modifier = Modifier.background(overlayColor)) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .statusBarsPadding()
                ) {
                    topOverlayContent()
                }
            }
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
        ) {
            Box(modifier = Modifier.background(overlayColor)) {
                Box(modifier = Modifier
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        buttons.forEach { it() }
                    }
                }
            }
        }
    }
}

val locale = Locale.getDefault()
val zone = java.time.ZoneId.systemDefault()
val dateFormatter = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(locale)
    .withZone(zone)
val timeFormatter = DateTimeFormatter
    .ofLocalizedTime(FormatStyle.MEDIUM)
    .withLocale(locale)
    .withZone(zone)

@Composable
fun SingleMediaViewScreen(
    viewModel: ImageGalleryViewModel,
    initialImageId: Hash,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = view.context.findActivity()
    // TODO: fix a bug where going back after browsing photos left/right changes the scroll
    // position in the gallery

    val images = viewModel.imageStream.collectAsLazyPagingItems()

    val initialPage = images.itemSnapshotList.items.indexOfFirst{
        when (it) {
            is PagedImage.FromUri -> it.id == initialImageId
            is PagedImage.FromBytes -> it.id == initialImageId
        }
    }

    val pagerState = rememberPagerState(
        initialPage = if (initialPage != -1) initialPage else 0,
        pageCount = { images.itemCount }
    )

    LaunchedEffect(initialPage) {
        if (initialPage != -1) {
            pagerState.scrollToPage(initialPage)
        }
    }

    val autoRotateEnabled = Utils.isAutoRotateEnabled(context)
    val image = images[pagerState.currentPage]
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
        if (image is PagedImage.FromBytes) {
            add {
                OutlinedIconButton(
                    onClick = {
                        Toast.makeText(context, "Download: Not implemented yet", Toast.LENGTH_SHORT)
                            .show()
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
                    Toast.makeText(context, "Share: Not implemented yet", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }
    }



    val creationDate: Instant? = when (image) {
        is PagedImage.FromUri -> image.date
        is PagedImage.FromBytes -> image.date
        else -> null
    }

    val topOverlayContent = @Composable {
        Text("${dateFormatter.format(creationDate)} ${timeFormatter.format(creationDate)}")
    }

    MediaOverlay(topOverlayContent = topOverlayContent, buttons = buttons) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (images.itemCount == 0) {
                CircularProgressIndicator()
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val image = images[pageIndex]
                    if (image != null) {
                        when (image) {
                            is PagedImage.FromUri -> {
                                AsyncImage(
                                    model = image.contentUri,
                                    contentDescription = "Full screen image",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            is PagedImage.FromBytes -> {
                                var fullImage: RemoteImage? by remember { mutableStateOf(null) }
                                var error by remember { mutableStateOf<String?>(null) }

                                if (fullImage != null) {
                                    AsyncImage(
                                        model = fullImage!!.bytes,
                                        contentDescription = "Full screen image",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = image.bytes,
                                                contentDescription = "Full screen thumbnail",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.Center),
                                                contentScale = ContentScale.Fit
                                            )
                                            if (error != null) {
                                                Text("Fetching error: $error")
                                            } else {
                                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                            }
                                        }
                                    }
                                    LaunchedEffect(fullImage) {
                                        try {
                                            fullImage = viewModel.getRemoteImage(image.id)
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                    }
                                }
                            }
                        }


                    } else {
                        Text("Image not loaded")
                    }
                }
            }
        }
    }
}
