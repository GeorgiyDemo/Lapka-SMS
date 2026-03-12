package com.moez.QKSMS.feature.conversationinfo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.jakewharton.rxbinding2.view.clicks
import com.moez.QKSMS.R
import com.moez.QKSMS.common.QkDialog
import com.moez.QKSMS.common.base.QkAdapter
import com.moez.QKSMS.common.base.QkViewHolder
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.extensions.setTint
import com.moez.QKSMS.common.util.extensions.setVisible
import com.moez.QKSMS.feature.conversationinfo.ConversationInfoItem.*
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import android.widget.ImageView
import android.widget.TextView
import com.moez.QKSMS.common.widget.AvatarView
import com.moez.QKSMS.common.widget.PreferenceView
import javax.inject.Inject

class ConversationInfoAdapter @Inject constructor(
    private val context: Context,
    private val colors: Colors,
    val deleteEncryptedAfterDialog: QkDialog,
    val deleteReceivedAfterDialog: QkDialog,
    val deleteSentAfterDialog: QkDialog,
) : QkAdapter<ConversationInfoItem>() {

    val recipientClicks: Subject<Long> = PublishSubject.create()
    val recipientLongClicks: Subject<Long> = PublishSubject.create()
    val themeClicks: Subject<Long> = PublishSubject.create()
    val nameClicks: Subject<Unit> = PublishSubject.create()
    val notificationClicks: Subject<Unit> = PublishSubject.create()
    val archiveClicks: Subject<Unit> = PublishSubject.create()
    val blockClicks: Subject<Unit> = PublishSubject.create()
    val deleteClicks: Subject<Unit> = PublishSubject.create()
    val encryptionKeyClicks: Subject<Unit> = PublishSubject.create()
    val deleteEncryptedAfterClicks: Subject<Unit> = PublishSubject.create()
    val deleteReceivedAfterClicks: Subject<Unit> = PublishSubject.create()
    val deleteSentAfterClicks: Subject<Unit> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> QkViewHolder(inflater.inflate(R.layout.conversation_recipient_list_item, parent, false)).apply {
                itemView.setOnClickListener {
                    val item = getItem(adapterPosition) as? ConversationInfoRecipient
                    item?.value?.id?.run(recipientClicks::onNext)
                }

                itemView.setOnLongClickListener {
                    val item = getItem(adapterPosition) as? ConversationInfoRecipient
                    item?.value?.id?.run(recipientLongClicks::onNext)
                    true
                }

                itemView.findViewById<ImageView>(R.id.theme).setOnClickListener {
                    val item = getItem(adapterPosition) as? ConversationInfoRecipient
                    item?.value?.id?.run(themeClicks::onNext)
                }
            }

            1 -> QkViewHolder(inflater.inflate(R.layout.conversation_info_settings, parent, false)).apply {
                itemView.findViewById<PreferenceView>(R.id.groupName).clicks().subscribe(nameClicks)
                itemView.findViewById<PreferenceView>(R.id.notifications).clicks().subscribe(notificationClicks)
                itemView.findViewById<PreferenceView>(R.id.archive).clicks().subscribe(archiveClicks)
                itemView.findViewById<PreferenceView>(R.id.block).clicks().subscribe(blockClicks)
                itemView.findViewById<PreferenceView>(R.id.delete).clicks().subscribe(deleteClicks)
                itemView.findViewById<PreferenceView>(R.id.encryptionKey).clicks().subscribe(encryptionKeyClicks)
                itemView.findViewById<PreferenceView>(R.id.conversationDeleteEncryptedAfter).clicks().subscribe(deleteEncryptedAfterClicks)
                itemView.findViewById<PreferenceView>(R.id.conversationDeleteReceivedAfter).clicks().subscribe(deleteReceivedAfterClicks)
                itemView.findViewById<PreferenceView>(R.id.conversationDeleteSentAfter).clicks().subscribe(deleteSentAfterClicks)
            }

            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ConversationInfoRecipient -> {
                val recipient = item.value
                holder.itemView.findViewById<AvatarView>(R.id.avatar).setRecipient(recipient)

                holder.itemView.findViewById<TextView>(R.id.name).text = recipient.contact?.name ?: recipient.address

                holder.itemView.findViewById<TextView>(R.id.address).text = recipient.address
                holder.itemView.findViewById<TextView>(R.id.address).setVisible(recipient.contact != null)

                holder.itemView.findViewById<ImageView>(R.id.add).setVisible(recipient.contact == null)

                val theme = colors.theme(recipient)
                holder.itemView.findViewById<ImageView>(R.id.theme).setTint(theme.theme)
            }

            is ConversationInfoSettings -> {
                holder.itemView.findViewById<PreferenceView>(R.id.groupName).isVisible = item.recipients.size > 1
                holder.itemView.findViewById<PreferenceView>(R.id.groupName).summary = item.name

                holder.itemView.findViewById<PreferenceView>(R.id.notifications).isEnabled = !item.blocked

                holder.itemView.findViewById<PreferenceView>(R.id.archive).isEnabled = !item.blocked
                holder.itemView.findViewById<PreferenceView>(R.id.archive).title = context.getString(when (item.archived) {
                    true -> R.string.info_unarchive
                    false -> R.string.info_archive
                })

                holder.itemView.findViewById<PreferenceView>(R.id.block).title = context.getString(when (item.blocked) {
                    true -> R.string.info_unblock
                    false -> R.string.info_block
                })

                // partisan
                holder.itemView.findViewById<PreferenceView>(R.id.encryptionKey).summary = if (item.encryptionKeyExist) "***" else ""

                val labels = context.resources.getStringArray(R.array.delete_message_after_labels)

                holder.itemView.findViewById<PreferenceView>(R.id.conversationDeleteEncryptedAfter).visibility =
                    if (item.encryptionKeyExist) View.VISIBLE else View.GONE
                holder.itemView.findViewById<PreferenceView>(R.id.conversationDeleteEncryptedAfter).summary = labels[item.deleteEncryptedAfter]
                deleteEncryptedAfterDialog.adapter.selectedItem = item.deleteEncryptedAfter

                holder.itemView.findViewById<PreferenceView>(R.id.conversationDeleteReceivedAfter).summary = labels[item.deleteReceivedAfter]
                deleteReceivedAfterDialog.adapter.selectedItem = item.deleteReceivedAfter

                holder.itemView.findViewById<PreferenceView>(R.id.conversationDeleteSentAfter).summary = labels[item.deleteSentAfter]
                deleteSentAfterDialog.adapter.selectedItem = item.deleteSentAfter
            }

        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is ConversationInfoRecipient -> 0
            is ConversationInfoSettings -> 1
        }
    }

    override fun areItemsTheSame(old: ConversationInfoItem, new: ConversationInfoItem): Boolean {
        return when {
            old is ConversationInfoRecipient && new is ConversationInfoRecipient -> {
               old.value.id == new.value.id
            }

            old is ConversationInfoSettings && new is ConversationInfoSettings -> {
                true
            }

            else -> false
        }
    }

}
