package xyz.jdubiel.migawka

import android.net.Uri
import java.time.Instant

sealed class PagedImage {
    abstract val date: Instant

    data class FromUri(
        // TODO: add id (sha256)
        val contentUri: Uri,
        override val date: Instant
    ) : PagedImage()

    data class FromBytes(
        // TODO: add id (sha256)
        val bytes: ByteArray,
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