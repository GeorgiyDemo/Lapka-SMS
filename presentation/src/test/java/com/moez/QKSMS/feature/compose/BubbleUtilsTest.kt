package com.moez.QKSMS.feature.compose

import com.moez.QKSMS.R
import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleUtilsTest {

    // --- getBubble ---

    @Test
    fun `getBubble returns emoji drawable when emojiOnly is true`() {
        val result = BubbleUtils.getBubble(
            emojiOnly = true,
            canGroupWithPrevious = false,
            canGroupWithNext = false,
            isMe = false
        )
        assertEquals(R.drawable.message_emoji, result)
    }

    @Test
    fun `getBubble returns emoji drawable regardless of other flags`() {
        // emojiOnly takes priority over all other flags
        assertEquals(
            R.drawable.message_emoji,
            BubbleUtils.getBubble(emojiOnly = true, canGroupWithPrevious = true, canGroupWithNext = true, isMe = true)
        )
        assertEquals(
            R.drawable.message_emoji,
            BubbleUtils.getBubble(emojiOnly = true, canGroupWithPrevious = false, canGroupWithNext = true, isMe = false)
        )
    }

    @Test
    fun `getBubble returns first outgoing when no previous but has next`() {
        val result = BubbleUtils.getBubble(
            emojiOnly = false,
            canGroupWithPrevious = false,
            canGroupWithNext = true,
            isMe = true
        )
        assertEquals(R.drawable.message_out_first, result)
    }

    @Test
    fun `getBubble returns first incoming when no previous but has next`() {
        val result = BubbleUtils.getBubble(
            emojiOnly = false,
            canGroupWithPrevious = false,
            canGroupWithNext = true,
            isMe = false
        )
        assertEquals(R.drawable.message_in_first, result)
    }

    @Test
    fun `getBubble returns middle outgoing when has both previous and next`() {
        val result = BubbleUtils.getBubble(
            emojiOnly = false,
            canGroupWithPrevious = true,
            canGroupWithNext = true,
            isMe = true
        )
        assertEquals(R.drawable.message_out_middle, result)
    }

    @Test
    fun `getBubble returns middle incoming when has both previous and next`() {
        val result = BubbleUtils.getBubble(
            emojiOnly = false,
            canGroupWithPrevious = true,
            canGroupWithNext = true,
            isMe = false
        )
        assertEquals(R.drawable.message_in_middle, result)
    }

    @Test
    fun `getBubble returns last outgoing when has previous but no next`() {
        val result = BubbleUtils.getBubble(
            emojiOnly = false,
            canGroupWithPrevious = true,
            canGroupWithNext = false,
            isMe = true
        )
        assertEquals(R.drawable.message_out_last, result)
    }

    @Test
    fun `getBubble returns last incoming when has previous but no next`() {
        val result = BubbleUtils.getBubble(
            emojiOnly = false,
            canGroupWithPrevious = true,
            canGroupWithNext = false,
            isMe = false
        )
        assertEquals(R.drawable.message_in_last, result)
    }

    @Test
    fun `getBubble returns only drawable when standalone message outgoing`() {
        val result = BubbleUtils.getBubble(
            emojiOnly = false,
            canGroupWithPrevious = false,
            canGroupWithNext = false,
            isMe = true
        )
        assertEquals(R.drawable.message_only, result)
    }

    @Test
    fun `getBubble returns only drawable when standalone message incoming`() {
        val result = BubbleUtils.getBubble(
            emojiOnly = false,
            canGroupWithPrevious = false,
            canGroupWithNext = false,
            isMe = false
        )
        assertEquals(R.drawable.message_only, result)
    }

    // --- TIMESTAMP_THRESHOLD ---

    @Test
    fun `timestamp threshold is 10 minutes`() {
        assertEquals(10, BubbleUtils.TIMESTAMP_THRESHOLD)
    }
}
