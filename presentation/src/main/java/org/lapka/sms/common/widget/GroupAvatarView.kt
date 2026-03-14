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
package org.lapka.sms.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import org.lapka.sms.R
import org.lapka.sms.common.util.extensions.getColorCompat
import org.lapka.sms.common.util.extensions.resolveThemeColor
import org.lapka.sms.common.util.extensions.setBackgroundTint
import org.lapka.sms.model.Recipient
import android.widget.FrameLayout

class GroupAvatarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {


    private val avatar1Frame: FrameLayout by lazy { findViewById(R.id.avatar1Frame) }
    private val avatar1: AvatarView by lazy { findViewById(R.id.avatar1) }
    private val avatar2Frame: FrameLayout by lazy { findViewById(R.id.avatar2Frame) }
    private val avatar2: AvatarView by lazy { findViewById(R.id.avatar2) }

    var recipients: List<Recipient> = ArrayList()
        set(value) {
            field = value.sortedWith(compareByDescending { contact -> contact.contact?.lookupKey })
            updateView()
        }

    init {
        View.inflate(context, R.layout.group_avatar_view, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (!isInEditMode) {
            updateView()
        }
    }

    private fun updateView() {
        avatar1Frame.setBackgroundTint(
            when (recipients.size > 1) {
                true -> context.resolveThemeColor(android.R.attr.windowBackground)
                false -> context.getColorCompat(android.R.color.transparent)
            }
        )
        avatar1Frame.updateLayoutParams<LayoutParams> {
            matchConstraintPercentWidth = if (recipients.size > 1) 0.75f else 1.0f
        }
        avatar2.isVisible = recipients.size > 1


        recipients.getOrNull(0).run(avatar1::setRecipient)
        recipients.getOrNull(1).run(avatar2::setRecipient)
    }

}
