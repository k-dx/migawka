package xyz.jdubiel.migawka

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun SingleMediaViewScreen(imageUri: Uri, modifier: Modifier = Modifier) {
    AsyncImage(
        model = imageUri,
        contentDescription = "Full screen image",
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Fit // whole image without cropping
    )
}