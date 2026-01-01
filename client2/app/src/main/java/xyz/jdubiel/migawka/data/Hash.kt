package xyz.jdubiel.migawka.data

interface Hash {
    fun bytes(): ByteArray
    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}



