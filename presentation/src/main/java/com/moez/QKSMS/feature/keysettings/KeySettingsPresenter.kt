package com.moez.QKSMS.feature.keysettings

import android.util.Base64
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkPresenter
import com.moez.QKSMS.extensions.Optional
import com.moez.QKSMS.extensions.asObservable
import com.moez.QKSMS.interactor.SetDeleteMessagesAfter
import com.moez.QKSMS.interactor.SetEncodingScheme
import com.moez.QKSMS.interactor.SetEncryptionEnabled
import com.moez.QKSMS.interactor.SetEncryptionKey
import com.moez.QKSMS.model.Conversation
import com.moez.QKSMS.repository.ConversationRepository
import com.moez.QKSMS.util.Preferences
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.crypto.KeyGenerator
import javax.inject.Inject
import javax.inject.Named

const val KeySettingsInvalidThreadId = -2L

class KeySettingsPresenter @Inject constructor(
    @Named("keySettingsConversationThreadId") threadId: Long,
    private val setDeleteMessagesAfter: SetDeleteMessagesAfter,
    private val setEncryptionKey: SetEncryptionKey,
    private val setEncodingScheme: SetEncodingScheme,
    private val setEncryptionEnabled: SetEncryptionEnabled,
    private val prefs: Preferences,
    private val conversationRepo: ConversationRepository
) : QkPresenter<KeySettingsView, KeySettingsState>(
    KeySettingsState(threadId = threadId)
) {

    private var initialized = false
    private var initialState: KeySettingsState? = null
    private var conversation: Subject<Optional<Conversation>> = BehaviorSubject.create()

    init {
        if (threadId == KeySettingsInvalidThreadId || threadId == 0L || threadId == -1L) {
            newState { copy(hasError = true) }
        } else {
            disposables += conversationRepo.getConversationAsync(threadId)
                .asObservable()
                .filter { conversation -> conversation.isLoaded }
                .filter { conversation -> conversation.isValid }
                .filter { conversation -> conversation.id != 0L }
                .subscribe { conv ->
                    if (!initialized) {
                        conversation.onNext(Optional(conv))
                        newState {
                            val state = copy(
                                key = conv.encryptionKey,
                                keyEnabled = conv.encryptionKey.isNotEmpty(),
                                keySettingsIsShown = false,
                                resetKeyIsShown = conv.encryptionKey.isNotEmpty(),
                                keyValid = validateKey(conv.encryptionKey),
                                encodingScheme = conv.encodingSchemeId
                                    .takeIf { it != Conversation.SCHEME_NOT_DEF }
                                    ?: 0,
                                deleteEncryptedAfter = conv.deleteEncryptedAfter,
                                deleteReceivedAfter = conv.deleteReceivedAfter,
                                deleteSentAfter = conv.deleteSentAfter,
                                threadId = threadId,
                            )
                            initialState = state
                            state
                        }
                        initialized = true
                    }
                }
        }
    }

    override fun bindIntents(view: KeySettingsView) {
        super.bindIntents(view)

        view.preferenceClicks()
            .autoDisposable(view.scope())
            .subscribe {
                when (it.id) {
                    R.id.enableKey -> {
                        newState {
                            if (key.isNotBlank()) {
                                if (initialState?.key?.isNotBlank() == true && initialState?.key == key) {
                                    view.showResetKeyDialog(true)
                                    copy()
                                } else {
                                    copy(
                                        key = "",
                                        keyEnabled = false,
                                        keySettingsIsShown = false,
                                        keyValid = false
                                    )
                                }
                            } else {
                                copy(
                                    key = generateKey(),
                                    keyEnabled = true,
                                    keySettingsIsShown = true,
                                    keyValid = true
                                )
                            }

                        }
                    }

                    R.id.scanQr -> {
                        view.scanQrCode()
                    }

                    R.id.generateKey -> {
                        newState {
                            copy(key = generateKey(), keyValid = true)
                        }
                    }

                    R.id.resetKey -> {
                        view.showResetKeyDialog(false)
                    }

                }
            }

        view.keyResetConfirmed
            .autoDisposable(view.scope())
            .subscribe {
                newState {
                    copy(
                        key = generateKey(),
                        keyEnabled = true,
                        keySettingsIsShown = true,
                        keyValid = true,
                        resetKeyIsShown = true
                    )
                }
            }

        view.keyDisableConfirmed
            .autoDisposable(view.scope())
            .subscribe {
                newState {
                    copy(
                        key = "",
                        keyEnabled = false,
                        keySettingsIsShown = false,
                        keyValid = false,
                        resetKeyIsShown = false
                    )
                }
            }

        view.optionsItemIntent
            .withLatestFrom(state, conversation) { itemId, lastState, conv ->
                when (itemId) {
                    R.id.confirm -> {
                        if (lastState.allowSave) {
                            if (lastState != initialState) {
                                saveChanges(lastState, conv.value, view)
                            }
                            view.goBack()
                        }
                    }
                }

            }
            .autoDisposable(view.scope())
            .subscribe()

        view.backClicked
            .withLatestFrom(state) { _, latestState ->
                if (latestState != initialState) {
                    view.showSaveDialog(latestState.allowSave)
                } else {
                    view.goBack()
                }
            }
            .autoDisposable(view.scope())
            .subscribe()

        view.exitWithSavingIntent
            .withLatestFrom(state, conversation) { withSaving, lastState, conv ->
                if (withSaving) {
                    saveChanges(lastState, conv.value, view)
                }
                view.goBack()
            }
            .autoDisposable(view.scope())
            .subscribe()

        view.qrScannedIntent
            .autoDisposable(view.scope())
            .subscribe { key ->
                if (validateKey(key)) {
                    newState {
                        copy(
                            key = key,
                            keyEnabled = true,
                            keySettingsIsShown = true,
                            keyValid = true
                        )
                    }
                    view.keySet()
                } else {
                    view.keyNotSet()
                }
            }

        view.keyChanged()
            .withLatestFrom(state) { key, lastState ->
                if (key != lastState.key) {
                    if (validateKey(key)) {
                        newState { copy(key = key, keyValid = true) }
                    } else {
                        newState { copy(key = key, keyValid = false) }
                    }
                }
            }
            .autoDisposable(view.scope())
            .subscribe()

        view.schemeChanged
            .withLatestFrom(state) { scheme, state ->
                if (state.encodingScheme != scheme) {
                    newState { copy(encodingScheme = scheme) }
                }
            }
            .autoDisposable(view.scope())
            .subscribe()

        view.stateRestored
            .autoDisposable(view.scope())
            .subscribe { state ->
                val unpackedState = state.value
                if (unpackedState != null) {
                    if (unpackedState.threadId != -1L) {
                        conversation.onNext(Optional(conversationRepo.getConversationAsync(unpackedState.threadId)))
                    } else {
                        conversation.onNext(Optional(null))
                    }
                    newState { unpackedState }
                } else {
                    newState { copy(bound = true) }
                }
            }
    }

    private fun generateKey(): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return Base64.encodeToString(keyGen.generateKey().encoded, Base64.NO_WRAP)
    }

    private fun saveChanges(lastState: KeySettingsState, conversation: Conversation?, view: KeySettingsView) {
        if (!lastState.allowSave) {
            return
        }
        if (lastState.isConversation) {
            val threadId = lastState.threadId
            setDeleteMessagesAfter.execute(
                SetDeleteMessagesAfter.Params(
                    threadId,
                    SetDeleteMessagesAfter.MessageType.ENCRYPTED,
                    lastState.deleteEncryptedAfter
                )
            )
            setDeleteMessagesAfter.execute(
                SetDeleteMessagesAfter.Params(
                    threadId,
                    SetDeleteMessagesAfter.MessageType.RECEIVED,
                    lastState.deleteReceivedAfter
                )
            )
            setDeleteMessagesAfter.execute(
                SetDeleteMessagesAfter.Params(
                    threadId,
                    SetDeleteMessagesAfter.MessageType.SENT,
                    lastState.deleteSentAfter
                )
            )
            setEncryptionKey.execute(SetEncryptionKey.Params(threadId, lastState.key))
            setEncodingScheme.execute(SetEncodingScheme.Params(threadId, lastState.encodingScheme))
            if (conversation?.encryptionEnabled == true && lastState.key.isBlank()) {
                setEncryptionEnabled.execute(SetEncryptionEnabled.Params(threadId, null))
            } else if (conversation?.encryptionEnabled == null && lastState.key.isNotBlank()) {
                setEncryptionEnabled.execute(SetEncryptionEnabled.Params(threadId, true))
            }
        }
        initialState = lastState
        view.onSaved(if (lastState.keyValid) lastState.key else null)
    }

    private fun validateKey(text: String): Boolean {
        try {
            if (text.isEmpty()) {
                return false
            }
            val data = Base64.decode(text, Base64.DEFAULT)
            return data.size == 16 || data.size == 24 || data.size == 32
        } catch (ignored: IllegalArgumentException) {
            return false
        }
    }

}