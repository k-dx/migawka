package xyz.jdubiel.migawka.ui.imageGallery

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FastScroller(
    gridState: LazyGridState,
    label: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val maxHeight = constraints.maxHeight.toFloat()
        val thumbHeight = with(density) { 48.dp.toPx() }
        val scrollbarHeight = maxHeight - thumbHeight

        val totalItemsCount = gridState.layoutInfo.totalItemsCount
        val visibleItemsCount = gridState.layoutInfo.visibleItemsInfo.size
        val scrollableItems = totalItemsCount - visibleItemsCount

        val scrollProgress = remember(totalItemsCount, visibleItemsCount, gridState.firstVisibleItemIndex) {
            if (scrollableItems <= 0) 0f else gridState.firstVisibleItemIndex.toFloat() / scrollableItems.toFloat()
        }

        LaunchedEffect(isDragging, scrollProgress) {
            if (!isDragging) {
                offsetY = scrollbarHeight * scrollProgress
            }
        }

        // --- THE LABEL ---
        if (isDragging) {
            val firstItemIndex = gridState.firstVisibleItemIndex

            Text(
                text = label(firstItemIndex),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset((-60).dp.roundToPx(), offsetY.roundToInt()) }
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // --- THE TRACK ---
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .width(8.dp)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                )
        )

        // --- THE THUMB ---
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .width(8.dp)
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .height(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false }
                    ) { change, dragAmount ->
                        change.consume()
                        val newOffsetY = (offsetY + dragAmount.y).coerceIn(0f, scrollbarHeight)
                        offsetY = newOffsetY

                        val scrollProportion = newOffsetY / scrollbarHeight
                        if (scrollableItems > 0) {
                            coroutineScope.launch {
                                gridState.scrollToItem((scrollProportion * scrollableItems).roundToInt())
                            }
                        }
                    }
                }
        )
    }
}
