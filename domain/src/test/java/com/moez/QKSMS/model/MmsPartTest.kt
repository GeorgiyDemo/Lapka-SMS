package com.moez.QKSMS.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for MmsPart.getSummary() logic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class MmsPartTest {

    private fun part(type: String, text: String? = null): MmsPart {
        return MmsPart().apply {
            this.type = type
            this.text = text
        }
    }

    @Test
    fun `getSummary returns text for text plain type`() {
        val p = part("text/plain", "Hello")
        assertEquals("Hello", p.getSummary())
    }

    @Test
    fun `getSummary returns null text for text plain with null text`() {
        val p = part("text/plain", null)
        assertNull(p.getSummary())
    }

    @Test
    fun `getSummary returns Contact card for vCard type`() {
        val p = part("text/x-vCard")
        assertEquals("Contact card", p.getSummary())
    }

    @Test
    fun `getSummary returns Photo for image types`() {
        assertEquals("Photo", part("image/jpeg").getSummary())
        assertEquals("Photo", part("image/png").getSummary())
        assertEquals("Photo", part("image/gif").getSummary())
    }

    @Test
    fun `getSummary returns Video for video types`() {
        assertEquals("Video", part("video/mp4").getSummary())
        assertEquals("Video", part("video/3gpp").getSummary())
    }

    @Test
    fun `getSummary returns null for unknown types`() {
        assertNull(part("application/pdf").getSummary())
        assertNull(part("audio/mpeg").getSummary())
    }

    @Test
    fun `getUri constructs correct content URI`() {
        val p = MmsPart().apply { id = 42 }
        assertEquals("content://mms/part/42", p.getUri().toString())
    }

    @Test
    fun `default values are sensible`() {
        val p = MmsPart()
        assertEquals(0L, p.id)
        assertEquals(0L, p.messageId)
        assertEquals("", p.type)
        assertEquals(-1, p.seq)
        assertNull(p.name)
        assertNull(p.text)
    }
}
