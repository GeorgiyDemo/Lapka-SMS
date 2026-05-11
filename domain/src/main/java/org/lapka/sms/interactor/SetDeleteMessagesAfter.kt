package org.lapka.sms.interactor

import org.lapka.sms.repository.ConversationRepository
import io.reactivex.Flowable
import javax.inject.Inject

class SetDeleteMessagesAfter @Inject constructor(
    private val conversationRepo: ConversationRepository
) : Interactor<SetDeleteMessagesAfter.Params>() {

    enum class MessageType { ENCRYPTED, RECEIVED, SENT }
    data class Params(val threadId: Long, val type: MessageType, val durationId: Int)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
            .doOnNext { (threadId, type, durationId) ->
                when (type) {
                    MessageType.ENCRYPTED -> conversationRepo.setDeleteEncryptedAfter(threadId, durationId)
                    MessageType.RECEIVED -> conversationRepo.setDeleteReceivedAfter(threadId, durationId)
                    MessageType.SENT -> conversationRepo.setDeleteSentAfter(threadId, durationId)
                }
            }
    }

}