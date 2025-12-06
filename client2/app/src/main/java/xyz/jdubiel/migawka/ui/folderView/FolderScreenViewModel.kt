package xyz.jdubiel.migawka.ui.folderView

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import xyz.jdubiel.migawka.DirectoryEntry
import xyz.jdubiel.migawka.data.FolderEntriesPagingSource

class FolderScreenViewModel(
    private val path: String,
    private val pageSize: Int = 30
) : ViewModel() {

    private val pathForRequest = path.removePrefix("/")

    val dirEntriesStream: Flow<PagingData<DirectoryEntry>> =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { FolderEntriesPagingSource(path, pageSize) }
        ).flow
        .cachedIn(viewModelScope)
}

class FolderScreenViewModelFactory(
    private val path: String,
    private val pageSize: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FolderScreenViewModel(path, pageSize) as T
    }
}

