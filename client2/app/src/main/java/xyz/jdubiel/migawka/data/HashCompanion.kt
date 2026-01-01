package xyz.jdubiel.migawka.data

interface HashCompanion {
    fun of(bytes: ByteArray): Hash
    fun fromString(s: String): Hash
}