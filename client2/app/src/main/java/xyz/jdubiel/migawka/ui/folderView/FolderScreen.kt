package xyz.jdubiel.migawka.ui.folderView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import xyz.jdubiel.migawka.data.DirectoryEntryK

@Composable
fun FolderScreen(
    path: String, // this has the form with leading slash `/`
    navigateToPath: (String) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderScreenViewModel
) {
    val entries = viewModel.dirEntriesStream.collectAsLazyPagingItems()
    Column(modifier = modifier.padding(4.dp)) {
        PathBar(path = path, navigateToPath = navigateToPath)

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
                FolderScreenGrid(
                    entries,
                    onDirClick = { dirName ->
                        val newPath =
                            if (path.endsWith('/')) (path + dirName) else ("$path/$dirName")
                        navigateToPath(newPath)
                    },
                    onImageClick = onImageClick
                )
            }
        }
    }
}

@Composable
fun PathBar(path: String, navigateToPath: (String) -> Unit, modifier: Modifier = Modifier) {
    val pathParts = remember(path) {
        path.trim('/').split('/').filter { it.isNotEmpty() }
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(pathParts) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(vertical = 8.dp)
            .horizontalScroll(scrollState)
    ) {
        Text(
            text = "HOME",
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { navigateToPath("/") }
                .padding(horizontal = 4.dp)
        )

        pathParts.forEachIndexed { index, part ->
            Icon(Icons.Default.ChevronRight, contentDescription = "Separator")
            val currentPath = remember(pathParts, index) {
                "/" + pathParts.subList(0, index + 1).joinToString("/")
            }
            Text(
                text = part,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { navigateToPath(currentPath) }
                    .padding(horizontal = 4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PathBarPreview() {
    PathBar(path = "/Photos/2023/very/long/subdirectoryname/path/to/another/dir", navigateToPath = {})
}

@Composable
fun FolderScreenGrid(
    entries: LazyPagingItems<DirectoryEntryK>,
    onDirClick: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = entries.itemCount,
            key = { index ->
                val item = entries.peek(index)
                item?.name
                "placeholder_$index"
            },
        ) { index ->
            val item = entries[index]
            if (item != null) {
                when (item) {
                    is DirectoryEntryK.DirectoryK -> {
                        Box(modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onDirClick(item.name) }
                            .background(color = Color(0xFFA89B32))) {
                            Box(modifier = Modifier.padding(2.dp)) {
                                Text("${item.name}")
                            }
                        }
                    }
                    is DirectoryEntryK.ThumbnailK -> {
                        AsyncImage(
                            model = item.content,
                            contentDescription = "Gallery Image",
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onImageClick(item.id.toHex()) },
                            contentScale = ContentScale.Crop)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FolderScreenGridPreview() {
    // 1. Create your static list of data for the preview
    val fakeEntries = listOf(
        DirectoryEntryK.DirectoryK("Folder 1"),
        DirectoryEntryK.DirectoryK("Photos"),
        // For ThumbnailK, you can't easily fake the byte array, so just use more directories
        // or a placeholder if you have one.
        DirectoryEntryK.DirectoryK("Another Folder with a very long name that might not fit in the constrained size of the box; Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin ut diam vitae tellus semper aliquam eget eget libero. Maecenas consectetur blandit vestibulum. Etiam at tortor pharetra, vulputate neque eu, malesuada arcu."),
        DirectoryEntryK.DirectoryK("Vacation Pics"),
        DirectoryEntryK.DirectoryK("2024"),
        DirectoryEntryK.DirectoryK("2025"),
    )

    // 2. Create a Flow that emits PagingData from your static list
    val fakePagingDataFlow: Flow<PagingData<DirectoryEntryK>> = remember {
        flowOf(PagingData.from(fakeEntries))
    }

    // 3. Use collectAsLazyPagingItems on that flow
    val fakeLazyPagingItems = fakePagingDataFlow.collectAsLazyPagingItems()

    // 4. Pass the result to your composable
    FolderScreenGrid(entries = fakeLazyPagingItems, onDirClick = {})
}