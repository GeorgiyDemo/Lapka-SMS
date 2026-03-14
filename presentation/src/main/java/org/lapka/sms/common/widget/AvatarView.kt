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
import android.widget.FrameLayout
import org.lapka.sms.R
import org.lapka.sms.common.Navigator
import org.lapka.sms.common.util.Colors
import org.lapka.sms.common.util.extensions.setBackgroundTint
import org.lapka.sms.common.util.extensions.setTint
import org.lapka.sms.injection.appComponent
import org.lapka.sms.model.Recipient
import org.lapka.sms.util.GlideApp
import javax.inject.Inject
import android.widget.ImageView
import org.lapka.sms.common.widget.QkTextView

class AvatarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    @Inject
    lateinit var colors: Colors

    @Inject
    lateinit var navigator: Navigator


    private val initialView: QkTextView by lazy { findViewById(R.id.initial) }
    private val iconView: ImageView by lazy { findViewById(R.id.icon) }
    private val photoView: ImageView by lazy { findViewById(R.id.photo) }

    private var lookupKey: String? = null
    private var fullName: String? = null
    private var photoUri: String? = null
    private var lastUpdated: Long? = null
    private var theme: Colors.Theme

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
        }

        theme = colors.theme()

        View.inflate(context, R.layout.avatar_view, this)
        setBackgroundResource(R.drawable.circle)
        clipToOutline = true
    }

    /**
     * Use the [contact] information to display the avatar.
     */
    fun setRecipient(recipient: Recipient?) {
        lookupKey = recipient?.contact?.lookupKey
        fullName = recipient?.contact?.name
        photoUri = recipient?.contact?.photoUri
        lastUpdated = recipient?.contact?.lastUpdate
        theme = colors.theme(recipient)
        updateView()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (!isInEditMode) {
            updateView()
        }
    }

    private fun updateView() {
        // Apply theme
        setBackgroundTint(theme.theme)
        initialView.setTextColor(theme.textPrimary)
        iconView.setTint(theme.textPrimary)

        val initials = fullName
            ?.substringBefore(',')
            ?.split(" ").orEmpty()
            .filter { name -> name.isNotEmpty() }
            .map { name -> name[0] }
            .filter { initial -> initial.isLetterOrDigit() }
            .map { initial -> initial.toString() }

        if (initials.isNotEmpty()) {
            initialView.text = if (initials.size > 1) initials.first() + initials.last() else initials.first()
            iconView.visibility = GONE
        } else {
            initialView.text = null
            iconView.visibility = VISIBLE
        }

        photoView.setImageDrawable(null)
        photoUri?.let { photoUri ->
            GlideApp.with(photoView)
                .load(photoUri)
                .into(photoView)
        }
    }
}
