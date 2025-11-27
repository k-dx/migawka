package xyz.jdubiel.migawka.data

interface HashCompanion {
    fun of(bytes: ByteArray): Hash
    fun fromHex(s: String): Hash
}