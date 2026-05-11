package org.lapka.sms.interactor

import org.lapka.sms.repository.ConversationRepository
import io.reactivex.Flowable
import javax.inject.Inject

class SetEncryptionEnabled @Inject constructor(
    private val conversationRepo: ConversationRepository
) : Interactor<SetEncryptionEnabled.Params>() {

    data class Params(val threadId: Long, val encryptionEnabled: Boolean?)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
            .doOnNext { (threadId, encryptionEnabled) ->
                conversationRepo.setEncryptionEnabled(threadId, encryptionEnabled)
            }
    }

}