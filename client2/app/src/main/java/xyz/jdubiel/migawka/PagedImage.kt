package xyz.jdubiel.migawka

import android.net.Uri
import java.time.Instant

// `private constructor` Makes the primary constructor private so callers cannot
// instantiate Sha256 directly with arbitrary arrays.

// `private val bytes: ByteArray` Stores the underlying bytes in a private
// property bytes. Keeping it private prevents external code from mutating or
// replacing the internal array reference directly
class Sha256 private constructor(private val bytes: ByteArray) {
    // `companion object` Provides factory functions and helpers associated with
    // Sha256. Because the primary constructor is private, this is the intended
    // public surface for creating instances.
    companion object {
        fun of(bytes: ByteArray): Sha256 =
            if (bytes.size == 32) Sha256(bytes.copyOf())
            else throw IllegalArgumentException("SHA-256 must be 32 bytes")
        fun fromHex(hex: String): Sha256 = of(hex.chunked(2)
                                    .map { it.toInt(16).toByte() }.toByteArray())
    }

    fun bytes(): ByteArray = bytes.copyOf()
    fun toHex(): String = bytes.joinToString("") { "%02x".format(it) }

    // Because we are using ByteArray, default equals will not work as it compares
    // references. We must override and compare contents instead.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sha256) return false

        return bytes.contentEquals(other.bytes)
    }
}


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