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
package org.lapka.sms.feature.settings

import android.animation.ObjectAnimator
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.longClicks
import org.lapka.sms.BuildConfig
import org.lapka.sms.R
import org.lapka.sms.common.MenuItem
import org.lapka.sms.common.Navigator
import org.lapka.sms.common.QkChangeHandler
import org.lapka.sms.common.QkDialog
import org.lapka.sms.common.base.QkController
import org.lapka.sms.common.util.Colors
import org.lapka.sms.common.util.extensions.animateLayoutChanges
import org.lapka.sms.common.util.extensions.setBackgroundTint
import org.lapka.sms.common.util.extensions.setVisible
import org.lapka.sms.common.widget.PreferenceView
import org.lapka.sms.common.widget.QkSwitch
import org.lapka.sms.common.widget.TextInputDialog
import org.lapka.sms.feature.settings.about.AboutController
import org.lapka.sms.feature.settings.autodelete.AutoDeleteDialog
import org.lapka.sms.feature.settings.swipe.SwipeActionsController
import org.lapka.sms.feature.themepicker.ThemePickerController
import org.lapka.sms.injection.appComponent
import org.lapka.sms.repository.SyncRepository
import org.lapka.sms.util.Preferences
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

class SettingsController : QkController<SettingsView, SettingsState, SettingsPresenter>(), SettingsView {

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var colors: Colors

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var nightModeDialog: QkDialog

    @Inject
    lateinit var textSizeDialog: QkDialog

    @Inject
    lateinit var sendDelayDialog: QkDialog

    @Inject
    lateinit var prefs: Preferences


    @Inject
    override lateinit var presenter: SettingsPresenter

    private var signatureDialog: TextInputDialog? = null
    private fun getSignatureDialog(): TextInputDialog? {
        return signatureDialog ?: activity?.let {
            TextInputDialog(it, context.getString(R.string.settings_signature_title), signatureSubject::onNext)
                .also { d -> signatureDialog = d }
        }
    }

    private var autoDeleteDialog: AutoDeleteDialog? = null
    private fun getAutoDeleteDialog(): AutoDeleteDialog? {
        return autoDeleteDialog ?: activity?.let {
            AutoDeleteDialog(it, autoDeleteSubject::onNext)
                .also { d -> autoDeleteDialog = d }
        }
    }

    private var smsForResetDialog: TextInputDialog? = null
    private fun getSmsForResetDialog(): TextInputDialog? {
        return smsForResetDialog ?: activity?.let {
            TextInputDialog(it, context.getString(R.string.sms_for_reset), smsForResetSubject::onNext)
                .also { d -> smsForResetDialog = d }
        }
    }

    private val startTimeSelectedSubject: Subject<Pair<Int, Int>> = PublishSubject.create()
    private val endTimeSelectedSubject: Subject<Pair<Int, Int>> = PublishSubject.create()
    private val signatureSubject: Subject<String> = PublishSubject.create()
    private val autoDeleteSubject: Subject<Int> = PublishSubject.create()

    // partisan
    private val smsForResetSubject: Subject<String> = PublishSubject.create()
    private val languageSubject: Subject<String> = PublishSubject.create()

    // View accessors — use containerView instead of view because view is null during onViewCreated
    private val preferences: LinearLayout get() = containerView!!.findViewById(R.id.preferences)
    private val themePreview: View get() = containerView!!.findViewById(R.id.themePreview)
    private val night: PreferenceView get() = containerView!!.findViewById(R.id.night)
    private val nightStart: PreferenceView get() = containerView!!.findViewById(R.id.nightStart)
    private val nightEnd: PreferenceView get() = containerView!!.findViewById(R.id.nightEnd)
    private val black: PreferenceView get() = containerView!!.findViewById(R.id.black)
    private val autoEmoji: PreferenceView get() = containerView!!.findViewById(R.id.autoEmoji)
    private val delayed: PreferenceView get() = containerView!!.findViewById(R.id.delayed)
    private val delivery: PreferenceView get() = containerView!!.findViewById(R.id.delivery)
    private val signature: PreferenceView get() = containerView!!.findViewById(R.id.signature)
    private val textSize: PreferenceView get() = containerView!!.findViewById(R.id.textSize)
    private val autoColor: PreferenceView get() = containerView!!.findViewById(R.id.autoColor)
    private val systemFont: PreferenceView get() = containerView!!.findViewById(R.id.systemFont)
    private val mobileOnly: PreferenceView get() = containerView!!.findViewById(R.id.mobileOnly)
    private val autoDelete: PreferenceView get() = containerView!!.findViewById(R.id.autoDelete)
    private val syncingProgress: ProgressBar get() = containerView!!.findViewById(R.id.syncingProgress)
    private val about: PreferenceView get() = containerView!!.findViewById(R.id.about)
    private val smsForReset: PreferenceView get() = containerView!!.findViewById(R.id.smsForReset)
    private val showInTaskSwitcher: PreferenceView get() = containerView!!.findViewById(R.id.showInTaskSwitcher)
    private val language: PreferenceView get() = containerView!!.findViewById(R.id.language)

    private val progressAnimator by lazy { ObjectAnimator.ofInt(syncingProgress, "progress", 0, 0) }

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.settings_controller

        colors.themeObservable()
            .autoDisposable(scope())
            .subscribe { activity?.recreate() }
    }

    override fun onViewCreated() {
        preferences.postDelayed({
            containerView?.findViewById<LinearLayout>(R.id.preferences)?.animateLayoutChanges = true
        }, 100)

        when (Build.VERSION.SDK_INT >= 29) {
            true -> nightModeDialog.adapter.setData(R.array.night_modes)
            false -> nightModeDialog.adapter.data = context.resources.getStringArray(R.array.night_modes)
                .mapIndexed { index, title -> MenuItem(title, index) }
                .drop(1)
        }
        textSizeDialog.adapter.setData(R.array.text_sizes)
        sendDelayDialog.adapter.setData(R.array.delayed_sending_labels)

        about.summary = context.getString(
            R.string.settings_version,
            BuildConfig.VERSION_NAME,
            "3.9.4",
            org.lapka.sms.VERSION.toString()
        )
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.title_settings)
        showBackButton(true)
    }

    override fun preferenceClicks(): Observable<PreferenceView> {
        val prefViews = mutableListOf<PreferenceView>()
        fun collect(group: android.view.ViewGroup) {
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                if (child is PreferenceView) prefViews.add(child)
                else if (child is android.view.ViewGroup) collect(child)
            }
        }
        collect(preferences)
        return Observable.merge(prefViews.map { pref -> pref.clicks().map { pref } })
    }

    override fun aboutLongClicks(): Observable<*> = about.longClicks()

    override fun nightModeSelected(): Observable<Int> = nightModeDialog.adapter.menuItemClicks

    override fun nightStartSelected(): Observable<Pair<Int, Int>> = startTimeSelectedSubject

    override fun nightEndSelected(): Observable<Pair<Int, Int>> = endTimeSelectedSubject

    override fun textSizeSelected(): Observable<Int> = textSizeDialog.adapter.menuItemClicks

    override fun sendDelaySelected(): Observable<Int> = sendDelayDialog.adapter.menuItemClicks

    override fun signatureChanged(): Observable<String> = signatureSubject

    override fun autoDeleteChanged(): Observable<Int> = autoDeleteSubject

    // partisan
    override fun smsForResetSet(): Observable<String> = smsForResetSubject

    override fun languageSelected(): Observable<String> = languageSubject

    override fun render(state: SettingsState) {
        language.summary = state.languageSummary
        themePreview.setBackgroundTint(state.theme)
        night.summary = state.nightModeSummary
        nightModeDialog.adapter.selectedItem = state.nightModeId
        nightStart.setVisible(state.nightModeId == Preferences.NIGHT_MODE_AUTO)
        nightStart.summary = state.nightStart
        nightEnd.setVisible(state.nightModeId == Preferences.NIGHT_MODE_AUTO)
        nightEnd.summary = state.nightEnd

        black.setVisible(state.nightModeId != Preferences.NIGHT_MODE_OFF)
        black.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.black

        autoEmoji.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.autoEmojiEnabled

        delayed.summary = state.sendDelaySummary
        sendDelayDialog.adapter.selectedItem = state.sendDelayId

        delivery.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.deliveryEnabled

        signature.summary = state.signature.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.settings_signature_summary)

        textSize.summary = state.textSizeSummary
        textSizeDialog.adapter.selectedItem = state.textSizeId

        autoColor.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.autoColor

        systemFont.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.systemFontEnabled

        mobileOnly.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.mobileOnly

        autoDelete.summary = when (state.autoDelete) {
            0 -> context.getString(R.string.settings_auto_delete_never)
            else -> context.resources.getQuantityString(
                R.plurals.settings_auto_delete_summary, state.autoDelete, state.autoDelete
            )
        }

        when (state.syncProgress) {
            is SyncRepository.SyncProgress.Idle -> syncingProgress.isVisible = false

            is SyncRepository.SyncProgress.Running -> {
                syncingProgress.isVisible = true
                syncingProgress.max = state.syncProgress.max
                progressAnimator.apply { setIntValues(syncingProgress.progress, state.syncProgress.progress) }.start()
                syncingProgress.isIndeterminate = state.syncProgress.indeterminate
            }
        }

        // partisan
        smsForReset.summary = state.smsForReset

        showInTaskSwitcher.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.showInTaskSwitcher

        if (state.showInTaskSwitcher) {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun showNightModeDialog() {
        activity?.let { nightModeDialog.show(it) }
    }

    override fun showStartTimePicker(hour: Int, minute: Int) {
        TimePickerDialog(activity, { _, newHour, newMinute ->
            startTimeSelectedSubject.onNext(Pair(newHour, newMinute))
        }, hour, minute, DateFormat.is24HourFormat(activity)).show()
    }

    override fun showEndTimePicker(hour: Int, minute: Int) {
        TimePickerDialog(activity, { _, newHour, newMinute ->
            endTimeSelectedSubject.onNext(Pair(newHour, newMinute))
        }, hour, minute, DateFormat.is24HourFormat(activity)).show()
    }

    override fun showTextSizePicker() {
        activity?.let { textSizeDialog.show(it) }
    }

    override fun showDelayDurationDialog() {
        activity?.let { sendDelayDialog.show(it) }
    }

    override fun showSignatureDialog(signature: String) {
        getSignatureDialog()?.setText(signature)?.show()
    }

    override fun showAutoDeleteDialog(days: Int) {
        getAutoDeleteDialog()?.setExpiry(days)?.show()
    }

    override suspend fun showAutoDeleteWarningDialog(messages: Int): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Boolean> { cont ->
            val ctx = activity ?: run { cont.resume(false); return@suspendCancellableCoroutine }
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.settings_auto_delete_warning)
                .setMessage(context.resources.getString(R.string.settings_auto_delete_warning_message, messages))
                .setOnCancelListener { cont.resume(false) }
                .setNegativeButton(R.string.button_cancel) { _, _ -> cont.resume(false) }
                .setPositiveButton(R.string.button_yes) { _, _ -> cont.resume(true) }
                .show()
        }
    }

    override fun showSwipeActions() {
        router.pushController(
            RouterTransaction.with(SwipeActionsController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler())
        )
    }

    override fun showThemePicker() {
        router.pushController(
            RouterTransaction.with(ThemePickerController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler())
        )
    }

    override fun showAbout() {
        router.pushController(
            RouterTransaction.with(AboutController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler())
        )
    }

    override fun showSmsForResetDialog(smsForReset: String) {
        getSmsForResetDialog()?.setText(smsForReset)?.show()
    }

    override fun showLanguageDialog() {
        val locales = listOf(
            "" to context.getString(R.string.settings_language_system),
            "ar" to "العربية",
            "bn" to "বাংলা",
            "cs" to "Čeština",
            "da" to "Dansk",
            "de" to "Deutsch",
            "el" to "Ελληνικά",
            "en" to "English",
            "es" to "Español",
            "fa" to "فارسی",
            "fi" to "Suomi",
            "fr" to "Français",
            "hi" to "हिन्दी",
            "hr" to "Hrvatski",
            "hu" to "Magyar",
            "in" to "Bahasa Indonesia",
            "it" to "Italiano",
            "iw" to "עברית",
            "ja" to "日本語",
            "ko" to "한국어",
            "lt" to "Lietuvių",
            "nb" to "Norsk bokmål",
            "ne" to "नेपाली",
            "nl" to "Nederlands",
            "pl" to "Polski",
            "pt" to "Português",
            "pt-rBR" to "Português (Brasil)",
            "ro" to "Română",
            "ru" to "Русский",
            "sk" to "Slovenčina",
            "sl" to "Slovenščina",
            "sr" to "Српски",
            "sv" to "Svenska",
            "th" to "ไทย",
            "tl" to "Filipino",
            "tr" to "Türkçe",
            "uk" to "Українська",
            "ur" to "اردو",
            "vi" to "Tiếng Việt",
            "zh" to "繁體中文",
            "zh-rCN" to "简体中文"
        )
        val names = locales.map { it.second }.toTypedArray()
        val codes = locales.map { it.first }
        val currentLang = prefs.language.get()
        val checkedItem = codes.indexOf(currentLang).coerceAtLeast(0)

        val ctx = activity ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.settings_language_title)
            .setSingleChoiceItems(names, checkedItem) { dialog, which ->
                languageSubject.onNext(codes[which])
                dialog.dismiss()
                applyLocale(codes[which])
            }
            .show()
    }

    private fun applyLocale(lang: String) {
        val localeList = if (lang.isEmpty()) {
            androidx.core.os.LocaleListCompat.getEmptyLocaleList()
        } else {
            val tag = lang.replace("-r", "-")
            androidx.core.os.LocaleListCompat.forLanguageTags(tag)
        }
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
    }


}
