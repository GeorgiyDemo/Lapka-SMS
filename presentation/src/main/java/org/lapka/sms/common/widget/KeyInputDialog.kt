package org.lapka.sms.common.widget

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.lapka.sms.R
import javax.crypto.KeyGenerator

class KeyInputDialog(private val context: Activity, private val hint: String, private val listener: (String) -> Unit) {

    private var text: String = ""

    fun setText(text: String): KeyInputDialog {
        this.text = text
        return this
    }

    fun show() {
        val layout = LayoutInflater.from(context).inflate(R.layout.key_input_dialog, null)
        val field = layout.findViewById<EditText>(R.id.field)
        field.hint = hint
        field.setText(text)
        if (text.isNotEmpty() && !validate(text)) {
            field.error = context.getString(R.string.invalid_key)
        }

        layout.findViewById<View>(R.id.btnGenerateKey).setOnClickListener {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            field.setText(Base64.encodeToString(keyGen.generateKey().encoded, Base64.NO_WRAP))
        }

        val clipboardHandler = Handler(Looper.getMainLooper())
        var clipboardClearRunnable: Runnable? = null

        layout.findViewById<View>(R.id.btnCopyKey).setOnClickListener {
            val clipboard = getSystemService(context, ClipboardManager::class.java)
            if (field.text.isNotBlank() && clipboard != null) {
                clipboard.setPrimaryClip(
                    ClipData.newPlainText(context.getString(R.string.conversation_encryption_key_title), field.text)
                )
                field.selectAll()
                Toast.makeText(context, R.string.encryption_key_copied, Toast.LENGTH_SHORT).show()
                // Auto-clear clipboard after 30 seconds for security
                clipboardClearRunnable?.let { clipboardHandler.removeCallbacks(it) }
                val runnable = Runnable {
                    try {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    } catch (_: Exception) {
                    }
                }
                clipboardClearRunnable = runnable
                clipboardHandler.postDelayed(runnable, 30_000)
            }
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(layout)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.button_save, null)
            .create()

        dialog.setOnDismissListener {
            clipboardClearRunnable?.let { clipboardHandler.removeCallbacks(it) }
        }

        dialog.show()

        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val value = field.text.toString()
            if (validate(value)) {
                listener(value)
                dialog.dismiss()
            } else {
                field.error = context.getString(R.string.invalid_key)
            }
        }
    }

    private fun validate(text: String): Boolean {
        return try {
            if (text.isEmpty()) return true
            val data = Base64.decode(text, Base64.DEFAULT)
            data.size == 16 || data.size == 24 || data.size == 32
        } catch (ignored: IllegalArgumentException) {
            false
        }
    }
}
