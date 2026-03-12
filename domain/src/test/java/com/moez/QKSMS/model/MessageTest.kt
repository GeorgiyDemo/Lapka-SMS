package com.moez.QKSMS.model

import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for Message model logic.
 *
 * Note: Message extends RealmObject, but these tests exercise the pure Kotlin
 * methods without touching Realm. The methods under test do not rely on Realm
 * managed state — they use plain field values.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class MessageTest {

    private fun smsMessage(
        boxId: Int = Sms.MESSAGE_TYPE_INBOX,
        body: String = "",
        subId: Int = 1,
        address: String = ""
    ): Message {
        return Message().apply {
            this.type = "sms"
            this.boxId = boxId
            this.body = body
            this.subId = subId
            this.address = address
        }
    }

    private fun mmsMessage(
        boxId: Int = Mms.MESSAGE_BOX_INBOX,
        subject: String = "",
        subId: Int = 1,
        address: String = ""
    ): Message {
        return Message().apply {
            this.type = "mms"
            this.boxId = boxId
            this.subject = subject
            this.subId = subId
            this.address = address
        }
    }

    // -- isMms / isSms --

    @Test
    fun `isMms returns true for mms type`() {
        assertTrue(mmsMessage().isMms())
    }

    @Test
    fun `isMms returns false for sms type`() {
        assertFalse(smsMessage().isMms())
    }

    @Test
    fun `isSms returns true for sms type`() {
        assertTrue(smsMessage().isSms())
    }

    @Test
    fun `isSms returns false for mms type`() {
        assertFalse(mmsMessage().isSms())
    }

    @Test
    fun `isSms and isMms both return false for unknown type`() {
        val msg = Message().apply { type = "unknown" }
        assertFalse(msg.isSms())
        assertFalse(msg.isMms())
    }

    // -- isMe --

    @Test
    fun `isMe returns false for incoming SMS (inbox)`() {
        assertFalse(smsMessage(boxId = Sms.MESSAGE_TYPE_INBOX).isMe())
    }

    @Test
    fun `isMe returns false for incoming SMS (all)`() {
        assertFalse(smsMessage(boxId = Sms.MESSAGE_TYPE_ALL).isMe())
    }

    @Test
    fun `isMe returns true for sent SMS`() {
        assertTrue(smsMessage(boxId = Sms.MESSAGE_TYPE_SENT).isMe())
    }

    @Test
    fun `isMe returns true for outbox SMS`() {
        assertTrue(smsMessage(boxId = Sms.MESSAGE_TYPE_OUTBOX).isMe())
    }

    @Test
    fun `isMe returns false for incoming MMS (inbox)`() {
        assertFalse(mmsMessage(boxId = Mms.MESSAGE_BOX_INBOX).isMe())
    }

    @Test
    fun `isMe returns false for incoming MMS (all)`() {
        assertFalse(mmsMessage(boxId = Mms.MESSAGE_BOX_ALL).isMe())
    }

    @Test
    fun `isMe returns true for sent MMS`() {
        assertTrue(mmsMessage(boxId = Mms.MESSAGE_BOX_SENT).isMe())
    }

    // -- isOutgoingMessage --

    @Test
    fun `isOutgoingMessage true for MMS outbox`() {
        assertTrue(mmsMessage(boxId = Mms.MESSAGE_BOX_OUTBOX).isOutgoingMessage())
    }

    @Test
    fun `isOutgoingMessage false for MMS sent`() {
        assertFalse(mmsMessage(boxId = Mms.MESSAGE_BOX_SENT).isOutgoingMessage())
    }

    @Test
    fun `isOutgoingMessage true for SMS failed`() {
        assertTrue(smsMessage(boxId = Sms.MESSAGE_TYPE_FAILED).isOutgoingMessage())
    }

    @Test
    fun `isOutgoingMessage true for SMS outbox`() {
        assertTrue(smsMessage(boxId = Sms.MESSAGE_TYPE_OUTBOX).isOutgoingMessage())
    }

    @Test
    fun `isOutgoingMessage true for SMS queued`() {
        assertTrue(smsMessage(boxId = Sms.MESSAGE_TYPE_QUEUED).isOutgoingMessage())
    }

    @Test
    fun `isOutgoingMessage false for SMS inbox`() {
        assertFalse(smsMessage(boxId = Sms.MESSAGE_TYPE_INBOX).isOutgoingMessage())
    }

    @Test
    fun `isOutgoingMessage false for SMS sent`() {
        assertFalse(smsMessage(boxId = Sms.MESSAGE_TYPE_SENT).isOutgoingMessage())
    }

    // -- isFailedMessage --

    @Test
    fun `isFailedMessage true for SMS failed type`() {
        assertTrue(smsMessage(boxId = Sms.MESSAGE_TYPE_FAILED).isFailedMessage())
    }

    @Test
    fun `isFailedMessage false for SMS sent type`() {
        assertFalse(smsMessage(boxId = Sms.MESSAGE_TYPE_SENT).isFailedMessage())
    }

    // -- isSending --

    @Test
    fun `isSending true for outgoing non-failed message`() {
        val msg = smsMessage(boxId = Sms.MESSAGE_TYPE_OUTBOX)
        assertTrue(msg.isSending())
    }

    @Test
    fun `isSending false for failed message`() {
        val msg = smsMessage(boxId = Sms.MESSAGE_TYPE_FAILED)
        assertFalse(msg.isSending())
    }

    @Test
    fun `isSending false for incoming message`() {
        val msg = smsMessage(boxId = Sms.MESSAGE_TYPE_INBOX)
        assertFalse(msg.isSending())
    }

    // -- isDelivered --

    @Test
    fun `isDelivered true when delivery status is complete`() {
        val msg = smsMessage()
        msg.deliveryStatus = Sms.STATUS_COMPLETE
        assertTrue(msg.isDelivered())
    }

    @Test
    fun `isDelivered false when delivery status is none`() {
        val msg = smsMessage()
        msg.deliveryStatus = Sms.STATUS_NONE
        assertFalse(msg.isDelivered())
    }

    // -- getCleansedSubject --

    @Test
    fun `getCleansedSubject returns empty for useless subjects`() {
        val useless = listOf("no subject", "NoSubject", "<not present>")
        for (subj in useless) {
            val msg = mmsMessage(subject = subj)
            assertEquals("Subject '$subj' should be cleansed", "", msg.getCleansedSubject())
        }
    }

    @Test
    fun `getCleansedSubject returns subject when meaningful`() {
        val msg = mmsMessage(subject = "Hello World")
        assertEquals("Hello World", msg.getCleansedSubject())
    }

    @Test
    fun `getCleansedSubject returns empty string for empty subject`() {
        val msg = mmsMessage(subject = "")
        assertEquals("", msg.getCleansedSubject())
    }

    // -- getText --

    @Test
    fun `getText returns body for SMS`() {
        val msg = smsMessage(body = "Hello there")
        assertEquals("Hello there", msg.getText())
    }

    @Test
    fun `getText returns empty string for SMS with no body`() {
        val msg = smsMessage()
        assertEquals("", msg.getText())
    }

    // -- getSummary --

    @Test
    fun `getSummary returns body for SMS`() {
        val msg = smsMessage(body = "Test message")
        assertEquals("Test message", msg.getSummary())
    }

    // -- attachmentType --

    @Test
    fun `attachmentType getter converts from string`() {
        val msg = Message()
        msg.attachmentTypeString = "IMAGE"
        assertEquals(Message.AttachmentType.IMAGE, msg.attachmentType)
    }

    @Test
    fun `attachmentType setter updates string`() {
        val msg = Message()
        msg.attachmentType = Message.AttachmentType.VIDEO
        assertEquals("VIDEO", msg.attachmentTypeString)
    }

    @Test
    fun `attachmentType defaults to NOT_LOADED`() {
        val msg = Message()
        assertEquals(Message.AttachmentType.NOT_LOADED, msg.attachmentType)
    }

    // -- compareSender --

    @Test
    fun `compareSender returns true for two outgoing messages with same subId`() {
        val a = smsMessage(boxId = Sms.MESSAGE_TYPE_SENT, subId = 1)
        val b = smsMessage(boxId = Sms.MESSAGE_TYPE_SENT, subId = 1)
        assertTrue(a.compareSender(b))
    }

    @Test
    fun `compareSender returns false for two outgoing messages with different subId`() {
        val a = smsMessage(boxId = Sms.MESSAGE_TYPE_SENT, subId = 1)
        val b = smsMessage(boxId = Sms.MESSAGE_TYPE_SENT, subId = 2)
        assertFalse(a.compareSender(b))
    }

    @Test
    fun `compareSender returns true for two incoming messages with same subId and address`() {
        val a = smsMessage(boxId = Sms.MESSAGE_TYPE_INBOX, subId = 1, address = "+123")
        val b = smsMessage(boxId = Sms.MESSAGE_TYPE_INBOX, subId = 1, address = "+123")
        assertTrue(a.compareSender(b))
    }

    @Test
    fun `compareSender returns false for two incoming messages with different address`() {
        val a = smsMessage(boxId = Sms.MESSAGE_TYPE_INBOX, subId = 1, address = "+123")
        val b = smsMessage(boxId = Sms.MESSAGE_TYPE_INBOX, subId = 1, address = "+456")
        assertFalse(a.compareSender(b))
    }

    @Test
    fun `compareSender returns false when one is incoming and other is outgoing`() {
        val incoming = smsMessage(boxId = Sms.MESSAGE_TYPE_INBOX)
        val outgoing = smsMessage(boxId = Sms.MESSAGE_TYPE_SENT)
        assertFalse(incoming.compareSender(outgoing))
        assertFalse(outgoing.compareSender(incoming))
    }
}
