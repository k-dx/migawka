package xyz.jdubiel.migawka.ui.folderView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun FolderScreen(
    path: String, // this has the form that is presented in UI, ie with leading slash `/`
    modifier: Modifier = Modifier,
    viewModel: FolderScreenViewModel = viewModel(factory = FolderScreenViewModelFactory(path, 30))
) {
    val entries = viewModel.dirEntriesStream.collectAsLazyPagingItems()
    Column(modifier = modifier) {
        Text("Hello! The path is $path")

        when (entries.loadState.refresh) {
            is LoadState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LoadState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error loading directory.", modifier = modifier.padding(16.dp))
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(
                        count = entries.itemCount,
                    ) { index ->
                        val item = entries[index]
                        if (item != null) {
                            Text("$index: ${item.name}")
                        }
                    }
                }
            }
        }
    }
}