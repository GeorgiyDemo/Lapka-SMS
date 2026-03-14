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
package org.lapka.sms.feature.compose.editing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.lapka.sms.R
import org.lapka.sms.common.base.QkAdapter
import org.lapka.sms.common.base.QkViewHolder
import org.lapka.sms.common.util.Colors
import org.lapka.sms.common.util.extensions.forwardTouches
import org.lapka.sms.common.util.extensions.setTint
import org.lapka.sms.extensions.associateByNotNull
import org.lapka.sms.model.Contact
import org.lapka.sms.model.ContactGroup
import org.lapka.sms.model.Conversation
import org.lapka.sms.model.Recipient
import org.lapka.sms.repository.ConversationRepository
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import org.lapka.sms.common.widget.GroupAvatarView
import org.lapka.sms.common.widget.QkTextView
import javax.inject.Inject

class ComposeItemAdapter @Inject constructor(
    private val colors: Colors,
    private val conversationRepo: ConversationRepository
) : QkAdapter<ComposeItem>() {

    val clicks: Subject<ComposeItem> = PublishSubject.create()
    val longClicks: Subject<ComposeItem> = PublishSubject.create()

    private val numbersViewPool = RecyclerView.RecycledViewPool()
    private val disposables = CompositeDisposable()

    var recipients: Map<String, Recipient> = mapOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.contact_list_item, parent, false)

        view.findViewById<AppCompatImageView>(R.id.icon).setTint(colors.theme().theme)

        view.findViewById<RecyclerView>(R.id.numbers).setRecycledViewPool(numbersViewPool)
        view.findViewById<RecyclerView>(R.id.numbers).adapter = PhoneNumberAdapter()
        view.findViewById<RecyclerView>(R.id.numbers).forwardTouches(view)

        return QkViewHolder(view).apply {
            view.setOnClickListener {
                val item = getItem(adapterPosition)
                clicks.onNext(item)
            }
            view.setOnLongClickListener {
                val item = getItem(adapterPosition)
                longClicks.onNext(item)
                true
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val prevItem = if (position > 0) getItem(position - 1) else null
        val item = getItem(position)

        when (item) {
            is ComposeItem.New -> bindNew(holder, item.value)
            is ComposeItem.Recent -> bindRecent(holder, item.value, prevItem)
            is ComposeItem.Starred -> bindStarred(holder, item.value, prevItem)
            is ComposeItem.Person -> bindPerson(holder, item.value, prevItem)
            is ComposeItem.Group -> bindGroup(holder, item.value, prevItem)
        }
    }

    private fun bindNew(holder: QkViewHolder, contact: Contact) {
        holder.itemView.findViewById<TextView>(R.id.index).isVisible = false

        holder.itemView.findViewById<AppCompatImageView>(R.id.icon).isVisible = false

        holder.itemView.findViewById<GroupAvatarView>(R.id.avatar).recipients = listOf(createRecipient(contact))

        holder.itemView.findViewById<TextView>(R.id.title).text = contact.numbers.joinToString { it.address }

        holder.itemView.findViewById<TextView>(R.id.subtitle).isVisible = false

        holder.itemView.findViewById<RecyclerView>(R.id.numbers).isVisible = false
    }

    private fun bindRecent(holder: QkViewHolder, conversation: Conversation, prev: ComposeItem?) {
        holder.itemView.findViewById<TextView>(R.id.index).isVisible = false

        holder.itemView.findViewById<AppCompatImageView>(R.id.icon).isVisible = prev !is ComposeItem.Recent
        holder.itemView.findViewById<AppCompatImageView>(R.id.icon).setImageResource(R.drawable.ic_history_black_24dp)

        holder.itemView.findViewById<GroupAvatarView>(R.id.avatar).recipients = conversation.recipients

        holder.itemView.findViewById<TextView>(R.id.title).text = conversation.getTitle()

        holder.itemView.findViewById<TextView>(R.id.subtitle).isVisible =
            conversation.recipients.size > 1 && conversation.name.isBlank()
        holder.itemView.findViewById<TextView>(R.id.subtitle).text =
            conversation.recipients.joinToString(", ") { recipient ->
                recipient.contact?.name ?: recipient.address
            }
        holder.itemView.findViewById<QkTextView>(R.id.subtitle).collapseEnabled = conversation.recipients.size > 1

        holder.itemView.findViewById<RecyclerView>(R.id.numbers).isVisible = conversation.recipients.size == 1
        (holder.itemView.findViewById<RecyclerView>(R.id.numbers).adapter as PhoneNumberAdapter).data =
            conversation.recipients
                .mapNotNull { recipient -> recipient.contact }
                .flatMap { contact -> contact.numbers }
    }

    private fun bindStarred(holder: QkViewHolder, contact: Contact, prev: ComposeItem?) {
        holder.itemView.findViewById<TextView>(R.id.index).isVisible = false

        holder.itemView.findViewById<AppCompatImageView>(R.id.icon).isVisible = prev !is ComposeItem.Starred
        holder.itemView.findViewById<AppCompatImageView>(R.id.icon).setImageResource(R.drawable.ic_star_black_24dp)

        holder.itemView.findViewById<GroupAvatarView>(R.id.avatar).recipients = listOf(createRecipient(contact))

        holder.itemView.findViewById<TextView>(R.id.title).text = contact.name

        holder.itemView.findViewById<TextView>(R.id.subtitle).isVisible = false

        holder.itemView.findViewById<RecyclerView>(R.id.numbers).isVisible = true
        (holder.itemView.findViewById<RecyclerView>(R.id.numbers).adapter as PhoneNumberAdapter).data = contact.numbers
    }

    private fun bindGroup(holder: QkViewHolder, group: ContactGroup, prev: ComposeItem?) {
        holder.itemView.findViewById<TextView>(R.id.index).isVisible = false

        holder.itemView.findViewById<AppCompatImageView>(R.id.icon).isVisible = prev !is ComposeItem.Group
        holder.itemView.findViewById<AppCompatImageView>(R.id.icon).setImageResource(R.drawable.ic_people_black_24dp)

        holder.itemView.findViewById<GroupAvatarView>(R.id.avatar).recipients = group.contacts.map(::createRecipient)

        holder.itemView.findViewById<TextView>(R.id.title).text = group.title

        holder.itemView.findViewById<TextView>(R.id.subtitle).isVisible = true
        holder.itemView.findViewById<TextView>(R.id.subtitle).text = group.contacts.joinToString(", ") { it.name }
        holder.itemView.findViewById<QkTextView>(R.id.subtitle).collapseEnabled = group.contacts.size > 1

        holder.itemView.findViewById<RecyclerView>(R.id.numbers).isVisible = false
    }

    private fun bindPerson(holder: QkViewHolder, contact: Contact, prev: ComposeItem?) {
        holder.itemView.findViewById<TextView>(R.id.index).isVisible = true
        holder.itemView.findViewById<TextView>(R.id.index).text =
            if (contact.name.getOrNull(0)?.isLetter() == true) contact.name[0].toString() else "#"
        holder.itemView.findViewById<TextView>(R.id.index).isVisible = prev !is ComposeItem.Person ||
                (contact.name[0].isLetter() && !contact.name[0].equals(prev.value.name[0], ignoreCase = true)) ||
                (!contact.name[0].isLetter() && prev.value.name[0].isLetter())

        holder.itemView.findViewById<AppCompatImageView>(R.id.icon).isVisible = false

        holder.itemView.findViewById<GroupAvatarView>(R.id.avatar).recipients = listOf(createRecipient(contact))

        holder.itemView.findViewById<TextView>(R.id.title).text = contact.name

        holder.itemView.findViewById<TextView>(R.id.subtitle).isVisible = false

        holder.itemView.findViewById<RecyclerView>(R.id.numbers).isVisible = true
        (holder.itemView.findViewById<RecyclerView>(R.id.numbers).adapter as PhoneNumberAdapter).data = contact.numbers
    }

    private fun createRecipient(contact: Contact): Recipient {
        return recipients[contact.lookupKey] ?: Recipient(
            address = contact.numbers.firstOrNull()?.address ?: "",
            contact = contact
        )
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        disposables += conversationRepo.getUnmanagedRecipients()
            .map { recipients -> recipients.associateByNotNull { recipient -> recipient.contact?.lookupKey } }
            .subscribe { recipients -> this@ComposeItemAdapter.recipients = recipients }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        disposables.clear()
    }

    override fun areItemsTheSame(old: ComposeItem, new: ComposeItem): Boolean {
        val oldIds = old.getContacts().map { contact -> contact.lookupKey }
        val newIds = new.getContacts().map { contact -> contact.lookupKey }
        return oldIds == newIds
    }

    override fun areContentsTheSame(old: ComposeItem, new: ComposeItem): Boolean {
        return false
    }

}
