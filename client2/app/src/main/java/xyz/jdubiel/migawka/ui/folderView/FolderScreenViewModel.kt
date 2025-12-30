package xyz.jdubiel.migawka.ui.folderView

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.DirectoryEntryK
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.RemoteFileExplorer
import xyz.jdubiel.migawka.data.TimelineEntryK
import xyz.jdubiel.migawka.data.sortedDirectoriesThenImagesByDateDesc

class FolderScreenViewModel(
    private val path: String,
    private val imageRepository: ImageRepository,
    private val remoteFileExplorer: RemoteFileExplorer
) : ViewModel() {

    private val _entries = MutableStateFlow<List<DirectoryEntryK>>(emptyList())
    val entries: StateFlow<List<DirectoryEntryK>> = _entries.asStateFlow()

    private val _mediaEntries = MutableStateFlow<List<TimelineEntryK>>(emptyList())
    val mediaEntries: StateFlow<List<TimelineEntryK>> = _mediaEntries.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "loading entries")
                val raw = remoteFileExplorer.getDirectoryEntries(path)
                Log.d(TAG, "entries loaded")

                // ensure desired order: directories first, then thumbnails
                val sorted = raw.sortedDirectoriesThenImagesByDateDesc()

                // set entries
                _entries.value = sorted

                // set media entries
                val mediaEntries = sorted
                    .filterIsInstance<DirectoryEntryK.Image>()
                    .map { TimelineEntryK.Remote(it.id, it.date) }
                _mediaEntries.value = mediaEntries
            }

            Log.d(TAG, "loaded ${entries.value.size} directory entries, including ${mediaEntries.value.size} media items")
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
        if (modelClass.isAssignableFrom(FolderScreenViewModel::class.java)) {
            return FolderScreenViewModel(path, imageRepository, remoteFileExplorer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

