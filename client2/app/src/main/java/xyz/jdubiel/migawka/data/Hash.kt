package xyz.jdubiel.migawka.data

interface Hash {
    fun bytes(): ByteArray
    fun toHex(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}



