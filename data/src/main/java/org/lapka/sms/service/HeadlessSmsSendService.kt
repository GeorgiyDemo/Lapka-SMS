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
package org.lapka.sms.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.TelephonyManager
import org.lapka.sms.interactor.SendMessage
import org.lapka.sms.repository.ConversationRepository
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

class HeadlessSmsSendService : Service() {

    @Inject
    lateinit var conversationRepo: ConversationRepository

    @Inject
    lateinit var sendMessage: SendMessage

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != TelephonyManager.ACTION_RESPOND_VIA_MESSAGE) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        AndroidInjection.inject(this)

        val body = intent.extras?.getString(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
        val intentUri = intent.data
        val recipients = intentUri?.let(::getRecipients)?.split(";")

        if (body == null || recipients == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            val threadId = conversationRepo.getOrCreateConversation(recipients)?.id ?: 0L
            sendMessage.execute(SendMessage.Params(-1, threadId, recipients, body))
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun getRecipients(uri: Uri): String {
        val base = uri.schemeSpecificPart
        val position = base.indexOf('?')
        return if (position == -1) base else base.substring(0, position)
    }

}
