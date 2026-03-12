package com.moez.QKSMS.common.widget

import android.app.Activity
import android.view.LayoutInflater
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.moez.QKSMS.R

class TextInputDialog(private val context: Activity, private val hint: String, private val listener: (String) -> Unit) {

    private var text: String = ""

    fun setText(text: String): TextInputDialog {
        this.text = text
        return this
    }

    fun show() {
        val layout = LayoutInflater.from(context).inflate(R.layout.text_input_dialog, null)
        val field = layout.findViewById<EditText>(R.id.field)
        field.hint = hint
        field.setText(text)

        MaterialAlertDialogBuilder(context)
            .setView(layout)
            .setNeutralButton(R.string.button_cancel, null)
            .setNegativeButton(R.string.button_delete) { _, _ -> listener("") }
            .setPositiveButton(R.string.button_save) { _, _ ->
                listener(field.text.toString())
            }
            .show()
    }
}
