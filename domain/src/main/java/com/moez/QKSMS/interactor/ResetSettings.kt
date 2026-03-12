package com.moez.QKSMS.interactor

import com.moez.QKSMS.repository.ConversationRepository
import com.moez.QKSMS.util.Preferences
import io.reactivex.Flowable
import javax.inject.Inject

class ResetSettings @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val prefs: Preferences,
) : Interactor<ResetSettings.Params>() {

    class Params

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
            .doOnNext { _ ->
                conversationRepo.resetHiddenSettings()
                prefs.globalEncryptionKey.set("")
                prefs.smsForReset.set("")
                prefs.smsForResetHash.set("")
                prefs.deleteEncryptedAfter.set(0)
            }
    }

}