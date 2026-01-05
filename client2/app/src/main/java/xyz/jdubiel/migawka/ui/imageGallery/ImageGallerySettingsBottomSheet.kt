package xyz.jdubiel.migawka.ui.imageGallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.jdubiel.migawka.R
import xyz.jdubiel.migawka.ui.components.SliderWithLabels
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGallerySettingsBottomSheet(
    columnOptions: List<UInt>,
    columnCount: UInt,
    setColumnCount: (UInt) -> Unit,
    showOverlayIcons: Boolean,
    onShowOverlayIconsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val sliderValue = columnOptions.indexOf(columnCount).coerceAtLeast(0).toFloat()
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState
    ) {
        Box(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 32.dp
            )
        ) {
            SliderWithLabels(
                value = sliderValue,
                onValueChange = { setColumnCount(columnOptions[it.toInt()]) },
                options = columnOptions
            )
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 48.dp)
                .clickable { onShowOverlayIconsChange(!showOverlayIcons) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.show_media_origin))
            Switch(
                checked = showOverlayIcons,
                onCheckedChange = null//onShowOverlayIconsChange
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    MigawkaTheme {
        ImageGallerySettingsBottomSheet(
            columnOptions = listOf(1u, 2u, 3u, 4u),
            columnCount = 2u,
            setColumnCount = {},
            showOverlayIcons = true,
            onShowOverlayIconsChange = {}
        )
    }
}
