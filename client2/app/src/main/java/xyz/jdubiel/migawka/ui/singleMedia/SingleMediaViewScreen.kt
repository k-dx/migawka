package xyz.jdubiel.migawka.ui.singleMedia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import xyz.jdubiel.migawka.data.PagedImage
import xyz.jdubiel.migawka.data.RemoteImage
import xyz.jdubiel.migawka.data.Sha256
import xyz.jdubiel.migawka.ui.imageGallery.ImageGalleryViewModel

@Composable
fun SingleMediaViewScreen(
    viewModel: ImageGalleryViewModel,
    initialImageId: Sha256,
    modifier: Modifier = Modifier
) {
    // TODO: fix a bug where going back after browsing photos left/right changes the scroll
    // position in the gallery

    val images = viewModel.imageStream.collectAsLazyPagingItems()

    var showButtons by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showButtons = !showButtons }
    ) {
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
                                                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
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

        AnimatedVisibility(
            visible = showButtons,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
                .padding(top = 16.dp)
        ) {
            Box() {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    val currentImage = images[pagerState.currentPage]
                    Button(onClick = { /* Action 1 */ }) { Text("Rotate") }
                    if (currentImage is PagedImage.FromBytes) {
                        Button(onClick = { /* Action 2 */ }) { Text("Download") }
                    }
                    Button(onClick = { /* Action 2 */ }) { Text("Share") }
                }
            }

        }
    }
}
