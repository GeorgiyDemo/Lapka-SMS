package org.lapka.sms.util

import org.lapka.sms.model.Conversation

/**
 * Identifies duplicate conversations by comparing recipient phone numbers.
 * Two conversations are considered duplicates if they have the same number of recipients
 * and all recipients match by phone number (using fuzzy comparison).
 */
class ConversationDeduplicator(
    private val phoneNumberComparator: (String, String) -> Boolean
) {

    /**
     * Represents a merge action: messages from [sourceId] should be moved to [targetId],
     * and settings should be migrated.
     */
    data class MergeAction(
        val targetId: Long,
        val sourceId: Long
    )

    /**
     * Finds a conversation from [candidates] that has matching recipients to [conversation].
     * Returns null if no duplicate is found.
     */
    fun findDuplicate(
        conversation: ConversationInfo,
        candidates: List<ConversationInfo>
    ): ConversationInfo? {
        return candidates.firstOrNull { candidate ->
            candidate.id != conversation.id &&
            candidate.recipientAddresses.size == conversation.recipientAddresses.size &&
            conversation.recipientAddresses.all { addr ->
                candidate.recipientAddresses.any { candidateAddr ->
                    phoneNumberComparator(addr, candidateAddr)
                }
            }
        }
    }

    /**
     * Given a list of conversations, returns merge actions for all duplicates.
     * The conversation with the most recent message becomes the target (primary).
     */
    fun findDuplicateGroups(conversations: List<ConversationInfo>): List<MergeAction> {
        val merged = mutableSetOf<Long>()
        val actions = mutableListOf<MergeAction>()

        for (conv in conversations) {
            if (conv.id in merged) continue

            val duplicates = conversations.filter { other ->
                other.id != conv.id && other.id !in merged &&
                other.recipientAddresses.size == conv.recipientAddresses.size &&
                conv.recipientAddresses.all { addr ->
                    other.recipientAddresses.any { otherAddr ->
                        phoneNumberComparator(addr, otherAddr)
                    }
                }
            }

            if (duplicates.isEmpty()) continue

            val allCandidates = listOf(conv) + duplicates
            val primary = allCandidates.maxByOrNull { it.lastMessageDate } ?: conv

            for (dup in allCandidates) {
                if (dup.id == primary.id) continue
                actions.add(MergeAction(targetId = primary.id, sourceId = dup.id))
                merged.add(dup.id)
            }
        }

        return actions
    }

    /**
     * Minimal conversation info needed for deduplication.
     */
    data class ConversationInfo(
        val id: Long,
        val recipientAddresses: List<String>,
        val lastMessageDate: Long = 0
    )
}
