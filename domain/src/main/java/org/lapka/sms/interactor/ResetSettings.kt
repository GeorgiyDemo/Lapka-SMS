package org.lapka.sms.interactor

import org.lapka.sms.repository.ConversationRepository
import org.lapka.sms.util.Preferences
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
                prefs.smsForReset.set("")
                prefs.smsForResetHash.set("")
            }
    }

}