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
package com.moez.QKSMS.feature.compose

import android.Manifest
import android.animation.LayoutTransition
import android.app.Activity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.moez.QKSMS.R
import com.moez.QKSMS.common.Navigator
import com.moez.QKSMS.common.base.QkThemedActivity
import com.moez.QKSMS.common.util.DateFormatter
import com.moez.QKSMS.common.util.extensions.autoScrollToStart
import com.moez.QKSMS.common.util.extensions.hideKeyboard
import com.moez.QKSMS.common.util.extensions.resolveThemeColor
import com.moez.QKSMS.common.util.extensions.scrapViews
import com.moez.QKSMS.common.util.extensions.setBackgroundTint
import com.moez.QKSMS.common.util.extensions.setTint
import com.moez.QKSMS.common.util.extensions.setVisible
import com.moez.QKSMS.common.util.extensions.showKeyboard
import com.moez.QKSMS.common.widget.QkEditText
import com.moez.QKSMS.common.widget.QkSwitch
import com.moez.QKSMS.common.widget.QkTextView
import com.moez.QKSMS.feature.compose.editing.ChipsAdapter
import com.moez.QKSMS.feature.contacts.ContactsActivity
import com.moez.QKSMS.feature.keysettings.KeySettingsActivity
import com.moez.QKSMS.model.Conversation
import com.moez.QKSMS.model.Message
import com.moez.QKSMS.model.Recipient
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class ComposeActivity : QkThemedActivity(), ComposeView {

    @Inject
    lateinit var chipsAdapter: ChipsAdapter

    @Inject
    lateinit var dateFormatter: DateFormatter

    @Inject
    lateinit var messageAdapter: MessagesAdapter

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val selectContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            chipsSelectedIntent.onNext(
                result.data?.getSerializableExtra(ContactsActivity.ChipsKey)
                    ?.let { serializable -> serializable as? HashMap<String, String?> }
                    ?: hashMapOf())
        }

    private val encryptionKeyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                encryptionKeySetIntent.onNext(Unit)
            }
        }

    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val defaultSmsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private val contentView: ConstraintLayout by lazy { findViewById(R.id.contentView) }
    private val searchInput: QkEditText by lazy { findViewById(R.id.searchInput) }
    private val messageList: RecyclerView by lazy { findViewById(R.id.messageList) }
    private val messagesEmpty: QkTextView by lazy { findViewById(R.id.messagesEmpty) }
    private val loading: ProgressBar by lazy { findViewById(R.id.loading) }
    private val sendAsGroup: Group by lazy { findViewById(R.id.sendAsGroup) }
    private val sendAsGroupBackground: View by lazy { findViewById(R.id.sendAsGroupBackground) }
    private val sendAsGroupSwitch: QkSwitch by lazy { findViewById(R.id.sendAsGroupSwitch) }
    private val chips: RecyclerView by lazy { findViewById(R.id.chips) }
    private val toolbar: androidx.appcompat.widget.Toolbar by lazy { findViewById(R.id.toolbar) }
    private val toolbarSubtitle: QkTextView by lazy { findViewById(R.id.toolbarSubtitle) }
    private val toolbarTitle: QkTextView by lazy { findViewById(R.id.toolbarTitle) }
    private val composeBar: Group by lazy { findViewById(R.id.composeBar) }
    private val messageBackground: View by lazy { findViewById(R.id.messageBackground) }
    private val message: QkEditText by lazy { findViewById(R.id.message) }
    private val counter: QkTextView by lazy { findViewById(R.id.counter) }
    private val sim: ImageView by lazy { findViewById(R.id.sim) }
    private val simIndex: QkTextView by lazy { findViewById(R.id.simIndex) }
    private val send: ImageView by lazy { findViewById(R.id.send) }

    override val activityVisibleIntent: Subject<Boolean> = PublishSubject.create()
    override val chipsSelectedIntent: Subject<HashMap<String, String?>> = PublishSubject.create()
    override val chipDeletedIntent: Subject<Recipient> by lazy { chipsAdapter.chipDeleted }
    override val menuReadyIntent: Observable<Unit> = menu.map { Unit }
    override val optionsItemIntent: Subject<Int> = PublishSubject.create()
    override val sendAsGroupIntent by lazy { sendAsGroupBackground.clicks() }
    override val messageClickIntent: Subject<Long> by lazy { messageAdapter.clicks }
    override val messagesSelectedIntent by lazy { messageAdapter.selectionChanges }
    override val cancelSendingIntent: Subject<Long> by lazy { messageAdapter.cancelSending }
    override val textChangedIntent by lazy { message.textChanges() }
    override val changeSimIntent by lazy { sim.clicks() }
    override val sendIntent by lazy { send.clicks() }
    override val viewQksmsPlusIntent: Subject<Unit> = PublishSubject.create()
    override val backPressedIntent: Subject<Unit> = PublishSubject.create()
    override val encryptionKeySetIntent: Subject<Unit> = PublishSubject.create()
    override val disableEncryptionConfirmed: Subject<Unit> = PublishSubject.create()
    override val searchQueryChangedIntent by lazy { searchInput.textChanges() }

    private val viewModel by lazy { ViewModelProvider(this, viewModelFactory)[ComposeViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_activity)
        showBackButton(true)
        viewModel.bindView(this)

        contentView.layoutTransition = LayoutTransition().apply {
            disableTransitionType(LayoutTransition.CHANGING)
        }

        chipsAdapter.view = chips

        chips.itemAnimator = null
        chips.layoutManager = FlexboxLayoutManager(this)

        messageAdapter.autoScrollToStart(messageList)
        messageAdapter.emptyView = messagesEmpty

        messageList.setHasFixedSize(true)
        messageList.adapter = messageAdapter

        theme
            .doOnNext { loading.setTint(it.theme) }
            .doOnNext { messageAdapter.theme = it }
            .autoDisposable(scope())
            .subscribe()

        window.callback = ComposeWindowCallback(window.callback, this)

        onBackPressedDispatcher.addCallback(this) {
            backPressedIntent.onNext(Unit)
        }

        // These theme attributes don't apply themselves on API 21
        if (Build.VERSION.SDK_INT <= 22) {
            messageBackground.setBackgroundTint(resolveThemeColor(R.attr.bubbleColor))
        }
    }

    override fun onStart() {
        super.onStart()
        activityVisibleIntent.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        activityVisibleIntent.onNext(false)
    }

    override fun render(state: ComposeState) {
        if (state.hasError) {
            finish()
            return
        }

        threadId.onNext(state.threadId)

        title = when {
            state.selectedMessages > 0 -> getString(R.string.compose_title_selected, state.selectedMessages)
            state.query.isNotEmpty() -> state.query
            else -> state.conversationtitle
        }

        toolbarSubtitle.setVisible(state.query.isNotEmpty())
        toolbarSubtitle.text = getString(
            R.string.compose_subtitle_results, state.searchSelectionPosition,
            state.searchResults
        )

        searchInput.setVisible(state.searching)
        toolbarTitle.setVisible(!state.editingMode && !state.searching)
        chips.setVisible(state.editingMode)
        composeBar.setVisible(!state.loading)

        // Don't set the adapters unless needed
        if (state.editingMode && chips.adapter == null) chips.adapter = chipsAdapter

        toolbar.menu.findItem(R.id.search)?.isVisible = !state.editingMode && state.selectedMessages == 0
                && !state.searching && state.query.isEmpty()
        toolbar.menu.findItem(R.id.add)?.isVisible = state.editingMode
        toolbar.menu.findItem(R.id.call)?.isVisible = !state.editingMode && state.selectedMessages == 0
                && state.query.isEmpty() && !state.searching
        toolbar.menu.findItem(R.id.info)?.isVisible = !state.editingMode && state.selectedMessages == 0
                && state.query.isEmpty() && !state.searching
        toolbar.menu.findItem(R.id.copy)?.isVisible = !state.editingMode && state.selectedMessages > 0
        toolbar.menu.findItem(R.id.details)?.isVisible = !state.editingMode && state.selectedMessages == 1
        toolbar.menu.findItem(R.id.delete)?.isVisible = !state.editingMode && state.selectedMessages > 0
        toolbar.menu.findItem(R.id.forward)?.isVisible = !state.editingMode && state.selectedMessages == 1
        toolbar.menu.findItem(R.id.previous)?.isVisible =
            state.selectedMessages == 0 && (state.query.isNotEmpty() || state.searching)
        toolbar.menu.findItem(R.id.next)?.isVisible =
            state.selectedMessages == 0 && (state.query.isNotEmpty() || state.searching)
        toolbar.menu.findItem(R.id.clear)?.isVisible =
            state.selectedMessages == 0 && (state.query.isNotEmpty() || state.searching)
        toolbar.menu.findItem(R.id.encrypted)?.isVisible = state.encryptionEnabled
        toolbar.menu.findItem(R.id.raw)?.isVisible = !state.encryptionEnabled

        chipsAdapter.data = state.selectedChips

        loading.setVisible(state.loading)

        sendAsGroup.setVisible(state.editingMode && state.selectedChips.size >= 2)
        sendAsGroupSwitch.isChecked = state.sendAsGroup

        messageList.setVisible(!state.editingMode || state.sendAsGroup || state.selectedChips.size == 1)
        messageAdapter.encryptionKey.onNext(state.encryptionKey ?: "")
        messageAdapter.conversationData = state.messages
        messageAdapter.highlight = state.searchSelectionId

        counter.text = state.remaining
        counter.setVisible(counter.text.isNotBlank())

        sim.setVisible(state.subscription != null)
        sim.contentDescription = getString(R.string.compose_sim_cd, state.subscription?.displayName)
        simIndex.text = state.subscription?.simSlotIndex?.plus(1)?.toString()

        send.isEnabled = state.canSend
        send.imageAlpha = if (state.canSend) 255 else 128

        if (state.encryptionEnabled) {
            message.imeOptions = message.imeOptions or IME_FLAG_NO_PERSONALIZED_LEARNING
        } else {
            message.imeOptions = message.imeOptions and IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
        }
    }

    override fun clearSelection() = messageAdapter.clearSelection()

    override fun showDetails(details: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.compose_details_title)
            .setMessage(details)
            .setCancelable(true)
            .show()
    }

    override fun requestDefaultSms() {
        navigator.showDefaultSmsDialog(this, defaultSmsLauncher)
    }

    override fun requestSmsPermission() {
        smsPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS
            )
        )
    }

    override fun showContacts(sharing: Boolean, chips: List<Recipient>) {
        message.hideKeyboard()
        val serialized = HashMap(chips.associate { chip -> chip.address to chip.contact?.lookupKey })
        val intent = Intent(this, ContactsActivity::class.java)
            .putExtra(ContactsActivity.SharingKey, sharing)
            .putExtra(ContactsActivity.ChipsKey, serialized)
        selectContactLauncher.launch(intent)
    }

    override fun themeChanged() {
        messageList.scrapViews()
    }

    override fun showKeyboard() {
        message.postDelayed({
            message.showKeyboard()
        }, 200)
    }

    override fun setDraft(draft: String) {
        message.setText(draft)
        message.setSelection(draft.length)
    }

    override fun scrollToMessage(id: Long) {
        messageAdapter.conversationData?.second
            ?.indexOfLast { message -> message.id == id }
            ?.takeIf { position -> position != -1 }
            ?.let(messageList::scrollToPosition)
    }

    override fun showQksmsPlusSnackbar(message: Int) {
        Snackbar.make(contentView, message, Snackbar.LENGTH_LONG).run {
            setAction(R.string.button_more) { viewQksmsPlusIntent.onNext(Unit) }
            setActionTextColor(colors.theme().theme)
            show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.compose, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        optionsItemIntent.onNext(item.itemId)
        return true
    }

    override fun getColoredMenuItems(): List<Int> {
        return super.getColoredMenuItems() + R.id.call
    }

    override fun showSearch() {
        searchInput.requestFocus()
        searchInput.postDelayed({ searchInput.showKeyboard() }, 200)
    }

    override fun clearSearch() {
        searchInput.setText("")
        searchInput.hideKeyboard()
    }

    override fun showDisableEncryptionDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.disable_encryption_confirmation)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.button_disable) { _, _ ->
                disableEncryptionConfirmed.onNext(Unit)
            }
            .show()
    }

    override fun showEncryptionKeySettings(conversation: Conversation) {
        (threadId as BehaviorSubject?)?.value?.let { threadId ->
            if (threadId != 0L) {
                val intent = Intent(this, KeySettingsActivity::class.java)
                    .putExtra("threadId", threadId)
                encryptionKeyLauncher.launch(intent)
            }
        }
    }
}
