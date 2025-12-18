package xyz.jdubiel.migawka.ui.imageGallery

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme

@Composable
fun Gallery(
    viewModel: ImageGalleryViewModel,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        GalleryPermissionWrapper(
            viewModel = viewModel,
            onImageClick = onImageClick
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MigawkaPreview() {
    MigawkaTheme {
        Gallery(
            viewModel = viewModel<ImageGalleryViewModel>(),
            onImageClick = {},
        )
    }
}