package xyz.jdubiel.migawka.ui.singleMedia

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.RemoteImage


enum class LoadingState {
    OK, ERROR, LOADING
}

data class ImageRequestResult(
    val image: RemoteImage?,
    val page: Int?,
    val error: String?
)

class SingleMediaViewScreenForTimelineViewModel(private val imageRepository: ImageRepository) :
    ViewModel() {

    // single full-image slot and metadata about which page it belongs to
    private val _fullImageRequestResult =
        mutableStateOf(ImageRequestResult(null, null, null))
    val fullImageRequestResult: State<ImageRequestResult> = _fullImageRequestResult
    var lastRequestId = 0

    /**
     * Fetches the "full" (higher resolution) image for the given page.
     * If the image is already fetched for this page, it is not fetched again.
     */
    fun fetchFullImage(id: Hash, page: Int) {
        // increment request id so earlier downloads don't override newer ones
        val requestId = ++lastRequestId

        Log.d(TAG, "fetchFullImage: id ${id.toHex()} page $page")

        val currentResult = _fullImageRequestResult.value
        if (currentResult.page == page && currentResult.image?.hash == id) {
            Log.d(TAG, "fetchFullImage: page $page + id $id already present")
            return
        }

        // launch download on IO dispatcher
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloaded = imageRepository.getRemoteOptimizedImage(id)
                withContext(Dispatchers.Main) {
                    // only set if this is the latest request
                    if (requestId == lastRequestId) {
                        _fullImageRequestResult.value = ImageRequestResult(
                            image = downloaded,
                            page = page,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                // keep thumbnail on failure; optionally set an error image
                withContext(Dispatchers.Main) {
                    if (requestId == lastRequestId) {
                        _fullImageRequestResult.value = ImageRequestResult(
                            image = null,
                            page = null,
                            error = e.message
                        )
                    }
                }
            }
        }
    }

    fun getLoadingState(pageIndex: Int): LoadingState {
        val image = _fullImageRequestResult.value.image
        val page = _fullImageRequestResult.value.page
        val error = _fullImageRequestResult.value.error

        return when {
            image != null && page == pageIndex -> LoadingState.OK
            error != null -> LoadingState.ERROR
            else -> LoadingState.LOADING
        }
    }

    companion object {
        private const val TAG = "SingleMediaViewScreenForTimelineViewModel"
    }
}

class SingleMediaViewScreenForTimelineViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SingleMediaViewScreenForTimelineViewModel::class.java)) {
            val imageRepository = (application as MigawkaApplication).imageRepository
            return SingleMediaViewScreenForTimelineViewModel(imageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

