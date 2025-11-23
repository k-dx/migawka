package xyz.jdubiel.migawka.data

import android.net.Uri
import xyz.jdubiel.migawka.Sha256
import java.time.Instant

sealed class PagedImage {
    abstract val date: Instant

    data class FromUri(
        val id: Sha256,
        val contentUri: Uri,
        override val date: Instant
    ) : PagedImage()

    data class FromBytes(
        val id: Sha256,
        val bytes: ByteArray,
        val fullBytes: ByteArray? = null,
        override val date: Instant
    ) : PagedImage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FromBytes) return false
            return date == other.date && bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = date.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }
}