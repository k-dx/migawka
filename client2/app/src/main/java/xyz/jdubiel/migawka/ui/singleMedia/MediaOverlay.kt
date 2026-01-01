package xyz.jdubiel.migawka.ui.singleMedia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.jdubiel.migawka.Utils.Companion.ToggleSystemBars

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OverlayPreview() {
    MediaOverlay(
        topOverlayContent = { Text("2022/01/02") },
        buttons = listOf(
            { Button(onClick = {}) { Text("Button 1") } },
            { Button(onClick = {}) { Text("Bu 2") } }
        )
    ) {
        Text("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse vel venenatis nulla. Proin sed luctus tellus, eu elementum nisl. Duis iaculis arcu a interdum ultricies. Aliquam viverra urna egestas nulla sodales, porta venenatis neque placerat. Nulla convallis elit vel diam facilisis, at elementum nibh pellentesque. Etiam lobortis pharetra mauris at interdum. Phasellus id ipsum lobortis, ultrices massa nec, elementum turpis. Aliquam vitae condimentum nunc. Proin tempus erat gravida nisi viverra, sed elementum nunc ornare. Aliquam venenatis tincidunt sodales. Nunc ut ipsum imperdiet, interdum ante vitae, fermentum orci. Pellentesque eget scelerisque turpis. Suspendisse vitae pulvinar mauris. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Integer dignissim sodales lacus, a mattis arcu aliquet eget. ")
    }
}

@Composable
fun MediaOverlay(
    topOverlayContent: @Composable () -> Unit,
    buttons: List<@Composable () -> Unit>,
    content: @Composable () -> Unit
) {
    val overlayColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    var showOverlay by rememberSaveable { mutableStateOf(true) }

    ToggleSystemBars(visible = showOverlay)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showOverlay = !showOverlay }
    ) {
        content()

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
        ) {
            Box(modifier = Modifier.background(overlayColor)) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .statusBarsPadding()
                ) {
                    topOverlayContent()
                }
            }
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
        ) {
            Box(modifier = Modifier.background(overlayColor)) {
                Box(modifier = Modifier
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        buttons.forEach { it() }
                    }
                }
            }
        }
    }
}