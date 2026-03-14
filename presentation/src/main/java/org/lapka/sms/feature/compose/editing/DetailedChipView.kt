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
package org.lapka.sms.feature.compose.editing

import android.content.Context
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.RelativeLayout
import org.lapka.sms.R
import org.lapka.sms.common.util.Colors
import org.lapka.sms.common.util.extensions.setBackgroundTint
import org.lapka.sms.common.util.extensions.setTint
import org.lapka.sms.injection.appComponent
import org.lapka.sms.model.Recipient
import javax.inject.Inject
import android.widget.ImageView
import org.lapka.sms.common.widget.AvatarView
import org.lapka.sms.common.widget.QkTextView
import com.google.android.material.card.MaterialCardView

class DetailedChipView(context: Context) : RelativeLayout(context) {

    @Inject
    lateinit var colors: Colors


    private val avatarView: AvatarView by lazy { findViewById(R.id.avatar) }
    private val nameView: QkTextView by lazy { findViewById(R.id.name) }
    private val infoView: QkTextView by lazy { findViewById(R.id.info) }
    private val deleteView: ImageView by lazy { findViewById(R.id.delete) }
    private val cardView: MaterialCardView by lazy { findViewById(R.id.card) }

    init {
        View.inflate(context, R.layout.contact_chip_detailed, this)
        appComponent.inject(this)

        setOnClickListener { hide() }

        visibility = View.GONE

        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setRecipient(recipient: Recipient) {
        avatarView.setRecipient(recipient)
        nameView.text = recipient.contact?.name?.takeIf { it.isNotBlank() } ?: recipient.address
        infoView.text = recipient.address

        colors.theme(recipient).let { theme ->
            cardView.setBackgroundTint(theme.theme)
            nameView.setTextColor(theme.textPrimary)
            infoView.setTextColor(theme.textTertiary)
            deleteView.setTint(theme.textPrimary)
        }
    }

    fun show() {
        startAnimation(AlphaAnimation(0f, 1f).apply { duration = 200 })

        visibility = View.VISIBLE
        requestFocus()
        isClickable = true
    }

    fun hide() {
        startAnimation(AlphaAnimation(1f, 0f).apply { duration = 200 })

        visibility = View.GONE
        clearFocus()
        isClickable = false
    }

    fun setOnDeleteListener(listener: (View) -> Unit) {
        deleteView.setOnClickListener(listener)
    }

}
