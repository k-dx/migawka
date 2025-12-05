package xyz.jdubiel.migawka.ui.folderView

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FolderScreen(
    path: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text("Hello! The path is $path")
    }
}