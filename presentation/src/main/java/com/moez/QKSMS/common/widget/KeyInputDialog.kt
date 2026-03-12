package com.moez.QKSMS.common.widget

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.util.Base64
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import com.moez.QKSMS.R
import javax.crypto.KeyGenerator
import android.view.View


class KeyInputDialog(context: Activity, hint: String, val listener: (String) -> Unit) : AlertDialog(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog) {

    private val layout = LayoutInflater.from(context).inflate(R.layout.key_input_dialog, null)

    init {
        layout.apply {
            findViewById<EditText>(R.id.field).hint = hint
            findViewById<View>(R.id.btnGenerateKey).setOnClickListener {
                generateKey()
            }
            findViewById<View>(R.id.btnCopyKey).setOnClickListener {
                findViewById<EditText>(R.id.field).apply {
                    if(copyToClipboard()) {
                        selectAll()
                        Toast.makeText(context, R.string.encryption_key_copied, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setView(layout)
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.button_cancel), null as DialogInterface.OnClickListener?)
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.button_save), null as DialogInterface.OnClickListener?)
    }


    fun setText(text: String): KeyInputDialog {
        if (validate(text)) {
            layout.findViewById<EditText>(R.id.field).setText(text)
        } else {
            layout.findViewById<EditText>(R.id.field).setText(text)
            layout.findViewById<EditText>(R.id.field).error = context.resources.getString(R.string.invalid_key)
        }
        return this
    }

    private fun EditText.copyToClipboard(): Boolean {
        val clipboard = getSystemService(context, ClipboardManager::class.java)
        return if (text.isNotBlank() && clipboard != null) {
            clipboard.setPrimaryClip(
                ClipData.newPlainText(resources.getString(R.string.conversation_encryption_key_title), text)
            )
            true
        } else false
    }

    private fun validate(text: String): Boolean {
        return try {
            if (text.isEmpty()) {
                return true
            }
            val data = Base64.decode(text, Base64.DEFAULT)
            data.size == 16 || data.size == 24 || data.size == 32
        } catch (ignored: IllegalArgumentException) {
            false
        }
    }

    private fun generateKey() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey = keyGen.generateKey()
        layout.findViewById<EditText>(R.id.field).setText(Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP))
    }

    override fun show() {
        super.show()
        getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (validate(layout.findViewById<EditText>(R.id.field).text.toString())) {
                listener(layout.findViewById<EditText>(R.id.field).text.toString())
                dismiss()
            } else {
                layout.findViewById<EditText>(R.id.field).error = context.resources.getString(R.string.invalid_key)
            }
        }
    }
}