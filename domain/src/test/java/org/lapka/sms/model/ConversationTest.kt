package org.lapka.sms.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for Conversation model computed properties and methods.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class ConversationTest {

    @Test
    fun `SCHEME_NOT_DEF constant is negative one`() {
        assertEquals(-1, Conversation.SCHEME_NOT_DEF)
    }

    @Test
    fun `date returns zero when lastMessage is null`() {
        val conv = Conversation()
        assertEquals(0L, conv.date)
    }

    @Test
    fun `date returns lastMessage date when present`() {
        val msg = Message().apply { date = 1234567890L }
        val conv = Conversation().apply { lastMessage = msg }
        assertEquals(1234567890L, conv.date)
    }

    @Test
    fun `unread returns false when lastMessage is null`() {
        val conv = Conversation()
        assertFalse(conv.unread)
    }

    @Test
    fun `unread returns true when lastMessage is not read`() {
        val msg = Message().apply { read = false }
        val conv = Conversation().apply { lastMessage = msg }
        assertTrue(conv.unread)
    }

    @Test
    fun `unread returns false when lastMessage is read`() {
        val msg = Message().apply { read = true }
        val conv = Conversation().apply { lastMessage = msg }
        assertFalse(conv.unread)
    }

    @Test
    fun `me returns false when lastMessage is null`() {
        val conv = Conversation()
        assertFalse(conv.me)
    }

    @Test
    fun `default field values are sensible`() {
        val conv = Conversation()
        assertEquals(0L, conv.id)
        assertFalse(conv.archived)
        assertFalse(conv.blocked)
        assertFalse(conv.pinned)
        assertEquals("", conv.draft)
        assertEquals("", conv.name)
        assertEquals("", conv.encryptionKey)
        assertEquals(Conversation.SCHEME_NOT_DEF, conv.encodingSchemeId)
        assertEquals(0, conv.deleteEncryptedAfter)
        assertEquals(0, conv.deleteReceivedAfter)
        assertEquals(0, conv.deleteSentAfter)
        assertNull(conv.encryptionEnabled)
    }

    @Test
    fun `getTitle returns name when name is not blank`() {
        val conv = Conversation().apply { name = "My Group" }
        assertEquals("My Group", conv.getTitle())
    }

    @Test
    fun `getTitle falls back to empty string when name is blank and no recipients`() {
        val conv = Conversation().apply { name = "  " }
        // With no recipients, joinToString returns ""
        assertEquals("", conv.getTitle())
    }

    @Test
    fun `snippet returns null when lastMessage is null`() {
        val conv = Conversation()
        assertNull(conv.snippet)
    }

    @Test
    fun `snippet returns SMS body from lastMessage`() {
        val msg = Message().apply {
            type = "sms"
            body = "Hello!"
        }
        val conv = Conversation().apply { lastMessage = msg }
        assertEquals("Hello!", conv.snippet)
    }
}
