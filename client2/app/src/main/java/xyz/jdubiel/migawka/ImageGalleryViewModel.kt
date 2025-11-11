package xyz.jdubiel.migawka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow

class ImageGalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val imageRepository = ImageRepository(application.contentResolver)

    val imageStream: Flow<PagingData<android.net.Uri>> = imageRepository.getImageStream()
        .cachedIn(viewModelScope)
}
