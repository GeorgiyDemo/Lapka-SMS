/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
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
package org.lapka.sms.feature.blocking.messages

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import org.lapka.sms.R
import org.lapka.sms.common.base.QkRealmAdapter
import org.lapka.sms.common.base.QkViewHolder
import org.lapka.sms.common.util.DateFormatter
import org.lapka.sms.common.util.extensions.resolveThemeColor
import org.lapka.sms.model.Conversation
import org.lapka.sms.util.Preferences
import org.lapka.sms.common.widget.GroupAvatarView
import org.lapka.sms.common.widget.QkTextView
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class BlockedMessagesAdapter @Inject constructor(
    private val context: Context,
    private val dateFormatter: DateFormatter
) : QkRealmAdapter<Conversation>() {

    val clicks: PublishSubject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.blocked_list_item, parent, false)

        if (viewType == 0) {
            view.findViewById<QkTextView>(R.id.title)
                .setTypeface(view.findViewById<QkTextView>(R.id.title).typeface, Typeface.BOLD)
            view.findViewById<QkTextView>(R.id.date)
                .setTypeface(view.findViewById<QkTextView>(R.id.date).typeface, Typeface.BOLD)
            view.findViewById<QkTextView>(R.id.date)
                .setTextColor(view.context.resolveThemeColor(android.R.attr.textColorPrimary))
        }

        return QkViewHolder(view).apply {
            view.setOnClickListener {
                val conversation = getItem(adapterPosition) ?: return@setOnClickListener
                when (toggleSelection(conversation.id, false)) {
                    true -> view.isActivated = isSelected(conversation.id)
                    false -> clicks.onNext(conversation.id)
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

        holder.containerView.isActivated = isSelected(conversation.id)

        holder.itemView.findViewById<GroupAvatarView>(R.id.avatars).recipients = conversation.recipients
        holder.itemView.findViewById<QkTextView>(R.id.title).collapseEnabled = conversation.recipients.size > 1
        holder.itemView.findViewById<QkTextView>(R.id.title).text = conversation.getTitle()
        holder.itemView.findViewById<QkTextView>(R.id.date).text =
            dateFormatter.getConversationTimestamp(conversation.date)

        holder.itemView.findViewById<QkTextView>(R.id.blocker).text = when (conversation.blockingClient) {
            Preferences.BLOCKING_MANAGER_CC -> context.getString(R.string.blocking_manager_call_control_title)
            Preferences.BLOCKING_MANAGER_SIA -> context.getString(R.string.blocking_manager_sia_title)
            else -> null
        }

        holder.itemView.findViewById<QkTextView>(R.id.reason).text = conversation.blockReason
        holder.itemView.findViewById<QkTextView>(R.id.blocker).isVisible =
            holder.itemView.findViewById<QkTextView>(R.id.blocker).text.isNotEmpty()
        holder.itemView.findViewById<QkTextView>(R.id.reason).isVisible =
            holder.itemView.findViewById<QkTextView>(R.id.blocker).text.isNotEmpty()
    }

    override fun getItemViewType(position: Int): Int {
        val conversation = getItem(position)
        return if (conversation?.unread == false) 1 else 0
    }

}
