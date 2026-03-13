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
import com.moez.QKSMS.crypto.ConversationKeyStore
import org.lapka.sms.InvalidVersionException
import org.lapka.sms.PSmsEncryptor
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
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class ConversationsAdapter @Inject constructor(
    private val colors: Colors,
    private val context: Context,
    private val dateFormatter: DateFormatter,
    private val navigator: Navigator,
    private val phoneNumberUtils: PhoneNumberUtils,
    private val prefs: Preferences
) : QkRealmAdapter<Conversation>() {

    private data class CachedSnippet(val originalSnippet: String, val decoded: PSmsMessage)

    private val snippetCache = android.util.LruCache<Long, CachedSnippet>(128)
    private val decryptionDisposables = HashMap<RecyclerView.ViewHolder, Disposable>()

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

        // Cancel any pending decryption for this holder
        decryptionDisposables.remove(holder)?.dispose()

        val snippet = conversation.snippet ?: ""
        val conversationId = conversation.id
        val encryptionKey = conversation.encryptionKey
        val draft = conversation.draft
        val isMe = conversation.me

        if (encryptionKey.isNotEmpty()) {
            val cached = snippetCache.get(conversationId)
            if (cached != null && cached.originalSnippet == snippet.toString()) {
                bindSnippet(holder, cached.decoded, draft, isMe)
            } else {
                // Show original text while decrypting
                bindSnippet(holder, PSmsMessage(snippet.toString()), draft, isMe)

                val snippetStr = snippet.toString()
                holder.itemView.tag = conversationId
                val disposable = Single.fromCallable {
                    try {
                        PSmsEncryptor().tryDecode(
                            snippetStr,
                            ConversationKeyStore.unwrapKeyBytes(encryptionKey)
                        )
                    } catch (_: InvalidVersionException) {
                        PSmsMessage(snippetStr)
                    }
                }
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ decoded ->
                        snippetCache.put(conversationId, CachedSnippet(snippetStr, decoded))
                        if (holder.itemView.tag == conversationId) {
                            bindSnippet(holder, decoded, draft, isMe)
                        }
                    }, { })
                decryptionDisposables[holder] = disposable
            }
        } else {
            bindSnippet(holder, PSmsMessage(snippet.toString()), draft, isMe)
        }
        holder.itemView.findViewById<ImageView>(R.id.pinned).isVisible = conversation.pinned
        holder.itemView.findViewById<ImageView>(R.id.unread).setTint(theme)
    }

    private fun bindSnippet(holder: QkViewHolder, snippetMessage: PSmsMessage, draft: String, isMe: Boolean) {
        val snippetText = if (snippetMessage.channelId != null) {
            val channelIdStr = context.resources.getString(R.string.channel_id)
            snippetMessage.text + " (${channelIdStr}: ${snippetMessage.channelId})"
        } else {
            snippetMessage.text
        }

        holder.itemView.findViewById<QkTextView>(R.id.snippet).text = when {
            draft.isNotEmpty() -> draft
            isMe -> context.getString(R.string.main_sender_you, snippetText)
            else -> snippetText
        }
    }

    override fun onViewRecycled(holder: QkViewHolder) {
        super.onViewRecycled(holder)
        decryptionDisposables.remove(holder)?.dispose()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        decryptionDisposables.values.forEach { it.dispose() }
        decryptionDisposables.clear()
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: -1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.unread == false) 0 else 1
    }
}
