package xyz.jdubiel.migawka

import org.junit.Assert.assertEquals
import org.junit.Test

class Sha256UnitTests {
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
