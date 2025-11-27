package xyz.jdubiel.migawka

import net.jpountz.xxhash.XXHashFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.jdubiel.migawka.data.Xx64Hash
import xyz.jdubiel.migawka.data.Xx64Hasher


class XxHashUnitTests {
    @Test
    fun calculatesCorrectValueRawNonStreaming() {
        val factory = XXHashFactory.fastestInstance()

        val data = "The quick brown fox jumps over the lazy dog".toByteArray(charset("UTF-8"))

        val hash64 = factory.hash64()
        val seed: Long = 0 // used to initialize the hash value, use whatever

        // value you want, but always the same
        val hash = hash64.hash(data, 0, data.size, seed)

        val hashHexString: String = hash.toString(16)
        val expected = "b242d361fda71bc"

        assertEquals(expected, hashHexString)
    }

    @Test
    fun calculatesCorrectValueRawStreaming() {
        val factory = XXHashFactory.fastestInstance()

        val data = "The quick brown fox jumps over the lazy dog".toByteArray(charset("UTF-8"))
        val seed: Long = 0

        val hash64 = factory.newStreamingHash64(seed)

        val offset = 9
        hash64.update(data, 0, offset)
        hash64.update(data, offset, data.size - offset)

        val hash = hash64.value

        val hashHexString: String = hash.toString(16)
        val expected = "b242d361fda71bc"

        assertEquals(expected, hashHexString)
    }

    @Test
    fun calculatesCorrectValue() {
        val data = "The quick brown fox jumps over the lazy dog".toByteArray(charset("UTF-8"))

        val hasher = Xx64Hasher()

        val digest = hasher.getInstance()
        digest.update(data, 0, data.size)

        val hash = hasher.fromBytes(digest.digest())

        val hashHexString: String = hash.toHex()
        val expected = "0b242d361fda71bc"

        assertEquals(expected, hashHexString)
    }

    @Test
    fun parsesHexCorrectly() {
        val expected = "000000000000000a"

        val h = Xx64Hash.fromHex(expected)
        assertEquals(expected, h.toHex())
    }

    @Test
    fun parsesHexCorrectly2() {
        val expected = "000000000000000a"
        val h = Xx64Hash.fromHex("00a")
        assertEquals(expected, h.toHex())
    }

    @Test
    fun comparison_isCorrect() {
        val v1: Xx64Hash = Xx64Hash.fromHex("ca978112ca1bbdca")
        val v2: Xx64Hash = Xx64Hash.fromHex("ca978112ca1bbdca")
        val v3: Xx64Hash = Xx64Hash.fromHex("ca978112ca1bbdcd")

        val result1 = v1 == v2
        assertEquals(true, result1)

        val result2 = v1 == v3
        assertEquals(false, result2)
    }


    @Test
    fun hashCode_isCorrect() {
        val v1: Xx64Hash = Xx64Hash.fromHex("ca978112ca1bbdca")
        val v2: Xx64Hash = Xx64Hash.fromHex("ca978112ca1bbdca")
        val v3: Xx64Hash = Xx64Hash.fromHex("ca978112ca1bbdcd")

        val result1 = v1.hashCode() == v2.hashCode()
        assertEquals(true, result1)

        val result2 = v1.hashCode() == v3.hashCode()
        assertEquals(false, result2)
    }
}
