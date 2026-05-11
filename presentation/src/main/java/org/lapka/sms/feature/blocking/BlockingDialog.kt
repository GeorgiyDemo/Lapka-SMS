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
package org.lapka.sms.feature.blocking

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context
import androidx.lifecycle.Lifecycle
import org.lapka.sms.R
import org.lapka.sms.blocking.BlockingClient
import org.lapka.sms.interactor.MarkBlocked
import org.lapka.sms.interactor.MarkUnblocked
import org.lapka.sms.repository.ConversationRepository
import org.lapka.sms.util.Preferences
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import timber.log.Timber
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// TODO: Once we have a custom dialog based on conductor, turn this into a controller
class BlockingDialog @Inject constructor(
    private val blockingManager: BlockingClient,
    private val context: Context,
    private val conversationRepo: ConversationRepository,
    private val prefs: Preferences,
    private val markBlocked: MarkBlocked,
    private val markUnblocked: MarkUnblocked
) {

    fun show(activity: AppCompatActivity, conversationIds: List<Long>, block: Boolean) =
        activity.lifecycleScope.launch {
            val addresses = conversationIds.toLongArray()
                .let { conversationRepo.getConversations(*it) }
                .flatMap { conversation -> conversation.recipients }
                .map { it.address }
                .distinct()

            if (addresses.isEmpty()) {
                return@launch
            }

            if (blockingManager.getClientCapability() == BlockingClient.Capability.BLOCK_WITHOUT_PERMISSION) {
                if (block) {
                    markBlocked.execute(MarkBlocked.Params(conversationIds, prefs.blockingManager.get(), null))
                    blockingManager.block(addresses)
                        .autoDisposable(activity.scope(Lifecycle.Event.ON_DESTROY))
                        .subscribe({}, { Timber.w(it) })
                } else {
                    markUnblocked.execute(conversationIds)
                    blockingManager.unblock(addresses)
                        .autoDisposable(activity.scope(Lifecycle.Event.ON_DESTROY))
                        .subscribe({}, { Timber.w(it) })
                }
            } else if (block == allBlocked(addresses)) {
                when (block) {
                    true -> markBlocked.execute(MarkBlocked.Params(conversationIds, prefs.blockingManager.get(), null))
                    false -> markUnblocked.execute(conversationIds)
                }
            } else {
                showDialog(activity, conversationIds, addresses, block)
            }
        }

    private suspend fun allBlocked(addresses: List<String>): Boolean = withContext(Dispatchers.IO) {
        addresses.all { address ->
            blockingManager.isBlacklisted(address).blockingGet() is BlockingClient.Action.Block
        }
    }

    private suspend fun showDialog(
        activity: AppCompatActivity,
        conversationIds: List<Long>,
        addresses: List<String>,
        block: Boolean
    ) = withContext(Dispatchers.Main) {
        val res = when (block) {
            true -> R.plurals.blocking_block_external
            false -> R.plurals.blocking_unblock_external
        }

        val manager = context.getString(
            when (prefs.blockingManager.get()) {
                Preferences.BLOCKING_MANAGER_CB -> R.string.blocking_manager_call_blocker_title
                Preferences.BLOCKING_MANAGER_CC -> R.string.blocking_manager_call_control_title
                Preferences.BLOCKING_MANAGER_SIA -> R.string.blocking_manager_sia_title
                else -> R.string.blocking_manager_qksms_title
            }
        )

        val message = context.resources.getQuantityString(res, addresses.size, manager)

        // Otherwise, show a dialog asking the user if they want to be directed to the external
        // blocking manager
        MaterialAlertDialogBuilder(activity)
            .setTitle(
                when (block) {
                    true -> R.string.blocking_block_title
                    false -> R.string.blocking_unblock_title
                }
            )
            .setMessage(message)
            .setPositiveButton(R.string.button_continue) { _, _ ->
                if (block) {
                    markBlocked.execute(MarkBlocked.Params(conversationIds, prefs.blockingManager.get(), null))
                    blockingManager.block(addresses)
                        .autoDisposable(activity.scope(Lifecycle.Event.ON_DESTROY))
                        .subscribe({}, { Timber.w(it) })
                } else {
                    markUnblocked.execute(conversationIds)
                    blockingManager.unblock(addresses)
                        .autoDisposable(activity.scope(Lifecycle.Event.ON_DESTROY))
                        .subscribe({}, { Timber.w(it) })
                }
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> }
            .create()
            .show()
    }

}
