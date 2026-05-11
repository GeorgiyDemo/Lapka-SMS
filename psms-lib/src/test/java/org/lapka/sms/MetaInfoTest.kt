package org.lapka.sms

import org.junit.Assert.*
import org.junit.Test

class MetaInfoTest {

    @Test
    fun `serialize and parse roundtrip`() {
        val original = MetaInfo(mode = 3, version = 2, isChannel = false)
        val parsed = MetaInfo.parse(original.toByte())
        assertEquals(original.mode, parsed.mode)
        assertEquals(original.version, parsed.version)
        assertEquals(original.isChannel, parsed.isChannel)
    }

    @Test
    fun `serialize and parse with channel flag`() {
        val original = MetaInfo(mode = 5, version = 3, isChannel = true)
        val parsed = MetaInfo.parse(original.toByte())
        assertEquals(5, parsed.mode)
        assertEquals(3, parsed.version)
        assertTrue(parsed.isChannel)
    }

    @Test
    fun `mode uses lower 4 bits`() {
        for (mode in 0..15) {
            val meta = MetaInfo(mode = mode, version = 0, isChannel = false)
            val parsed = MetaInfo.parse(meta.toByte())
            assertEquals(mode, parsed.mode)
        }
    }

    @Test
    fun `version uses bits 4-6`() {
        for (version in 0..7) {
            val meta = MetaInfo(mode = 0, version = version, isChannel = false)
            val parsed = MetaInfo.parse(meta.toByte())
            assertEquals(version, parsed.version)
        }
    }

    @Test
    fun `channel flag uses bit 7`() {
        val withChannel = MetaInfo(mode = 0, version = 0, isChannel = true)
        val withoutChannel = MetaInfo(mode = 0, version = 0, isChannel = false)
        assertTrue(MetaInfo.parse(withChannel.toByte()).isChannel)
        assertFalse(MetaInfo.parse(withoutChannel.toByte()).isChannel)
    }
}
