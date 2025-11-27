package xyz.jdubiel.migawka.data

import java.security.MessageDigest

// `private constructor` Makes the primary constructor private so callers cannot
// instantiate Sha256 directly with arbitrary arrays.

// `private val bytes: ByteArray` Stores the underlying bytes in a private
// property bytes. Keeping it private prevents external code from mutating or
// replacing the internal array reference directly
class Sha256 private constructor(private val bytes: ByteArray) : Hash {
    // `companion object` Provides factory functions and helpers associated with
    // Sha256. Because the primary constructor is private, this is the intended
    // public surface for creating instances.
    companion object : HashCompanion {
        override fun of(bytes: ByteArray): Sha256 =
            if (bytes.size == 32) Sha256(bytes.copyOf())
            else throw IllegalArgumentException("SHA-256 must be 32 bytes")
        override fun fromHex(hex: String): Sha256 = of(hex.chunked(2)
            .map { it.toInt(16).toByte() }.toByteArray())
    }

    override fun bytes(): ByteArray = bytes.copyOf()
    override fun toHex(): String = bytes.joinToString("") { "%02x".format(it) }

    // Because we are using ByteArray, default equals will not work as it compares
    // references. We must override and compare contents instead.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sha256) return false

        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

class Sha256Hasher() : Hasher {
    override fun fromHex(hex: String): Sha256 = Sha256.fromHex(hex)
    override fun fromBytes(bytes: ByteArray): Sha256 = Sha256.of(bytes)

    override fun getInstance(): MessageDigest {
        return MessageDigest.getInstance("SHA-256")
    }
}
