package com.moez.QKSMS.interactor

import com.moez.QKSMS.repository.ConversationRepository
import io.reactivex.Flowable
import javax.inject.Inject

class SetEncodingScheme @Inject constructor(
    private val conversationRepo: ConversationRepository
) : Interactor<SetEncodingScheme.Params>() {

    data class Params(val threadId: Long, val encodingSchemeId: Int)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
            .doOnNext { (threadId, encodingSchemeId) ->
                conversationRepo.setEncodingScheme(threadId, encodingSchemeId)
            }
    }

}