package xyz.jdubiel.migawka.ui.singleMedia

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.network.GrpcResult
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.RemoteFullImage
import xyz.jdubiel.migawka.data.RemoteImage
import xyz.jdubiel.migawka.data.TimelineEntryK

sealed interface FullImageUiState {
    data object Loading : FullImageUiState
    data class Success(val image: RemoteImage, val page: Int) : FullImageUiState
    data class Error(val message: String?) : FullImageUiState
    data object Empty : FullImageUiState
}

sealed interface DownloadState {
    data object Loading : DownloadState
    data class Success(val image: RemoteFullImage) : DownloadState
    data class Error(val message: String?) : DownloadState
    data object Empty : DownloadState
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

    private val _downloadState = mutableStateOf<DownloadState>(DownloadState.Empty)
    val downloadState: State<DownloadState> = _downloadState

    init {
        Log.d(TAG, "entries size = ${entries.size}")
        Log.d(TAG, "initialImageId = $initialImageId")
    }

    /**
     * Fetches the "full" (higher resolution) image for the given page.
     * If the image is already fetched for this page, it is not fetched again.
     */
    fun fetchFullImage(id: Hash, page: Int) {
        Log.d(TAG, "fetchFullImage: id $id page $page")

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
                val result = imageRepository.getRemoteOptimizedImage(id)

                withContext(Dispatchers.Main) {
                    when (result) {
                        is GrpcResult.Success ->
                            _fullImageState.value =
                                FullImageUiState.Success(image = result.data, page = page)
                        is GrpcResult.Error ->
                            _fullImageState.value = FullImageUiState.Error(result.message)
                    }
                }
            } catch (_: CancellationException) {
                // This is expected when a job is cancelled.
                Log.d(TAG, "Image fetch cancelled for page $page")
            }
        }
    }

    fun setCurrentPage(page: Int) {
        Log.d(TAG, "onPageChange: $page")
        _currentPage.intValue = page
        _downloadState.value = DownloadState.Empty
    }

    fun downloadImage(id: Hash) {
        _downloadState.value = DownloadState.Loading
        // TODO: change the scope, so the download doesn't get cancelled e.g. if the user changes
        // the screen or exits the app (?)
        viewModelScope.launch(Dispatchers.IO) {
            val result = imageRepository.getRemoteFullImage(id)
            when (result) {
                is GrpcResult.Success -> {
                    try {
                        imageRepository.saveImageToGallery(result.data)
                        withContext(Dispatchers.Main) {
                            _downloadState.value = DownloadState.Success(result.data)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "downloadImage: error saving to gallery: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            _downloadState.value = DownloadState.Error(e.message)
                        }
                    }
                }
                is GrpcResult.Error -> {
                    Log.e(TAG, "downloadImage: error: ${result.message}")
                    withContext(Dispatchers.Main) {
                        _downloadState.value = DownloadState.Error(result.message)
                    }
                }
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

