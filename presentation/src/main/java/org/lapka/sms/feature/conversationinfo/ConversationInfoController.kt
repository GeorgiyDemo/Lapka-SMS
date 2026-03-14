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
package org.lapka.sms.feature.conversationinfo

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.RouterTransaction
import org.lapka.sms.R
import org.lapka.sms.common.Navigator
import org.lapka.sms.common.QkChangeHandler
import org.lapka.sms.common.base.QkController
import org.lapka.sms.common.util.extensions.scrapViews
import org.lapka.sms.common.widget.TextInputDialog
import org.lapka.sms.feature.blocking.BlockingDialog
import org.lapka.sms.feature.conversationinfo.injection.ConversationInfoModule
import org.lapka.sms.feature.themepicker.ThemePickerController
import org.lapka.sms.injection.appComponent
import org.lapka.sms.model.Conversation
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class ConversationInfoController(
    val threadId: Long = 0
) : QkController<ConversationInfoView, ConversationInfoState, ConversationInfoPresenter>(), ConversationInfoView {

    @Inject
    lateinit var context: Context

    @Inject
    override lateinit var presenter: ConversationInfoPresenter

    @Inject
    lateinit var blockingDialog: BlockingDialog

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var adapter: ConversationInfoAdapter

    private val recyclerView: RecyclerView get() = containerView!!.findViewById(R.id.recyclerView)

    private var nameDialog: TextInputDialog? = null
    private fun getNameDialog(): TextInputDialog? {
        return nameDialog ?: activity?.let {
            TextInputDialog(it, it.getString(R.string.info_name), nameChangeSubject::onNext)
                .also { d -> nameDialog = d }
        }
    }

    private val nameChangeSubject: Subject<String> = PublishSubject.create()
    private val confirmDeleteSubject: Subject<Unit> = PublishSubject.create()

    init {
        appComponent
            .conversationInfoBuilder()
            .conversationInfoModule(ConversationInfoModule(this))
            .build()
            .inject(this)

        layoutRes = R.layout.conversation_info_controller
    }

    override fun onViewCreated() {
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(GridSpacingItemDecoration(adapter, activity ?: return))
        recyclerView.layoutManager = GridLayoutManager(activity, 3).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = if (adapter.getItemViewType(position) == 2) 1 else 3
            }
        }

        adapter.deleteEncryptedAfterDialog.adapter.setData(R.array.delete_message_after_labels)
        adapter.deleteSentAfterDialog.adapter.setData(R.array.delete_message_after_labels)
        adapter.deleteReceivedAfterDialog.adapter.setData(R.array.delete_message_after_labels)

        themedActivity?.theme
            ?.autoDisposable(scope())
            ?.subscribe { recyclerView.scrapViews() }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.info_title)
        showBackButton(true)
    }

    override fun render(state: ConversationInfoState) {
        if (state.hasError) {
            activity?.finish()
            return
        }

        adapter.data = state.data
    }

    override fun recipientClicks(): Observable<Long> = adapter.recipientClicks
    override fun recipientLongClicks(): Observable<Long> = adapter.recipientLongClicks
    override fun themeClicks(): Observable<Long> = adapter.themeClicks
    override fun nameClicks(): Observable<*> = adapter.nameClicks
    override fun nameChanges(): Observable<String> = nameChangeSubject
    override fun notificationClicks(): Observable<*> = adapter.notificationClicks
    override fun archiveClicks(): Observable<*> = adapter.archiveClicks
    override fun blockClicks(): Observable<*> = adapter.blockClicks
    override fun deleteClicks(): Observable<*> = adapter.deleteClicks
    override fun confirmDelete(): Observable<*> = confirmDeleteSubject
    override fun encryptionKeyClicks(): Observable<*> = adapter.encryptionKeyClicks
    override fun deleteEncryptedAfterClicks(): Observable<*> = adapter.deleteEncryptedAfterClicks
    override fun deleteReceivedAfterClicks(): Observable<*> = adapter.deleteReceivedAfterClicks
    override fun deleteSentAfterClicks(): Observable<*> = adapter.deleteSentAfterClicks
    override fun deleteEncryptedAfterSelected(): Observable<Int> =
        adapter.deleteEncryptedAfterDialog.adapter.menuItemClicks

    override fun deleteReceivedAfterSelected(): Observable<Int> =
        adapter.deleteReceivedAfterDialog.adapter.menuItemClicks

    override fun deleteSentAfterSelected(): Observable<Int> = adapter.deleteSentAfterDialog.adapter.menuItemClicks

    override fun showNameDialog(name: String) {
        getNameDialog()?.setText(name)?.show()
    }

    override fun showThemePicker(recipientId: Long) {
        router.pushController(
            RouterTransaction.with(ThemePickerController(recipientId))
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler())
        )
    }

    override fun showBlockingDialog(conversations: List<Long>, block: Boolean) {
        (activity as? AppCompatActivity)?.let { blockingDialog.show(it, conversations, block) }
    }

    override fun requestDefaultSms() {
        val act = activity ?: return
        val launcher = (act as? ConversationInfoActivity)?.defaultSmsLauncher
        navigator.showDefaultSmsDialog(act, launcher)
    }

    override fun showDeleteDialog() {
        val ctx = activity ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(resources?.getQuantityString(R.plurals.dialog_delete_message, 1))
            .setPositiveButton(R.string.button_delete) { _, _ -> confirmDeleteSubject.onNext(Unit) }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun showEncryptionKeySettings(conversation: Conversation) {
        navigator.showConversationKeySettings(conversation.id)
    }

    override fun showDeleteEncryptedAfterDialog(conversation: Conversation) {
        activity?.let { adapter.deleteEncryptedAfterDialog.show(it) }
    }

    override fun showDeleteReceivedAfterDialog(conversation: Conversation) {
        activity?.let { adapter.deleteReceivedAfterDialog.show(it) }
    }

    override fun showDeleteSentAfterDialog(conversation: Conversation) {
        activity?.let { adapter.deleteSentAfterDialog.show(it) }
    }
}
