package xyz.jdubiel.migawka.data

import android.net.Uri
import java.time.Instant

sealed class TimelineEntryK {
    abstract val date: Instant
    abstract val id: Hash

    data class Local(
        val contentUri: Uri,
        override val id: Hash,
        override val date: Instant,
        val onRemote: Boolean
    ) : TimelineEntryK()

    data class Remote(
        override val id: Hash,
        override val date: Instant
    ) : TimelineEntryK() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Remote) return false
            return id == other.id
        }
    }
}
