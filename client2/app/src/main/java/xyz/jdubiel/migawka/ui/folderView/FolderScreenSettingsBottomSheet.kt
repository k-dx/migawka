package xyz.jdubiel.migawka.ui.folderView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.jdubiel.migawka.ui.components.SliderWithLabels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreenSettingsBottomSheet(
    columnOptions: List<UInt>,
    columnCount: UInt,
    setColumnCount: (UInt) -> Unit,
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
    }
}
