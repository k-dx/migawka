package xyz.jdubiel.migawka.ui.singleMedia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.jdubiel.migawka.R

@Composable
fun DownloadStatus(state: DownloadState) {
    when (state) {
        is DownloadState.Success -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))

                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = stringResource(R.string.saved_to_gallery),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        is DownloadState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = stringResource(R.string.fetching_error, state.message ?: ""),
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        is DownloadState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = stringResource(R.string.saving_image_to_gallery),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        is DownloadState.Empty -> {}
    }
}