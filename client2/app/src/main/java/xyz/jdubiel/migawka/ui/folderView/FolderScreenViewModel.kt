package xyz.jdubiel.migawka.ui.folderView

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.MigawkaApplication
import xyz.jdubiel.migawka.data.DirectoryEntryK
import xyz.jdubiel.migawka.data.FolderEntriesPagingSource
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.data.ImageRepository
import xyz.jdubiel.migawka.data.PagedImage
import xyz.jdubiel.migawka.ui.singleMedia.SingleMediaViewModelI

class FolderScreenViewModel(
    private val path: String,
    private val pageSize: Int = 30,
    private val imageRepository: ImageRepository
) : ViewModel(), SingleMediaViewModelI {

    private val baseDirEntriesStream: Flow<PagingData<DirectoryEntryK>> =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { FolderEntriesPagingSource(path, pageSize) }
        ).flow
        .cachedIn(viewModelScope)

    val dirEntriesStream: Flow<PagingData<DirectoryEntryK>> = baseDirEntriesStream

    override val imageStream: Flow<PagingData<PagedImage>> = baseDirEntriesStream
        .map { pagingData ->
            pagingData.filter { it is DirectoryEntryK.ThumbnailK }
        }
        .map { pagingData ->
            pagingData.map<DirectoryEntryK, PagedImage> {
                when (it) {
                    is DirectoryEntryK.ThumbnailK -> PagedImage.FromBytes(
                        id = it.id,
                        bytes = it.content,
                        date = it.creationTime
                    )
                    else -> throw Exception("This should never happen after filtering")
                }
            }
        }


    override fun downloadImage(id: Hash) {
        TODO("Not yet implemented")
    }

    override suspend fun getRemoteOptimizedImage(id: Hash) = withContext(Dispatchers.IO) {
        imageRepository.getRemoteOptimizedImage(id)
    }
}

class FolderScreenViewModelFactory(
    private val path: String,
    private val pageSize: Int,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val imageRepository = (application as MigawkaApplication).imageRepository
        if (modelClass.isAssignableFrom(FolderScreenViewModel::class.java)) {
            return FolderScreenViewModel(path, pageSize, imageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

