package com.moez.QKSMS.feature.keysettings

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import java.security.MessageDigest
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.qrcode.QRCodeWriter
import com.jakewharton.rxbinding2.view.clicks
import com.moez.QKSMS.R
import com.moez.QKSMS.common.QkDialog
import com.moez.QKSMS.common.base.QkController
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.TextViewStyler
import com.moez.QKSMS.common.util.extensions.animateLayoutChanges
import com.moez.QKSMS.common.util.extensions.setBackgroundTint
import com.moez.QKSMS.common.widget.PreferenceView
import com.moez.QKSMS.common.widget.QkSwitch
import com.moez.QKSMS.extensions.Optional
import com.moez.QKSMS.feature.keysettings.injection.KeySettingsModule
import com.moez.QKSMS.injection.appComponent
import com.moez.QKSMS.util.Preferences
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject


class KeySettingsController(
    val threadId: Long = KeySettingsInvalidThreadId
) : QkController<KeySettingsView, KeySettingsState, KeySettingsPresenter>(), KeySettingsView {

    companion object {
        const val EncryptionKeyKey = "encryption_key"
    }

    @Inject lateinit var prefs: Preferences
    @Inject lateinit var colors: Colors
    @Inject lateinit var context: Context
    @Inject lateinit var qrCodeWriter: QRCodeWriter
    @Inject lateinit var compatibilityModeDialog: QkDialog
    @Inject lateinit var textViewStyler: TextViewStyler
    @Inject override lateinit var presenter: KeySettingsPresenter

    override val keyResetConfirmed: Subject<Unit> = PublishSubject.create()
    override val keyDisableConfirmed: Subject<Unit> = PublishSubject.create()
    override val optionsItemIntent: Subject<Int> = PublishSubject.create()
    override val backClicked: Subject<Unit> = PublishSubject.create()
    override val exitWithSavingIntent: Subject<Boolean> = PublishSubject.create()
    override val qrScannedIntent: Subject<String> = PublishSubject.create()
    override val schemeChanged: Subject<Int> = PublishSubject.create()
    override val stateRestored: Subject<Optional<KeySettingsState>> = PublishSubject.create()

    private val keyTextWatcher = KeyTextWatcher()
    private var scannedQr: String? = null
    private var savedState: KeySettingsState? = null

    // View accessors
    private val preferences: LinearLayout get() = containerView!!.findViewById(R.id.preferences)
    private val encryptionKeyCategory: TextView get() = containerView!!.findViewById(R.id.encryptionKeyCategory)
    private val enableKey: PreferenceView get() = containerView!!.findViewById(R.id.enableKey)
    private val keyInputGroup: LinearLayoutCompat get() = containerView!!.findViewById(R.id.keyInputGroup)
    private val scanQr: PreferenceView get() = containerView!!.findViewById(R.id.scanQr)
    private val generateKey: PreferenceView get() = containerView!!.findViewById(R.id.generateKey)
    private val resetKey: PreferenceView get() = containerView!!.findViewById(R.id.resetKey)
    private val keyField: EditText get() = containerView!!.findViewById(R.id.keyField)
    private val copyKey: ImageButton get() = containerView!!.findViewById(R.id.copyKey)
    private val qrCodeImage: ImageView get() = containerView!!.findViewById(R.id.qrCodeImage)
    private val keyFingerprint: TextView get() = containerView!!.findViewById(R.id.keyFingerprint)
    private val legacyEncryption: PreferenceView get() = containerView!!.findViewById(R.id.legacyEncryption)
    private val legacyEncryptionConversation: PreferenceView get() = containerView!!.findViewById(R.id.legacyEncryptionConversation)
    private val encodingSchemes: RadioGroup get() = containerView!!.findViewById(R.id.encodingSchemes)
    private val schemeBase64: RadioButton get() = containerView!!.findViewById(R.id.schemeBase64)
    private val schemeBase64Cyrillic: RadioButton get() = containerView!!.findViewById(R.id.schemeBase64Cyrillic)
    private val schemeRussianWords: RadioButton get() = containerView!!.findViewById(R.id.schemeRussianWords)
    private val schemeDefault: RadioButton get() = containerView!!.findViewById(R.id.schemeDefault)

    init {
        appComponent
            .keySettingsBuilder()
            .keySettingsModule(KeySettingsModule(this))
            .build()
            .inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.settings_keys_activity
    }

    override fun preferenceClicks(): Observable<PreferenceView> = (0 until preferences.childCount)
        .map { index -> preferences.getChildAt(index) }
        .mapNotNull { view -> view as? PreferenceView }
        .map { preference -> preference.clicks().map { preference } }
        .let { preferences -> Observable.merge(preferences) }

    override fun compatibilityModeSelected(): Observable<Int> = compatibilityModeDialog.adapter.menuItemClicks

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        savedState = args.getParcelable("state")
        super.onRestoreViewState(view, savedViewState)
    }

    override fun render(state: KeySettingsState) {
        if (!state.bound) {
            return
        }
        if (state.hasError) {
            activity?.finish()
            return
        }

        args.putParcelable("state", state)
        encryptionKeyCategory.text =
            if (!state.isConversation) context.getText(R.string.settings_global_encryption_key_title)
            else context.getText(R.string.settings_conversation_encryption_key_title)

        enableKey.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.keyEnabled

        keyInputGroup.visibility = if (state.keySettingsIsShown) View.VISIBLE else View.GONE
        scanQr.visibility = if (state.keySettingsIsShown) View.VISIBLE else View.GONE
        generateKey.visibility = if (state.keySettingsIsShown) View.VISIBLE else View.GONE
        resetKey.visibility = if (state.resetKeyIsShown) View.VISIBLE else View.GONE
        keyField.setBackgroundTint(colors.theme().theme)
        if (state.keySettingsIsShown) {
            if (state.keyValid) {
                if (keyField.text.toString() != state.key) {
                    keyField.setText(state.key)
                }
                keyField.error = null
                qrCodeImage.visibility = View.VISIBLE
                renderQr(state.key)
                keyFingerprint.visibility = View.VISIBLE
                keyFingerprint.text = context.getString(R.string.key_fingerprint, computeFingerprint(state.key))
            } else {
                keyField.error = context.getText(R.string.settings_bad_key)
                qrCodeImage.visibility = View.GONE
                keyFingerprint.visibility = View.GONE
            }
        }

        val nonKeyEncryptionSettingsEnabled = state.keyEnabled
                || state.isConversation && prefs.globalEncryptionKey.get().isNotBlank()
        if (state.isConversation) {
            val strings = context.resources.getStringArray(R.array.compatibility_mode_settings_conversation)
            val selectedItem = when(state.legacyEncryptionEnabled) {
                null -> 0
                false -> 1
                true -> 2
            }
            legacyEncryption.visibility = View.GONE
            legacyEncryptionConversation.visibility = View.VISIBLE
            legacyEncryptionConversation.summary = strings[selectedItem]
            legacyEncryptionConversation.isEnabled = nonKeyEncryptionSettingsEnabled
            compatibilityModeDialog.adapter.selectedItem = selectedItem
        } else {
            legacyEncryption.visibility = View.VISIBLE
            legacyEncryption.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.legacyEncryptionEnabled ?: false
            legacyEncryption.isEnabled = nonKeyEncryptionSettingsEnabled
            legacyEncryptionConversation.visibility = View.GONE
        }

        schemeDefault.visibility = if (state.isConversation) View.VISIBLE else View.GONE
        renderEncodingRadioButton(schemeBase64, nonKeyEncryptionSettingsEnabled)
        renderEncodingRadioButton(schemeBase64Cyrillic, nonKeyEncryptionSettingsEnabled)
        renderEncodingRadioButton(schemeRussianWords, nonKeyEncryptionSettingsEnabled)
        renderEncodingRadioButton(schemeDefault, nonKeyEncryptionSettingsEnabled)
        if (state.encodingScheme >= 0) {
            encodingSchemes.check(encodingSchemes[state.encodingScheme].id)
        }
        if (nonKeyEncryptionSettingsEnabled) {
            encodingSchemes.setOnCheckedChangeListener { _, id ->
                val radioButton = encodingSchemes.findViewById<RadioButton>(id)
                val scheme = encodingSchemes.indexOfChild(radioButton)
                schemeChanged.onNext(scheme)
            }
        } else {
            encodingSchemes.setOnCheckedChangeListener(null)
        }
    }

    private fun renderEncodingRadioButton(radioButton: RadioButton, nonKeyEncryptionSettingsEnabled: Boolean) {
        radioButton.isEnabled = nonKeyEncryptionSettingsEnabled
        textViewStyler.applyAttributes(radioButton, null)
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked, android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_checked, android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_enabled)
            ), intArrayOf(
                radioButton.hintTextColors.getColorForState(intArrayOf(-android.R.attr.state_enabled), -1),
                colors.theme().theme,
                radioButton.textColors.getColorForState(intArrayOf(-android.R.attr.state_enabled), -1)
            )
        )
        radioButton.buttonTintList = colorStateList
    }

    private fun renderQr(key: String) {
        val matrix = qrCodeWriter.encode(key, BarcodeFormat.QR_CODE, 512, 512)
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
        for (i in 0 until matrix.width)
            for (j in 0 until matrix.height) {
                bitmap.setPixel(i,j, if(matrix[i,j]) Color.BLACK else Color.WHITE)
            }
        qrCodeImage.setImageBitmap(bitmap)
    }

    private fun computeFingerprint(base64Key: String): String {
        return try {
            val keyBytes = Base64.decode(base64Key, Base64.DEFAULT)
            val hash = MessageDigest.getInstance("SHA-256").digest(keyBytes)
            hash.take(8).joinToString(" ") { "%02X".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    override fun onViewCreated() {
        super.onViewCreated()
        preferences.postDelayed( { containerView?.findViewById<LinearLayout>(R.id.preferences)?.animateLayoutChanges = true }, 100)

        keyField.addTextChangedListener(keyTextWatcher)
        copyKey.setOnClickListener { copyKey() }
        compatibilityModeDialog.adapter.setData(R.array.compatibility_mode_settings_conversation)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        stateRestored.onNext(Optional(savedState))
        savedState = null
        checkScannedQr()
        setTitle(R.string.settings_encryption_key_title)
        showBackButton(true)
        setHasOptionsMenu(true)
    }

    private fun checkScannedQr() {
        val qr = scannedQr
        if (qr != null) {
            qrScannedIntent.onNext(qr)
            scannedQr = null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        optionsItemIntent.onNext(item.itemId)
        return true
    }

    override fun handleBack(): Boolean {
        backClicked.onNext(Unit)
        return true
    }

    override fun copyKey() {
        keyField.apply {
            if(copyToClipboard()) {
                selectAll()
                Toast.makeText(context, R.string.encryption_key_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun scanQrCode() {
        val keySettingsActivity = activity as? KeySettingsActivity ?: return
        val intent = IntentIntegrator(this.themedActivity)
            .setBeepEnabled(false)
            .setOrientationLocked(true)
            .setBarcodeImageEnabled(true)
            .createScanIntent()
        keySettingsActivity.onQrResult = { data ->
            if (data != null) {
                val qrResult = IntentIntegrator.parseActivityResult(Activity.RESULT_OK, data)
                if (qrResult != null && qrResult.contents != null) {
                    scannedQr = qrResult.contents
                }
            }
        }
        keySettingsActivity.qrScanLauncher.launch(intent)
    }

    override fun keySet() {
        val text = context.getText(R.string.settings_key_has_been_set)
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    override fun keyNotSet() {
        val text = context.getText(R.string.settings_bad_key)
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
    override fun keyChanged(): Observable<String> = keyTextWatcher.keyChanged

    private fun EditText.copyToClipboard(): Boolean {
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        return if (text.isNotBlank() && clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.conversation_encryption_key_title), text))
            // Auto-clear clipboard after 30 seconds for security
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                } catch (_: Exception) {}
            }, 30_000)
            true
        } else false
    }

    override fun showCompatibilityModeDialog() = compatibilityModeDialog.show(activity!!)

    override fun showResetKeyDialog(disableKey: Boolean) {
        MaterialAlertDialogBuilder(this.activity!!)
            .setMessage(R.string.settings_reset_key_confirmation_text)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.button_reset) { _, _ ->
                if (disableKey) {
                    keyDisableConfirmed.onNext(Unit)
                } else {
                    keyResetConfirmed.onNext(Unit)
                }
            }
            .create()
            .show()
    }

    override fun showSaveDialog(allowSave: Boolean) {
        val builder = MaterialAlertDialogBuilder(this.activity!!)
            .setMessage(R.string.settings_exit_with_no_changes)
            .setNeutralButton(R.string.button_cancel, null)
            .setNegativeButton(R.string.rate_dismiss) { _, _ -> exitWithSavingIntent.onNext(false) }
        if (allowSave) {
            builder.setPositiveButton(R.string.button_save) { _, _ -> exitWithSavingIntent.onNext(true) }
        }
        builder.create().show()
    }

    override fun goBack() {
        activity?.finish()
    }

    override fun onSaved(key: String?) {
        val intent = Intent().putExtra(EncryptionKeyKey, key)
        activity?.setResult(Activity.RESULT_OK, intent)
    }
}
