package xyz.jdubiel.migawka

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage

@Composable
fun SingleMediaViewScreen(
    viewModel: ImageGalleryViewModel,
    initialImageId: Sha256,
    modifier: Modifier = Modifier
) {
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

                            if (fullImage != null) {
                                AsyncImage(
                                    model = fullImage!!.bytes,
                                    contentDescription = "Full screen image",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Column(){
                                    AsyncImage(
                                        model = image.bytes,
                                        contentDescription = "Full screen thumbnail",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text("Thumbnail, fetching")
                                    CircularProgressIndicator()
                                }
                                LaunchedEffect(fullImage) {
                                    // fullImage = viewModel.getRemoteImage(image.id)

                                    // TODO: this creates and shuts down a channel for every request
                                    val mi = Utils.fetchImageBytesGrpc(image.id)
                                    fullImage = RemoteImage(
                                        bytes = mi.content.toByteArray(),
                                        date = image.date,
                                        sha256 = image.id
                                    )
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
