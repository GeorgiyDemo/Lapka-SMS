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
package org.lapka.sms.common.base

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import org.lapka.sms.R
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.iterator
import androidx.core.view.updatePadding
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.color.DynamicColors
import androidx.lifecycle.Lifecycle
import org.lapka.sms.common.util.Colors
import org.lapka.sms.common.util.extensions.resolveThemeColor
import org.lapka.sms.extensions.Optional
import org.lapka.sms.extensions.asObservable
import org.lapka.sms.extensions.mapNotNull
import org.lapka.sms.repository.ConversationRepository
import org.lapka.sms.repository.MessageRepository
import org.lapka.sms.util.PhoneNumberUtils
import org.lapka.sms.util.Preferences
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Base activity that automatically applies any necessary theme theme settings and colors
 *
 * In most cases, this should be used instead of the base QkActivity, except for when
 * an activity does not depend on the theme
 */
abstract class QkThemedActivity : QkActivity() {

    @Inject
    lateinit var colors: Colors

    @Inject
    lateinit var conversationRepo: ConversationRepository

    @Inject
    lateinit var messageRepo: MessageRepository

    @Inject
    lateinit var phoneNumberUtils: PhoneNumberUtils

    @Inject
    lateinit var prefs: Preferences

    /**
     * In case the activity should be themed for a specific conversation, the selected conversation
     * can be changed by pushing the threadId to this subject
     */
    val threadId: Subject<Long> = BehaviorSubject.createDefault(0)

    /**
     * Switch the theme if the threadId changes
     * Set it based on the latest message in the conversation
     */
    val theme: Observable<Colors.Theme> = threadId
        .distinctUntilChanged()
        .switchMap { threadId ->
            val conversation = conversationRepo.getConversation(threadId)
            when {
                conversation == null -> Observable.just(Optional(null))

                conversation.recipients.size == 1 -> Observable.just(Optional(conversation.recipients.first()))

                else -> messageRepo.getLastIncomingMessage(conversation.id)
                    .asObservable()
                    .mapNotNull { messages -> messages.firstOrNull() }
                    .distinctUntilChanged { message -> message.address }
                    .mapNotNull { message ->
                        conversation.recipients.find { recipient ->
                            phoneNumberUtils.compare(recipient.address, message.address)
                        }
                    }
                    .map { recipient -> Optional(recipient) }
                    .startWith(Optional(conversation.recipients.firstOrNull()))
                    .distinctUntilChanged()
            }
        }
        .switchMap { colors.themeObservable(it.value) }

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getActivityThemeRes(prefs.black.get()))
        DynamicColors.applyIfAvailable(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Activity transitions
        window.enterTransition = Slide(Gravity.END).apply { duration = 250 }
        window.exitTransition = Slide(Gravity.START).apply { duration = 200 }
        window.returnTransition = Slide(Gravity.END).apply { duration = 200 }
        window.reenterTransition = Slide(Gravity.START).apply { duration = 250 }

        // When certain preferences change, we need to recreate the activity
        val triggers =
            listOf(prefs.nightMode, prefs.night, prefs.black, prefs.textSize, prefs.systemFont, prefs.theme())
        Observable.merge(triggers.map { it.asObservable().skip(1) })
            .debounce(400, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDisposable(scope())
            .subscribe { recreate() }

        // Set the color for the recent apps title
        val toolbarColor = resolveThemeColor(android.R.attr.windowBackground)
        val icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        val taskDesc = ActivityManager.TaskDescription(getString(R.string.app_name), icon, toolbarColor)
        setTaskDescription(taskDesc)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        // Apply window insets for edge-to-edge
        val contentRoot = findViewById<View>(android.R.id.content)?.let {
            (it as? android.view.ViewGroup)?.getChildAt(0)
        }

        // Find the actual content view to apply bottom padding to.
        // DrawerLayout doesn't support padding for child positioning,
        // so we need to find its first child (the main content container).
        val bottomTarget = if (contentRoot is DrawerLayout) {
            (contentRoot as android.view.ViewGroup).getChildAt(0)
        } else {
            contentRoot
        }

        contentRoot?.let { root ->
            // CoordinatorLayout with AppBarLayout (fitsSystemWindows) handles its own
            // insets dispatch — don't intercept, just ensure fitsSystemWindows is set
            if (root is CoordinatorLayout) {
                root.fitsSystemWindows = true
                return
            }

            ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
                val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
                val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

                // Apply status bar padding to toolbar
                findViewById<View>(R.id.toolbar)?.updatePadding(top = statusBars.top)

                // Apply nav bar or IME padding to content
                val bottomInset = maxOf(navBars.bottom, imeInsets.bottom)
                bottomTarget?.updatePadding(bottom = bottomInset)

                // Apply insets to drawer panel if present
                if (root is DrawerLayout) {
                    for (i in 0 until root.childCount) {
                        val child = root.getChildAt(i)
                        val lp = child.layoutParams as? DrawerLayout.LayoutParams
                        if (lp != null && lp.gravity != android.view.Gravity.NO_GRAVITY && lp.gravity != 0) {
                            child.updatePadding(top = statusBars.top, bottom = navBars.bottom)
                        }
                    }
                }

                windowInsets
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (prefs.showInTaskSwitcher.get()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Set the color for the overflow and navigation icon
        val textSecondary = resolveThemeColor(android.R.attr.textColorSecondary)
        findViewById<Toolbar>(R.id.toolbar)?.overflowIcon =
            findViewById<Toolbar>(R.id.toolbar)?.overflowIcon?.apply { setTint(textSecondary) }

        // Update the colours of the menu items
        Observables.combineLatest(menu, theme) { menu, theme ->
            menu.iterator().forEach { menuItem ->
                val tint = when (menuItem.itemId) {
                    in getColoredMenuItems() -> theme.theme
                    else -> textSecondary
                }

                menuItem.icon = menuItem.icon?.apply { setTint(tint) }
            }
        }.autoDisposable(scope(Lifecycle.Event.ON_DESTROY)).subscribe()
    }

    open fun getColoredMenuItems(): List<Int> {
        return listOf()
    }

    /**
     * This can be overridden in case an activity does not want to use the default themes
     */
    open fun getActivityThemeRes(black: Boolean) = when {
        black -> R.style.AppTheme_Black
        else -> R.style.AppTheme
    }

}