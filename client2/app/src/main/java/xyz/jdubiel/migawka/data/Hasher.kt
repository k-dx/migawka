package xyz.jdubiel.migawka.data

interface Digest {
    fun update(input: ByteArray, offset: Int, len: Int)
    fun digest(): ByteArray
}

/**
 * This interface is responsible for creating hashes.
 */
interface Hasher {
    fun fromHex(hex: String): Hash
//    fun fromString(string: String): Hash
    fun fromBytes(bytes: ByteArray): Hash
    fun getInstance(): Digest
}