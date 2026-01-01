package xyz.jdubiel.migawka.data

import net.jpountz.xxhash.XXHashFactory
import java.nio.ByteBuffer

class Xx64Hash private constructor(private val bytes: ByteArray) : Hash {
    companion object : HashCompanion {
        override fun of(bytes: ByteArray): Xx64Hash =
            if (bytes.size == 8) Xx64Hash(bytes.copyOf())
            else throw IllegalArgumentException("xx64Hash must be 8 bytes")
//        override fun fromHex(hex: String): Xx64Hash = of(hex.chunked(2)
//            .map { it.toInt(16).toByte() }.toByteArray())

        override fun fromHex(hex: String): Xx64Hash {
            if (hex.length > 16) {
                throw IllegalArgumentException("xx64Hash in hex must be at most 16 characters")
            }

            // 1. Parse the hex string into an unsigned Long. This is very fast.
            val longValue = hex.toULong(16).toLong()

            // 2. Allocate an 8-byte buffer and put the long into it.
            val bytes = ByteBuffer.allocate(8).putLong(longValue).array()

            // 3. Use the existing 'of' constructor which validates the size.
            return Xx64Hash(bytes) // Use private constructor directly for performance

        }
    }

    override fun bytes(): ByteArray = bytes.copyOf()
    override fun toString(): String = bytes.toULong().toString(16).padStart(16, '0')

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Xx64Hash) return false

        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

private fun ByteArray.toULong(): ULong {
    if (this.size != 8) {
        throw IllegalArgumentException("ByteArray must be 8 bytes long to convert to ULong")
    }
    return ByteBuffer.wrap(this).long.toULong()
}

fun Long.toBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(Long.SIZE_BYTES) // Long.SIZE_BYTES is 8
    buffer.putLong(this)
    return buffer.array()    }


class Xx64Digest() : Digest {
    val factory = XXHashFactory.fastestInstance()
    val hash64 = factory.newStreamingHash64(0)

    override fun update(input: ByteArray, offset: Int, len: Int) {
        hash64.update(input, offset, len)
    }

    override fun digest(): ByteArray {
        return hash64.value.toBytes()
    }
}

class Xx64Hasher() : Hasher {
    override fun fromHex(hex: String): Xx64Hash = Xx64Hash.fromHex(hex)
    override fun fromBytes(bytes: ByteArray): Xx64Hash = Xx64Hash.of(bytes)

    override fun getInstance(): Digest {
        return Xx64Digest()
    }
}