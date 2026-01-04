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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import xyz.jdubiel.migawka.R
import xyz.jdubiel.migawka.data.DirectoryEntryK
import xyz.jdubiel.migawka.data.coil3.GrpcThumbnail

@Composable
fun FolderScreen(
    path: String, // this has the form with leading slash `/`
    navigateToPath: (String) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderScreenViewModel
) {
    val entries by viewModel.entries.collectAsState()
    Column(modifier = modifier.padding(4.dp)) {
        PathBar(path = path, navigateToPath = navigateToPath)

        when (val state = entries) {
            is EntriesState.Success -> {
                FolderScreenGrid(
                    modifier = Modifier,
                    state.data,
                    onDirClick = { dirName ->
                        val newPath =
                            if (path.endsWith('/')) (path + dirName) else ("$path/$dirName")
                        navigateToPath(newPath)
                    },
                    onImageClick = onImageClick
                )
            }
            is EntriesState.Loading, is EntriesState.Empty -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is EntriesState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.error_loading_directory, state.message),
                        modifier = modifier.padding(16.dp)
                    )
                }
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
            text = stringResource(R.string.home),
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { navigateToPath("/") }
                .padding(horizontal = 4.dp)
        )

        pathParts.forEachIndexed { index, part ->
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.separator)
            )
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
    modifier: Modifier = Modifier,
    entries: List<DirectoryEntryK>,
    onDirClick: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = entries.size,
            key = { index -> entries[index].name }
        ) { index ->
            val item = entries[index]
            when (item) {
                is DirectoryEntryK.Directory -> {
                    Box(modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onDirClick(item.name) }
                        .background(color = Color(0xFFA89B32))) {
                        Box(modifier = Modifier.padding(2.dp)) {
                            Text(item.name)
                        }
                    }
                }
                is DirectoryEntryK.Image -> {
                    AsyncImage(
                        model = GrpcThumbnail(item.id),
                        contentDescription = stringResource(R.string.gallery_image),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onImageClick(item.id.toString()) },
                        contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FolderScreenGridPreview() {
    val fakeEntries = listOf(
        DirectoryEntryK.Directory("Folder 1"),
        DirectoryEntryK.Directory("Photos"),
        // For ThumbnailK, you can't easily fake the byte array, so just use more directories
        // or a placeholder if you have one.
        DirectoryEntryK.Directory("Another Folder with a very long name that might not fit in the constrained size of the box; Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin ut diam vitae tellus semper aliquam eget eget libero. Maecenas consectetur blandit vestibulum. Etiam at tortor pharetra, vulputate neque eu, malesuada arcu."),
        DirectoryEntryK.Directory("Vacation Pics"),
        DirectoryEntryK.Directory("2024"),
        DirectoryEntryK.Directory("2025"),
    )

    FolderScreenGrid(entries = fakeEntries, onDirClick = {})
}