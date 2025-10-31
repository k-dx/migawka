package xyz.jdubiel.migawka

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun SingleMediaViewScreen(
    viewModel: ImageListViewModel,
    initialIndex: Int,
    modifier: Modifier = Modifier
) {
    // Get the list of images from the ViewModel's state
    val images = viewModel.uiState.images

    // Create a PagerState. It controls the pager and knows the current page.
    val pagerState = rememberPagerState(
        initialPage = if (initialIndex < images.size) initialIndex else 0,
        pageCount = { images.size }
    )

    // A one-time effect to scroll to the initial page.
    // This is good practice if the initialIndex could change.
    LaunchedEffect(initialIndex) {
        pagerState.scrollToPage(initialIndex)
    }

    // The HorizontalPager composable, which provides the swipe functionality.
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { pageIndex ->
        // This is the content for a single page. It gets called for each visible page.
        // We get the URI for the current page and display it.
        val imageUri = images[pageIndex]
        AsyncImage(
            model = imageUri,
            contentDescription = "Full screen image",
            modifier = modifier,
            contentScale = ContentScale.Fit // Fit ensures the whole image is visible
        )
    }
}