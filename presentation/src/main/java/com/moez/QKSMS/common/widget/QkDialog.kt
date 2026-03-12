/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
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
package com.moez.QKSMS.common.widget

import android.app.Activity
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkAdapter
import androidx.recyclerview.widget.RecyclerView
import com.moez.QKSMS.common.widget.QkTextView

class QkDialog(private val context: Activity) :
    AlertDialog(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog) {

    private val view = LayoutInflater.from(context).inflate(R.layout.qk_dialog, null)


    private val titleView: QkTextView = view.findViewById(R.id.title)
    private val subtitleView: QkTextView = view.findViewById(R.id.subtitle)
    private val listView: RecyclerView = view.findViewById(R.id.list)
    private val positiveBtn: QkTextView = view.findViewById(R.id.positiveButton)
    private val negativeBtn: QkTextView = view.findViewById(R.id.negativeButton)

    @StringRes
    var titleRes: Int? = null
        set(value) {
            field = value
            title = value?.let(context::getString)
        }

    var title: String? = null
        set(value) {
            field = value
            titleView.text = value
            titleView.isVisible = !value.isNullOrBlank()
        }

    @StringRes
    var subtitleRes: Int? = null
        set(value) {
            field = value
            subtitle = value?.let(context::getString)
        }

    var subtitle: String? = null
        set(value) {
            field = value
            subtitleView.text = value
            subtitleView.isVisible = !value.isNullOrBlank()
        }

    var adapter: QkAdapter<*>? = null
        set(value) {
            field = value
            listView.isVisible = value != null
            listView.adapter = value
        }

    var positiveButtonListener: (() -> Unit)? = null

    @StringRes
    var positiveButton: Int? = null
        set(value) {
            field = value
            value?.run(positiveBtn::setText)
            positiveBtn.isVisible = value != null
            positiveBtn.setOnClickListener {
                positiveButtonListener?.invoke() ?: dismiss()
            }
        }

    var negativeButtonListener: (() -> Unit)? = null

    @StringRes
    var negativeButton: Int? = null
        set(value) {
            field = value
            value?.run(negativeBtn::setText)
            negativeBtn.isVisible = value != null
            negativeBtn.setOnClickListener {
                negativeButtonListener?.invoke() ?: dismiss()
            }
        }

    var cancelListener: (() -> Unit)? = null
        set(value) {
            field = value
            setOnCancelListener { value?.invoke() }
        }

    init {
        setView(view)
    }

}
