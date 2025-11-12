package xyz.jdubiel.migawka

import android.net.Uri
import java.util.Date

data class PagedImage(
    val contentUri: Uri,
    val date: Date
)