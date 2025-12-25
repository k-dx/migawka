package xyz.jdubiel.migawka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.jdubiel.migawka.ui.imageGallery.FastScroller

@Composable
fun SecondScreen(modifier: Modifier = Modifier) {
    val items = remember { (1..1000).toList() }
    val gridState = rememberLazyGridState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "First visible item: ${gridState.firstVisibleItemIndex}",
                modifier = Modifier.padding(16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(count = items.size, key = { index -> items[index] }) { index ->
                    Box(
                        modifier = Modifier.padding(8.dp).background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Item ${items[index]}")
                    }
                }
            }
        }
        FastScroller(
            gridState = gridState,
            label = { index -> "Item ${items[index]}" },
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        )
    }
}

