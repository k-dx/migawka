package xyz.jdubiel.migawka.ui.singleMedia

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.RemoteImage
import xyz.jdubiel.migawka.data.TimelineEntryK

sealed interface FullImageUiState {
    data object Loading : FullImageUiState
    data class Success(val image: RemoteImage, val page: Int) : FullImageUiState
    data class Error(val message: String?) : FullImageUiState
    data object Empty : FullImageUiState
}

class SingleMediaViewScreenViewModel(
    private val imageRepository: ImageRepository,
    val entries: List<TimelineEntryK>,
    initialImageId: Hash
) :
    ViewModel() {

    // pager state should survive config changes with viewModel
    private val _currentPage = mutableIntStateOf(let {
        val index = entries.indexOfFirst { it.id == initialImageId }
        if (index == -1) 0 else index
    })
    val currentPage: State<Int> = _currentPage

    // single full-image slot and metadata about which page it belongs to
    private val _fullImageState =
        mutableStateOf<FullImageUiState>(FullImageUiState.Empty)
    val fullImageState: State<FullImageUiState> = _fullImageState
    private var fetchJob: Job? = null

    var pendingBackAction by mutableStateOf(false)
        private set

    init {
        Log.d(TAG, "entries size = ${entries.size}")
        Log.d(TAG, "initialImageId = ${initialImageId.toHex()}")
    }

    fun scheduleBackAction() {
        pendingBackAction = true
    }

    fun onBackActionConsumed() {
        pendingBackAction = false
    }

    /**
     * Fetches the "full" (higher resolution) image for the given page.
     * If the image is already fetched for this page, it is not fetched again.
     */
    fun fetchFullImage(id: Hash, page: Int) {
        Log.d(TAG, "fetchFullImage: id ${id.toHex()} page $page")

        val currentState = _fullImageState.value
        if (currentState is FullImageUiState.Success && currentState.page == page && currentState.image.hash == id) {
            Log.d(TAG, "fetchFullImage: Image for page $page already present.")
            return
        }

        // cancel previous fetch job
        fetchJob?.cancel()

        // set loading state

        _fullImageState.value = FullImageUiState.Loading

        // launch download on IO dispatcher
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloaded = imageRepository.getRemoteOptimizedImage(id)
                withContext(Dispatchers.Main) {
                    _fullImageState.value =
                        FullImageUiState.Success(image = downloaded, page = page)
                }
            } catch (_: CancellationException) {
                // This is expected when a job is cancelled.
                Log.d(TAG, "Image fetch cancelled for page $page")
            } catch (e: Exception) {
                // keep thumbnail on failure; optionally set an error image
                withContext(Dispatchers.Main) {
                    _fullImageState.value = FullImageUiState.Error(e.message)
                }
            }
        }
    }

    fun setCurrentPage(page: Int) {
        Log.d(TAG, "onPageChange: $page")
        _currentPage.intValue = page
    }

    fun downloadImage(id: Hash) {
        viewModelScope.launch(Dispatchers.IO) { // TODO: change the scope
            try {
                val img = imageRepository.getRemoteFullImage(id)
                imageRepository.saveImageToGallery(img)
            } catch (e: Exception) {
                Log.d(xyz.jdubiel.migawka.TAG, "error when downloading image: ${e.message}")
                // TODO: Display info about the error to the user
            }
        }
    }

    companion object {
        private const val TAG = "SingleMediaViewScreenForTimelineViewModel"
    }
}

class SingleMediaViewScreenForTimelineViewModelFactory(
    private val application: Application,
    private val entries: List<TimelineEntryK>,
    private val initialImageId: Hash,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SingleMediaViewScreenViewModel::class.java)) {
            val imageRepository = (application as MigawkaApplication).imageRepository
            return SingleMediaViewScreenViewModel(
                imageRepository,
                entries,
                initialImageId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

