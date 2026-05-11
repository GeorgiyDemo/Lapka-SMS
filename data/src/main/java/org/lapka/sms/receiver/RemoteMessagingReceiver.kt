/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.lapka.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.core.app.RemoteInput
import org.lapka.sms.compat.SubscriptionManagerCompat
import org.lapka.sms.crypto.ConversationKeyStore
import org.lapka.sms.crypto.KSmsEncryptorFactory
import org.lapka.sms.interactor.MarkRead
import org.lapka.sms.interactor.SendMessage
import org.lapka.sms.model.Conversation
import org.lapka.sms.repository.ConversationRepository
import org.lapka.sms.repository.MessageRepository
import org.lapka.sms.util.Preferences
import dagger.android.AndroidInjection
import org.lapka.sms.Message as PSmsMessage
import javax.inject.Inject

class RemoteMessagingReceiver : BroadcastReceiver() {

    @Inject
    lateinit var conversationRepo: ConversationRepository

    @Inject
    lateinit var markRead: MarkRead

    @Inject
    lateinit var messageRepo: MessageRepository

    @Inject
    lateinit var sendMessage: SendMessage

    @Inject
    lateinit var subscriptionManager: SubscriptionManagerCompat

    @Inject
    lateinit var prefs: Preferences

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
        val bundle = intent.extras ?: return

        val threadId = bundle.getLong("threadId")
        val body = remoteInput.getCharSequence("body").toString()
        markRead.execute(listOf(threadId))

        val conversation = conversationRepo.getConversation(threadId) ?: return

        // Encrypt the reply if the conversation has encryption enabled
        val encryptedBody = encryptIfNeeded(body, conversation) ?: return

        val lastMessage = messageRepo.getMessages(threadId).lastOrNull()
        val subId = subscriptionManager.activeSubscriptionInfoList
            .firstOrNull { it.subscriptionId == lastMessage?.subId }
            ?.subscriptionId ?: -1
        val addresses = conversation.recipients.map { it.address }

        val pendingRepository = goAsync()
        sendMessage.execute(SendMessage.Params(subId, threadId, addresses, encryptedBody)) { pendingRepository.finish() }
    }

    private fun encryptIfNeeded(body: String, conversation: Conversation): String? {
        val encryptionKey = conversation.encryptionKey.takeIf { it.isNotBlank() }
            ?: return body

        val encryptionEnabled = conversation.encryptionEnabled ?: true
        if (!encryptionEnabled) return body

        val encryptionSchemeId = conversation.encodingSchemeId
            .takeIf { it != Conversation.SCHEME_NOT_DEF }
            ?: prefs.encodingScheme.get()

        return try {
            KSmsEncryptorFactory.create().encode(
                message = PSmsMessage(body),
                key = ConversationKeyStore.unwrapKeyBytes(encryptionKey),
                encryptionSchemeId = encryptionSchemeId
            )
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to encrypt notification reply, message not sent")
            null
        }
    }
}
