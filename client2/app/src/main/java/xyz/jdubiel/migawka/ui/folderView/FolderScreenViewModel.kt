package xyz.jdubiel.migawka.ui.folderView

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.DirectoryEntryK
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.RemoteFileExplorer
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.UserSettingsRepository
import xyz.jdubiel.migawka.data.network.GrpcResult
import xyz.jdubiel.migawka.data.sortedDirectoriesThenImagesByDateDesc

sealed interface EntriesState<out T> {
    data class Success<out T>(val data: List<T>) : EntriesState<T>
    data object Loading : EntriesState<Nothing>
    data class Error(val message: String) : EntriesState<Nothing>
    data object Empty : EntriesState<Nothing>
}

class FolderScreenViewModel(
    private val path: String,
    private val imageRepository: ImageRepository,
    private val remoteFileExplorer: RemoteFileExplorer,
    private val settingsRepository: UserSettingsRepository
) : ViewModel() {

    val galleryColumnCount: StateFlow<Int> = settingsRepository.galleryColumnCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettingsRepository.DEFAULT_GALLERY_COLUMN_COUNT
    )
    private val _entries = MutableStateFlow<EntriesState<DirectoryEntryK>>(EntriesState.Empty)
    val entries: StateFlow<EntriesState<DirectoryEntryK>> = _entries.asStateFlow()

    private val _mediaEntries = MutableStateFlow<EntriesState<TimelineEntryK>>(EntriesState.Empty)
    val mediaEntries: StateFlow<EntriesState<TimelineEntryK>> = _mediaEntries.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _entries.value = EntriesState.Loading
                _mediaEntries.value = EntriesState.Loading

                Log.d(TAG, "loading entries")
                val result = remoteFileExplorer.getDirectoryEntries(path)

                when (val raw = result) {
                    is GrpcResult.Error -> {
                        withContext(Dispatchers.Main) {
                            _entries.value = EntriesState.Error(result.message)
                            _mediaEntries.value = EntriesState.Error(result.message)
                        }
                    }
                    is GrpcResult.Success -> {
                        Log.d(TAG, "entries loaded")

                        // ensure desired order: directories first, then thumbnails
                        val sorted = raw.data.sortedDirectoriesThenImagesByDateDesc()

                        // media entries
                        val mediaEntries = sorted
                            .filterIsInstance<DirectoryEntryK.Image>()
                            .map { TimelineEntryK.Remote(it.id, it.date) }

                        withContext(Dispatchers.Main) {
                            _entries.value = EntriesState.Success(sorted)
                            _mediaEntries.value = EntriesState.Success(mediaEntries)
                        }

                        val entriesSize = (_entries.value as EntriesState.Success).data.size
                        val mediaEntriesSize = (_mediaEntries.value as EntriesState.Success).data.size
                        Log.d(TAG, "loaded $entriesSize directory entries, including $mediaEntriesSize media items")
                    }
                }
            }
        }
    }

    fun setGalleryColumnCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setGalleryColumnCount(count)
        }
    }

    companion object {
        const val TAG = "FolderScreenViewModel"
    }
}

class FolderScreenViewModelFactory(
    private val path: String,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val imageRepository = (application as MigawkaApplication).imageRepository
        val remoteFileExplorer = (application as MigawkaApplication).remoteFileExplorer
        val settingsRepository = (application as MigawkaApplication).userSettingsRepository

        if (modelClass.isAssignableFrom(FolderScreenViewModel::class.java)) {
            return FolderScreenViewModel(
                path,
                imageRepository,
                remoteFileExplorer,
                settingsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

