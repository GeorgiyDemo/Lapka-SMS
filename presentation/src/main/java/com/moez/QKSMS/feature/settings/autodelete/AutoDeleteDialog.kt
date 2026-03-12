package com.moez.QKSMS.feature.settings.autodelete

import android.app.Activity
import android.view.LayoutInflater
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.moez.QKSMS.R

class AutoDeleteDialog(private val context: Activity, private val listener: (Int) -> Unit) {

    private var days: Int = 0

    fun setExpiry(days: Int): AutoDeleteDialog {
        this.days = days
        return this
    }

    fun show() {
        val layout = LayoutInflater.from(context).inflate(R.layout.settings_auto_delete_dialog, null)
        val field = layout.findViewById<EditText>(R.id.field)
        if (days > 0) field.setText(days.toString())

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_auto_delete)
            .setMessage(context.getString(R.string.settings_auto_delete_dialog_message))
            .setView(layout)
            .setNeutralButton(R.string.button_cancel, null)
            .setNegativeButton(R.string.settings_auto_delete_never) { _, _ -> listener(0) }
            .setPositiveButton(R.string.button_save) { _, _ ->
                listener(field.text.toString().toIntOrNull() ?: 0)
            }
            .show()
    }
}
