package com.moez.QKSMS.interactor

import com.moez.QKSMS.repository.ConversationRepository
import io.reactivex.Flowable
import javax.inject.Inject

class SetEncryptionKey @Inject constructor(
    private val conversationRepo: ConversationRepository
) : Interactor<SetEncryptionKey.Params>() {

    data class Params(val threadId: Long, val encryptionKey: String)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
            .doOnNext { (threadId, encryptionKey) ->
                conversationRepo.setEncryptionKey(threadId, encryptionKey)
            }
    }

}