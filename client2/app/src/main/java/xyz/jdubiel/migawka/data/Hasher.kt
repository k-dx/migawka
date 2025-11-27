package xyz.jdubiel.migawka.data

import java.security.MessageDigest


/**
 * This interface is responsible for creating hashes.
 */
interface Hasher {
    fun fromHex(hex: String): Hash
    fun fromBytes(bytes: ByteArray): Hash
    fun getInstance(): MessageDigest
}