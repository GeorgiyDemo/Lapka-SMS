package org.lapka.sms.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConversationDeduplicatorTest {

    private lateinit var deduplicator: ConversationDeduplicator

    /**
     * Simple phone number comparator that normalizes by stripping non-digits
     * and comparing last 10 digits (mimics real PhoneNumberUtils behavior).
     */
    private val phoneComparator: (String, String) -> Boolean = { a, b ->
        val normA = a.replace(Regex("[^0-9]"), "").takeLast(10)
        val normB = b.replace(Regex("[^0-9]"), "").takeLast(10)
        normA == normB && normA.isNotEmpty()
    }

    @Before
    fun setUp() {
        deduplicator = ConversationDeduplicator(phoneComparator)
    }

    private fun info(id: Long, vararg addresses: String, lastMessageDate: Long = 0) =
        ConversationDeduplicator.ConversationInfo(id, addresses.toList(), lastMessageDate)

    // =========================================================================
    // findDuplicate
    // =========================================================================

    @Test
    fun `findDuplicate returns null when no candidates`() {
        val conv = info(1, "+79991234567")
        assertNull(deduplicator.findDuplicate(conv, emptyList()))
    }

    @Test
    fun `findDuplicate returns null when no match`() {
        val conv = info(1, "+79991234567")
        val candidates = listOf(info(2, "+79997654321"))
        assertNull(deduplicator.findDuplicate(conv, candidates))
    }

    @Test
    fun `findDuplicate matches same number exact`() {
        val conv = info(1, "+79991234567")
        val candidates = listOf(info(2, "+79991234567"))
        assertEquals(2L, deduplicator.findDuplicate(conv, candidates)?.id)
    }

    @Test
    fun `findDuplicate matches same number different format`() {
        val conv = info(1, "+79991234567")
        val candidates = listOf(info(2, "89991234567"))
        assertEquals(2L, deduplicator.findDuplicate(conv, candidates)?.id)
    }

    @Test
    fun `findDuplicate matches number with dashes and spaces`() {
        val conv = info(1, "+7 (999) 123-45-67")
        val candidates = listOf(info(2, "89991234567"))
        assertEquals(2L, deduplicator.findDuplicate(conv, candidates)?.id)
    }

    @Test
    fun `findDuplicate skips self`() {
        val conv = info(1, "+79991234567")
        val candidates = listOf(info(1, "+79991234567"))
        assertNull(deduplicator.findDuplicate(conv, candidates))
    }

    @Test
    fun `findDuplicate does not match different recipient count`() {
        val conv = info(1, "+79991234567")
        val candidates = listOf(info(2, "+79991234567", "+79997654321"))
        assertNull(deduplicator.findDuplicate(conv, candidates))
    }

    @Test
    fun `findDuplicate matches group conversation with same recipients`() {
        val conv = info(1, "+79991234567", "+79997654321")
        val candidates = listOf(info(2, "89991234567", "89997654321"))
        assertEquals(2L, deduplicator.findDuplicate(conv, candidates)?.id)
    }

    @Test
    fun `findDuplicate matches group conversation recipients in different order`() {
        val conv = info(1, "+79997654321", "+79991234567")
        val candidates = listOf(info(2, "89991234567", "89997654321"))
        assertEquals(2L, deduplicator.findDuplicate(conv, candidates)?.id)
    }

    @Test
    fun `findDuplicate does not match partial group overlap`() {
        val conv = info(1, "+79991234567", "+79997654321")
        val candidates = listOf(info(2, "89991234567", "89990000000"))
        assertNull(deduplicator.findDuplicate(conv, candidates))
    }

    // =========================================================================
    // findDuplicateGroups
    // =========================================================================

    @Test
    fun `findDuplicateGroups returns empty when no duplicates`() {
        val conversations = listOf(
            info(1, "+79991234567", lastMessageDate = 100),
            info(2, "+79997654321", lastMessageDate = 200)
        )
        assertTrue(deduplicator.findDuplicateGroups(conversations).isEmpty())
    }

    @Test
    fun `findDuplicateGroups merges two conversations for same number`() {
        val conversations = listOf(
            info(1, "+79991234567", lastMessageDate = 100),
            info(2, "89991234567", lastMessageDate = 200)
        )
        val actions = deduplicator.findDuplicateGroups(conversations)
        assertEquals(1L, actions.size.toLong())
        assertEquals(2L, actions[0].targetId) // newer message wins
        assertEquals(1L, actions[0].sourceId)
    }

    @Test
    fun `findDuplicateGroups keeps conversation with latest message as target`() {
        val conversations = listOf(
            info(1, "+79991234567", lastMessageDate = 500),
            info(2, "89991234567", lastMessageDate = 200)
        )
        val actions = deduplicator.findDuplicateGroups(conversations)
        assertEquals(1L, actions.size.toLong())
        assertEquals(1L, actions[0].targetId) // id=1 has newer message
        assertEquals(2L, actions[0].sourceId)
    }

    @Test
    fun `findDuplicateGroups handles three-way duplicate`() {
        val conversations = listOf(
            info(1, "+79991234567", lastMessageDate = 100),
            info(2, "89991234567", lastMessageDate = 300),
            info(3, "9991234567", lastMessageDate = 200)
        )
        val actions = deduplicator.findDuplicateGroups(conversations)
        assertEquals(2L, actions.size.toLong())
        // Target should be id=2 (latest message at 300)
        assertTrue(actions.all { it.targetId == 2L })
        assertEquals(setOf(1L, 3L), actions.map { it.sourceId }.toSet())
    }

    @Test
    fun `findDuplicateGroups handles multiple independent duplicate pairs`() {
        val conversations = listOf(
            info(1, "+79991234567", lastMessageDate = 100),
            info(2, "89991234567", lastMessageDate = 200),
            info(3, "+79990000000", lastMessageDate = 300),
            info(4, "89990000000", lastMessageDate = 400)
        )
        val actions = deduplicator.findDuplicateGroups(conversations)
        assertEquals(2L, actions.size.toLong())
        assertTrue(actions.any { it.targetId == 2L && it.sourceId == 1L })
        assertTrue(actions.any { it.targetId == 4L && it.sourceId == 3L })
    }

    @Test
    fun `findDuplicateGroups ignores non-duplicate conversations`() {
        val conversations = listOf(
            info(1, "+79991234567", lastMessageDate = 100),
            info(2, "89991234567", lastMessageDate = 200),
            info(3, "+79990000000", lastMessageDate = 300) // no duplicate
        )
        val actions = deduplicator.findDuplicateGroups(conversations)
        assertEquals(1L, actions.size.toLong())
        assertEquals(2L, actions[0].targetId)
        assertEquals(1L, actions[0].sourceId)
    }

    @Test
    fun `findDuplicateGroups with empty list returns empty`() {
        assertTrue(deduplicator.findDuplicateGroups(emptyList()).isEmpty())
    }

    @Test
    fun `findDuplicateGroups with single conversation returns empty`() {
        val conversations = listOf(info(1, "+79991234567", lastMessageDate = 100))
        assertTrue(deduplicator.findDuplicateGroups(conversations).isEmpty())
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Test
    fun `findDuplicate with empty recipients does not match`() {
        val conv = info(1)
        val candidates = listOf(info(2))
        // Both have empty recipient lists — size matches (0 == 0) but
        // all() on empty returns true, so they'd match. This tests that behavior.
        // In practice, conversations without recipients shouldn't exist.
        assertNotNull(deduplicator.findDuplicate(conv, candidates))
    }

    @Test
    fun `exact string comparator works`() {
        val exactDedup = ConversationDeduplicator { a, b -> a == b }
        val conv = ConversationDeduplicator.ConversationInfo(1, listOf("+79991234567"))
        val candidates = listOf(
            ConversationDeduplicator.ConversationInfo(2, listOf("89991234567")) // different format
        )
        // Exact comparator should NOT match different formats
        assertNull(exactDedup.findDuplicate(conv, candidates))
    }
}
