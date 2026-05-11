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
package org.lapka.sms.feature.main

import android.Manifest
import android.animation.ObjectAnimator
import androidx.activity.addCallback
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import org.lapka.sms.R
import org.lapka.sms.BuildConfig
import org.lapka.sms.common.Navigator
import org.lapka.sms.common.androidxcompat.drawerOpen
import org.lapka.sms.common.base.QkThemedActivity
import org.lapka.sms.common.util.extensions.autoScrollToStart
import org.lapka.sms.common.util.extensions.dismissKeyboard
import org.lapka.sms.common.util.extensions.resolveThemeColor
import org.lapka.sms.common.util.extensions.scrapViews
import org.lapka.sms.common.util.extensions.setBackgroundTint
import org.lapka.sms.common.util.extensions.setTint
import org.lapka.sms.common.util.extensions.setVisible
import org.lapka.sms.common.widget.QkEditText
import org.lapka.sms.common.widget.QkTextView
import org.lapka.sms.feature.blocking.BlockingDialog
import org.lapka.sms.feature.changelog.ChangelogDialog
import org.lapka.sms.feature.conversations.ConversationItemTouchCallback
import org.lapka.sms.feature.conversations.ConversationsAdapter
import org.lapka.sms.manager.ChangelogManager
import org.lapka.sms.repository.SyncRepository
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class MainActivity : QkThemedActivity(), MainView {

    @Inject
    lateinit var blockingDialog: BlockingDialog

    @Inject
    lateinit var disposables: CompositeDisposable

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var conversationsAdapter: ConversationsAdapter

    @Inject
    lateinit var drawerBadgesExperiment: DrawerBadgesExperiment

    @Inject
    lateinit var searchAdapter: SearchAdapter

    @Inject
    lateinit var itemTouchCallback: ConversationItemTouchCallback

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val drawerLayout: DrawerLayout by lazy { findViewById(R.id.drawerLayout) }
    private val toolbar: androidx.appcompat.widget.Toolbar by lazy { findViewById(R.id.toolbar) }
    private val toolbarSearch: QkEditText by lazy { findViewById(R.id.toolbarSearch) }
    private val toolbarTitle: QkTextView by lazy { findViewById(R.id.toolbarTitle) }
    private val swipeRefresh: SwipeRefreshLayout by lazy { findViewById(R.id.swipeRefresh) }
    private val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }
    private val compose: FloatingActionButton by lazy { findViewById(R.id.compose) }
    private val emptyContainer: LinearLayout by lazy { findViewById(R.id.emptyContainer) }
    private val emptyIcon: ImageView by lazy { findViewById(R.id.emptyIcon) }
    private val empty: QkTextView by lazy { findViewById(R.id.empty) }
    private val drawer: View by lazy { findViewById(R.id.drawer) }
    private val inbox: LinearLayout by lazy { findViewById(R.id.inbox) }
    private val inboxIcon: ImageView by lazy { findViewById(R.id.inboxIcon) }
    private val archived: LinearLayout by lazy { findViewById(R.id.archived) }
    private val archivedIcon: ImageView by lazy { findViewById(R.id.archivedIcon) }
    private val blocking: LinearLayout by lazy { findViewById(R.id.blocking) }
    private val settings: LinearLayout by lazy { findViewById(R.id.settings) }
    private val help: LinearLayout by lazy { findViewById(R.id.help) }
    private val invite: LinearLayout by lazy { findViewById(R.id.invite) }
    private val rateLayout: ConstraintLayout by lazy { findViewById(R.id.rateLayout) }
    private val rateIcon: ImageView by lazy { findViewById(R.id.rateIcon) }
    private val rateDismiss: QkTextView by lazy { findViewById(R.id.rateDismiss) }
    private val rateOkay: QkTextView by lazy { findViewById(R.id.rateOkay) }

    override val onNewIntentIntent: Subject<Intent> = PublishSubject.create()
    override val activityResumedIntent: Subject<Boolean> = PublishSubject.create()
    override val queryChangedIntent by lazy { toolbarSearch.textChanges() }
    override val composeIntent by lazy { compose.clicks() }
    override val drawerOpenIntent: Observable<Boolean> by lazy {
        drawerLayout
            .drawerOpen(Gravity.START)
            .doOnNext { dismissKeyboard() }
    }
    override val homeIntent: Subject<Unit> = PublishSubject.create()
    override val navigationIntent: Observable<NavItem> by lazy {
        Observable.merge(
            listOf(
                backPressedSubject,
                inbox.clicks().map { NavItem.INBOX },
                archived.clicks().map { NavItem.ARCHIVED },
                blocking.clicks().map { NavItem.BLOCKING },
                settings.clicks().map { NavItem.SETTINGS },
                help.clicks().map { NavItem.HELP },
                invite.clicks().map { NavItem.INVITE })
        )
    }
    override val optionsItemIntent: Subject<Int> = PublishSubject.create()
    override val dismissRatingIntent by lazy { rateDismiss.clicks() }
    override val rateIntent by lazy { rateOkay.clicks() }
    override val conversationsSelectedIntent by lazy { conversationsAdapter.selectionChanges }
    override val confirmDeleteIntent: Subject<List<Long>> = PublishSubject.create()
    override val swipeConversationIntent by lazy { itemTouchCallback.swipes }
    override val changelogMoreIntent by lazy { changelogDialog.moreClicks }
    override val undoArchiveIntent: Subject<Unit> = PublishSubject.create()
    override val snackbarButtonIntent: Subject<Unit> = PublishSubject.create()

    private val viewModel by lazy { ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java] }
    private val toggle by lazy { ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.main_drawer_open_cd, 0) }
    private val itemTouchHelper by lazy { ItemTouchHelper(itemTouchCallback) }
    private val syncingProgress: ProgressBar? get() = findViewById(R.id.syncingProgress)
    private val progressAnimator by lazy { ObjectAnimator.ofInt(syncingProgress, "progress", 0, 0) }
    private val changelogDialog by lazy { ChangelogDialog(this) }
    private val snackbar by lazy { findViewById<View>(R.id.snackbar) }
    private val syncing by lazy { findViewById<View>(R.id.syncing) }
    private val snackbarTitle: TextView? get() = findViewById(R.id.snackbarTitle)
    private val snackbarMessage: TextView? get() = findViewById(R.id.snackbarMessage)
    private val snackbarButton: TextView? get() = findViewById(R.id.snackbarButton)
    private val backPressedSubject: Subject<NavItem> = PublishSubject.create()

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    private val defaultSmsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        viewModel.bindView(this)
        onNewIntentIntent.onNext(intent)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        (snackbar as? ViewStub)?.setOnInflateListener { _, _ ->
            snackbarButton?.clicks()
                ?.autoDisposable(scope(Lifecycle.Event.ON_DESTROY))
                ?.subscribe(snackbarButtonIntent)
        }

        (syncing as? ViewStub)?.setOnInflateListener { _, _ ->
            val themeColor = colors.theme().theme
            syncingProgress?.progressTintList = ColorStateList.valueOf(themeColor)
            syncingProgress?.indeterminateTintList = ColorStateList.valueOf(themeColor)
        }

        // Pull-to-refresh setup
        swipeRefresh.setOnRefreshListener {
            // Dismiss after a short delay since conversations auto-update via Realm
            swipeRefresh.postDelayed({ swipeRefresh.isRefreshing = false }, 1000)
            contentResolver.notifyChange(android.net.Uri.parse("content://sms"), null)
        }

        // Set drawer version text
        findViewById<QkTextView>(R.id.drawerVersion)?.text = "v${BuildConfig.VERSION_NAME}"

        toggle.syncState()
        toolbar.setNavigationOnClickListener {
            dismissKeyboard()
            homeIntent.onNext(Unit)
        }

        itemTouchCallback.adapter = conversationsAdapter
        conversationsAdapter.autoScrollToStart(recyclerView)

        // Hide FAB on scroll down, show on scroll up
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && compose.isShown) compose.hide()
                else if (dy < 0 && !compose.isShown) compose.show()
            }
        })

        // Don't allow clicks to pass through the drawer layout
        drawer.clicks().autoDisposable(scope()).subscribe()

        // Set the theme color tint to the recyclerView, progressbar, and FAB
        theme
            .autoDisposable(scope())
            .subscribe { theme ->
                // Set the color for the drawer icons
                val states = arrayOf(
                    intArrayOf(android.R.attr.state_activated),
                    intArrayOf(-android.R.attr.state_activated)
                )

                resolveThemeColor(android.R.attr.textColorSecondary)
                    .let { textSecondary -> ColorStateList(states, intArrayOf(theme.theme, textSecondary)) }
                    .let { tintList ->
                        inboxIcon.imageTintList = tintList
                        archivedIcon.imageTintList = tintList
                    }

                // Miscellaneous views
                syncingProgress?.progressTintList = ColorStateList.valueOf(theme.theme)
                syncingProgress?.indeterminateTintList = ColorStateList.valueOf(theme.theme)
                rateIcon.setTint(theme.theme)
                swipeRefresh.setColorSchemeColors(theme.theme)
                compose.backgroundTintList = ColorStateList.valueOf(theme.theme)

                // Set the FAB compose icon color
                compose.imageTintList = ColorStateList.valueOf(theme.textPrimary)

                // Tint empty state icon
                emptyIcon.setColorFilter(theme.theme)
            }

        // These theme attributes don't apply themselves on API 21
        if (Build.VERSION.SDK_INT <= 22) {
            toolbarSearch.setBackgroundTint(resolveThemeColor(R.attr.bubbleColor))
        }
        onBackPressedDispatcher.addCallback(this) {
            backPressedSubject.onNext(NavItem.BACK)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.run(onNewIntentIntent::onNext)
    }

    override fun render(state: MainState) {
        if (state.hasError) {
            finish()
            return
        }

        val addContact = when (state.page) {
            is Inbox -> state.page.addContact
            is Archived -> state.page.addContact
            else -> false
        }

        val markPinned = when (state.page) {
            is Inbox -> state.page.markPinned
            is Archived -> state.page.markPinned
            else -> true
        }

        val markRead = when (state.page) {
            is Inbox -> state.page.markRead
            is Archived -> state.page.markRead
            else -> true
        }

        val selectedConversations = when (state.page) {
            is Inbox -> state.page.selected
            is Archived -> state.page.selected
            else -> 0
        }

        toolbarSearch.setVisible(state.page is Inbox && state.page.selected == 0 || state.page is Searching)
        toolbarTitle.setVisible(toolbarSearch.visibility != View.VISIBLE)

        toolbar.menu.findItem(R.id.archive)?.isVisible = state.page is Inbox && selectedConversations != 0
        toolbar.menu.findItem(R.id.unarchive)?.isVisible = state.page is Archived && selectedConversations != 0
        toolbar.menu.findItem(R.id.delete)?.isVisible = selectedConversations != 0
        toolbar.menu.findItem(R.id.add)?.isVisible = addContact && selectedConversations != 0
        toolbar.menu.findItem(R.id.pin)?.isVisible = markPinned && selectedConversations != 0
        toolbar.menu.findItem(R.id.unpin)?.isVisible = !markPinned && selectedConversations != 0
        toolbar.menu.findItem(R.id.read)?.isVisible = markRead && selectedConversations != 0
        toolbar.menu.findItem(R.id.unread)?.isVisible = !markRead && selectedConversations != 0
        toolbar.menu.findItem(R.id.block)?.isVisible = selectedConversations != 0

        rateLayout.setVisible(false)

        compose.setVisible(state.page is Inbox || state.page is Archived)
        conversationsAdapter.emptyView = emptyContainer.takeIf { state.page is Inbox || state.page is Archived }
        searchAdapter.emptyView = emptyContainer.takeIf { state.page is Searching }

        when (state.page) {
            is Inbox -> {
                showBackButton(state.page.selected > 0)
                title = getString(R.string.main_title_selected, state.page.selected)
                if (recyclerView.adapter !== conversationsAdapter) recyclerView.adapter = conversationsAdapter
                conversationsAdapter.updateData(state.page.data)
                itemTouchHelper.attachToRecyclerView(recyclerView)
                empty.setText(R.string.inbox_empty_text)
            }

            is Searching -> {
                showBackButton(true)
                if (recyclerView.adapter !== searchAdapter) recyclerView.adapter = searchAdapter
                searchAdapter.data = state.page.data ?: listOf()
                itemTouchHelper.attachToRecyclerView(null)
                empty.setText(R.string.inbox_search_empty_text)
            }

            is Archived -> {
                showBackButton(state.page.selected > 0)
                title = when (state.page.selected != 0) {
                    true -> getString(R.string.main_title_selected, state.page.selected)
                    false -> getString(R.string.title_archived)
                }
                if (recyclerView.adapter !== conversationsAdapter) recyclerView.adapter = conversationsAdapter
                conversationsAdapter.updateData(state.page.data)
                itemTouchHelper.attachToRecyclerView(null)
                empty.setText(R.string.archived_empty_text)
            }
        }

        inbox.isActivated = state.page is Inbox
        archived.isActivated = state.page is Archived

        if (drawerLayout.isDrawerOpen(GravityCompat.START) && !state.drawerOpen) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else if (!drawerLayout.isDrawerVisible(GravityCompat.START) && state.drawerOpen) {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        when (state.syncing) {
            is SyncRepository.SyncProgress.Idle -> {
                syncing.isVisible = false
                snackbar.isVisible = !state.defaultSms || !state.smsPermission || !state.contactPermission
            }

            is SyncRepository.SyncProgress.Running -> {
                syncing.isVisible = true
                syncingProgress?.max = state.syncing.max
                progressAnimator.apply { setIntValues(syncingProgress?.progress ?: 0, state.syncing.progress) }.start()
                syncingProgress?.isIndeterminate = state.syncing.indeterminate
                snackbar.isVisible = false
            }
        }

        when {
            !state.defaultSms -> {
                snackbarTitle?.setText(R.string.main_default_sms_title)
                snackbarMessage?.setText(R.string.main_default_sms_message)
                snackbarButton?.setText(R.string.main_default_sms_change)
            }

            !state.smsPermission -> {
                snackbarTitle?.setText(R.string.main_permission_required)
                snackbarMessage?.setText(R.string.main_permission_sms)
                snackbarButton?.setText(R.string.main_permission_allow)
            }

            !state.contactPermission -> {
                snackbarTitle?.setText(R.string.main_permission_required)
                snackbarMessage?.setText(R.string.main_permission_contacts)
                snackbarButton?.setText(R.string.main_permission_allow)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activityResumedIntent.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        activityResumedIntent.onNext(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun showBackButton(show: Boolean) {
        toggle.onDrawerSlide(drawer, if (show) 1f else 0f)
        toggle.drawerArrowDrawable.color = when (show) {
            true -> resolveThemeColor(android.R.attr.textColorSecondary)
            false -> resolveThemeColor(android.R.attr.textColorPrimary)
        }
    }


    override fun requestDefaultSms() {
        navigator.showDefaultSmsDialog(this, defaultSmsLauncher)
    }

    override fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    override fun clearSearch() {
        dismissKeyboard()
        toolbarSearch.text = null
    }

    override fun clearSelection() {
        conversationsAdapter.clearSelection()
    }

    override fun themeChanged() {
        recyclerView.scrapViews()
    }

    override fun showBlockingDialog(conversations: List<Long>, block: Boolean) {
        blockingDialog.show(this, conversations, block)
    }

    override fun showDeleteDialog(conversations: List<Long>) {
        val count = conversations.size
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(resources.getQuantityString(R.plurals.dialog_delete_message, count, count))
            .setPositiveButton(R.string.button_delete) { _, _ -> confirmDeleteIntent.onNext(conversations) }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun showChangelog(changelog: ChangelogManager.CumulativeChangelog) {
        changelogDialog.show(changelog)
    }

    override fun showArchivedSnackbar() {
        Snackbar.make(drawerLayout, R.string.toast_archived, Snackbar.LENGTH_LONG).apply {
            setAction(R.string.button_undo) { undoArchiveIntent.onNext(Unit) }
            setActionTextColor(colors.theme().theme)
            show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        optionsItemIntent.onNext(item.itemId)
        return true
    }

}
