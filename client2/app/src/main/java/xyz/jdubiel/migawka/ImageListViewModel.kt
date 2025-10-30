package xyz.jdubiel.migawka

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Represents the state of our UI
data class ImageListUiState(
    val images: List<Uri> = emptyList(),
    val isLoading: Boolean = false
)

class ImageListViewModel : ViewModel() {

    // UI state exposed to the composable
    var uiState by mutableStateOf(ImageListUiState())
        private set

    // Queries the MediaStore for images
    // Uses withContext to switch to a background thread (Dispatchers.IO)
    // because this can be a long-running operation.
    suspend fun loadImages(context: Context) {
        uiState = uiState.copy(isLoading = true) // Show loading indicator

        val imageUris = withContext(Dispatchers.IO) {
            val imageList = mutableListOf<Uri>()
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, // No selection (all images)
                null, // No selection args
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    imageList.add(contentUri)
                }
            }
            imageList // Return the list of URIs
        }

        uiState = uiState.copy(images = imageUris, isLoading = false)
    }
}