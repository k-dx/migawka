package xyz.jdubiel.migawka

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.jdubiel.migawka.data.Sha256
import xyz.jdubiel.migawka.data.Sha256Hasher

class Sha256UnitTests {
    @Test
    fun calculatesCorrectValue() {
        val data = "The quick brown fox jumps over the lazy dog".toByteArray(charset("UTF-8"))

        val hasher = Sha256Hasher()

        val digest = hasher.getInstance()
        digest.update(data, 0, data.size)

        val hash = hasher.fromBytes(digest.digest())

        val expected = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592"
        assertEquals(expected, hash.toHex())
    }

    @Test
    fun parsesHexCorrectly() {
        val h = Sha256.fromHex("a")
        val expected = "000000000000000000000000000000000000000000000000000000000000000a"
        assertEquals(expected, h.toHex())
    }


    @Test
    fun comparison_isCorrect() {
        val v1: Sha256 = Sha256.fromHex("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb")
        val v2: Sha256 = Sha256.fromHex("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb")
        val v3: Sha256 = Sha256.fromHex("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bc")

        val result1 = v1 == v2
        assertEquals(true, result1)

        val result2 = v1 == v3
        assertEquals(false, result2)
    }

    @Test
    fun hashCode_isCorrect() {
        val v1: Sha256 = Sha256.fromHex("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb")
        val v2: Sha256 = Sha256.fromHex("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb")
        val v3: Sha256 = Sha256.fromHex("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bc")

        val result1 = v1.hashCode() == v2.hashCode()
        assertEquals(true, result1)

        val result2 = v1.hashCode() == v3.hashCode()
        assertEquals(false, result2)
    }
}
