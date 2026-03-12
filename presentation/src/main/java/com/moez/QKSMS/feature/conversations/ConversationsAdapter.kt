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
package com.moez.QKSMS.feature.conversations

import android.content.Context
import android.graphics.Typeface
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import org.lapka.sms.InvalidVersionException
import com.moez.QKSMS.crypto.KSmsEncryptorFactory
import org.lapka.sms.Message as PSmsMessage
import com.moez.QKSMS.R
import com.moez.QKSMS.common.Navigator
import com.moez.QKSMS.common.base.QkRealmAdapter
import com.moez.QKSMS.common.base.QkViewHolder
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.DateFormatter
import com.moez.QKSMS.common.util.extensions.resolveThemeColor
import com.moez.QKSMS.common.util.extensions.setTint
import com.moez.QKSMS.model.Conversation
import com.moez.QKSMS.util.PhoneNumberUtils
import com.moez.QKSMS.common.widget.GroupAvatarView
import com.moez.QKSMS.common.widget.QkTextView
import com.moez.QKSMS.util.Preferences
import javax.inject.Inject
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ConversationsAdapter @Inject constructor(
    private val colors: Colors,
    private val context: Context,
    private val dateFormatter: DateFormatter,
    private val navigator: Navigator,
    private val phoneNumberUtils: PhoneNumberUtils,
    private val prefs: Preferences
) : QkRealmAdapter<Conversation>() {

    init {
        // This is how we access the threadId for the swipe actions
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.conversation_list_item, parent, false)

        if (viewType == 1) {
            val textColorPrimary = parent.context.resolveThemeColor(android.R.attr.textColorPrimary)

            view.findViewById<QkTextView>(R.id.title)
                .setTypeface(view.findViewById<QkTextView>(R.id.title).typeface, Typeface.BOLD)

            view.findViewById<QkTextView>(R.id.snippet)
                .setTypeface(view.findViewById<QkTextView>(R.id.snippet).typeface, Typeface.BOLD)
            view.findViewById<QkTextView>(R.id.snippet).setTextColor(textColorPrimary)
            view.findViewById<QkTextView>(R.id.snippet).maxLines = 5

            view.findViewById<ImageView>(R.id.unread).isVisible = true

            view.findViewById<QkTextView>(R.id.date)
                .setTypeface(view.findViewById<QkTextView>(R.id.date).typeface, Typeface.BOLD)
            view.findViewById<QkTextView>(R.id.date).setTextColor(textColorPrimary)
        }

        return QkViewHolder(view).apply {
            view.setOnClickListener {
                val conversation = getItem(adapterPosition) ?: return@setOnClickListener
                when (toggleSelection(conversation.id, false)) {
                    true -> view.isActivated = isSelected(conversation.id)
                    false -> navigator.showConversation(conversation.id)
                }
            }
            view.setOnLongClickListener {
                val conversation = getItem(adapterPosition) ?: return@setOnLongClickListener true
                toggleSelection(conversation.id)
                view.isActivated = isSelected(conversation.id)
                true
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val conversation = getItem(position) ?: return

        // If the last message wasn't incoming, then the colour doesn't really matter anyway
        val lastMessage = conversation.lastMessage
        val recipient = when {
            conversation.recipients.size == 1 || lastMessage == null -> conversation.recipients.firstOrNull()
            else -> conversation.recipients.find { recipient ->
                phoneNumberUtils.compare(recipient.address, lastMessage.address)
            }
        }
        val theme = colors.theme(recipient).theme

        holder.containerView.isActivated = isSelected(conversation.id)

        holder.itemView.findViewById<GroupAvatarView>(R.id.avatars).recipients = conversation.recipients
        holder.itemView.findViewById<QkTextView>(R.id.title).collapseEnabled = conversation.recipients.size > 1
        holder.itemView.findViewById<QkTextView>(R.id.title).text = buildSpannedString {
            append(conversation.getTitle())
            if (conversation.draft.isNotEmpty()) {
                color(theme) { append(" " + context.getString(R.string.main_draft)) }
            }
        }
        holder.itemView.findViewById<QkTextView>(R.id.date).text =
            conversation.date.takeIf { it > 0 }?.let(dateFormatter::getConversationTimestamp)

        val snippetMessage = try {
            if (conversation.encryptionKey.isNotEmpty()) {
                KSmsEncryptorFactory.create().tryDecode(
                    conversation.snippet.toString(),
                    Base64.decode(conversation.encryptionKey, Base64.DEFAULT)
                )
            } else if (prefs.globalEncryptionKey.get().isNotEmpty()) {
                KSmsEncryptorFactory.create().tryDecode(
                    conversation.snippet.toString(),
                    Base64.decode(prefs.globalEncryptionKey.get(), Base64.DEFAULT)
                )
            } else {
                PSmsMessage(conversation.snippet ?: "")
            }
        } catch (_: InvalidVersionException) {
            PSmsMessage(conversation.snippet ?: "")
        }


        val snippetText = if (snippetMessage.channelId != null) {
            val channelIdStr = context.resources.getString(R.string.channel_id)
            snippetMessage.text + " (${channelIdStr}: ${snippetMessage.channelId})"
        } else {
            snippetMessage.text
        }

        holder.itemView.findViewById<QkTextView>(R.id.snippet).text = when {
            conversation.draft.isNotEmpty() -> conversation.draft
            conversation.me -> context.getString(R.string.main_sender_you, snippetText)
            else -> snippetText
        }
        holder.itemView.findViewById<ImageView>(R.id.pinned).isVisible = conversation.pinned
        holder.itemView.findViewById<ImageView>(R.id.unread).setTint(theme)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: -1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.unread == false) 0 else 1
    }
}
