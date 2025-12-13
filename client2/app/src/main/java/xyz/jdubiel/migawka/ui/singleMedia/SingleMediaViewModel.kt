package xyz.jdubiel.migawka.ui.singleMedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.PagedImage

/**
 * ViewModel dedicated for the single media screen.
 * Exposes the same stream and operations needed by the composable,
 * but keeps dependencies localized for that screen.
 */
class SingleMediaViewModel(
//    application: Application,
    // image stream is provided externally and passed through
    val imageStream: Flow<PagingData<PagedImage>>,
    // repository injected from outside for better testability/DI
    private val imageRepository: ImageRepository
) : ViewModel() {

    suspend fun getRemoteOptimizedImage(id: Hash) = withContext(Dispatchers.IO) {
        imageRepository.getRemoteOptimizedImage(id)
    }

    fun downloadImage(id: Hash) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val img = imageRepository.getRemoteFullImage(id)
//
//                val file = File(img.path)
//                val filename = file.name
//                val path = "Pictures/Migawka/${file.parent}"
//                val values = ContentValues().apply {
//                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
//                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//                    put(MediaStore.Images.Media.RELATIVE_PATH, path)
//                    put(MediaStore.Images.Media.IS_PENDING, true)
//                }
//
//                val uri = getApplication<Application>().contentResolver.insert(
//                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
//                ) ?: return@launch
//
//                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
//                    output.write(img.bytes)
//                }
//
//                values.clear()
//                values.put(MediaStore.Images.Media.IS_PENDING, false)
//                getApplication<Application>().contentResolver.update(uri, values, null, null)
//            } catch (e: Exception) {
//                Log.d(TAG, "error when downloading image: ${e.message}")
//            }
//        }
    }
}

class SingleMediaViewModelFactory(
    private val imageStream: Flow<PagingData<PagedImage>>,
    private val imageRepository: ImageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SingleMediaViewModel(imageStream, imageRepository) as T
    }
}