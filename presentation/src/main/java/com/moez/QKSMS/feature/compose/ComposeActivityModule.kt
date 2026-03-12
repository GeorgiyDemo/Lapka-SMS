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
package com.moez.QKSMS.feature.compose

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.moez.QKSMS.injection.ViewModelKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import java.net.URLDecoder
import javax.inject.Named

@Module
class ComposeActivityModule {

    @Provides
    @Named("query")
    fun provideQuery(activity: ComposeActivity): String = activity.intent.extras?.getString("query") ?: ""

    @Provides
    @Named("threadId")
    fun provideThreadId(activity: ComposeActivity): Long = activity.intent.extras?.getLong("threadId") ?: 0L

    @Provides
    @Named("addresses")
    fun provideAddresses(activity: ComposeActivity): List<String> {
        return activity.intent
                ?.decodedDataString()
                ?.substringAfter(':') // Remove scheme
                ?.substringBefore("?") // Remove query
                ?.split(",", ";")
                ?.filter { number -> number.isNotEmpty() }
                ?: listOf()
    }

    @Provides
    @Named("text")
    fun provideSharedText(activity: ComposeActivity): String {
        var subject = activity.intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "";
        if (subject != "") {
            subject += "\n"
        }

        return subject + (activity.intent.extras?.getString(Intent.EXTRA_TEXT)
                ?: activity.intent.extras?.getString("sms_body")
                ?: activity.intent?.decodedDataString()
                        ?.substringAfter('?') // Query string
                        ?.takeIf { it.startsWith("body") }
                        ?.substringAfter('=')
                ?: "")
    }

    @Provides
    @IntoMap
    @ViewModelKey(ComposeViewModel::class)
    fun provideComposeViewModel(viewModel: ComposeViewModel): ViewModel = viewModel

    // The dialer app on Oreo sends a URL encoded string, make sure to decode it
    private fun Intent.decodedDataString(): String? {
        val data = data?.toString()
        if (data?.contains('%') == true) {
            return URLDecoder.decode(data, "UTF-8")
        }
        return data
    }

}
