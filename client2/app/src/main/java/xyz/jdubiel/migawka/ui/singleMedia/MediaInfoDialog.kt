package xyz.jdubiel.migawka.ui.singleMedia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import xyz.jdubiel.migawka.R
import xyz.jdubiel.migawka.data.MediaMetadata

val mockData = mapOf(
    MediaMetadata.ID to "12345678abcdef00",
    MediaMetadata.Exif_DateTime to "2 sty 2026 14:29:14",
    MediaMetadata.Exif_Make to "Samsung A52",
    MediaMetadata.Exif_Model to "A52s 5G",
    MediaMetadata.Exif_ISO to "800",
)

@Preview
@Composable
fun MediaInfoDialogPreview() {
    MediaInfoDialog(onClose = {}, state = MediaMetadataState.Success(mockData))
}

val tagToName = mapOf(
    MediaMetadata.ID to R.string.id,
    MediaMetadata.Path to R.string.path,
    MediaMetadata.CreationDate to R.string.creation_date,
    MediaMetadata.Exif_DateTime to R.string.exif_datetime,
    MediaMetadata.Exif_Make to R.string.exif_make,
    MediaMetadata.Exif_Model to R.string.exif_model,
    MediaMetadata.Exif_Orientation to R.string.exif_orientation,
    MediaMetadata.Exif_FocalLength to R.string.exif_focal_length,
    MediaMetadata.Exif_ExposureTime to R.string.exif_exposure_time,
    MediaMetadata.Exif_FNumber to R.string.exif_f_number,
    MediaMetadata.Exif_ISO to R.string.exif_iso,
    MediaMetadata.Exif_Flash to R.string.exif_flash,
    MediaMetadata.Exif_WhiteBalance to R.string.exif_white_balance,
)

@Composable
fun MediaInfoDialog(
    onClose: () -> Unit,
    state: MediaMetadataState
) {
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = { onClose() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Media info",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                when (state) {
                    is MediaMetadataState.Loading -> {
                        Text("Loading...")
                    }

                    is MediaMetadataState.Error -> {
                        Text("Error: ${state.message}")
                    }

                    is MediaMetadataState.Empty -> {
                        Text("No data")
                    }

                    is MediaMetadataState.Success -> {
                        MediaMetadata.entries.forEachIndexed { index, tag ->
                            println("$index: $tag")

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                val displayName =
                                    tagToName[tag]?.let { stringResource(it) } ?: tag.toString()
                                Text(displayName, fontWeight = FontWeight.Bold)

                                val tagValue = state.data[tag]
                                if (tagValue != null) {
                                    Text(tagValue)
                                } else {
                                    Text("Unknown", fontStyle = FontStyle.Italic)
                                }
                            }

                            if (index < MediaMetadata.entries.size) {
                                HorizontalDivider()
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onClose() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}