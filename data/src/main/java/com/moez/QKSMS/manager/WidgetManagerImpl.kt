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
package com.moez.QKSMS.manager

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import javax.inject.Inject

class WidgetManagerImpl @Inject constructor(private val context: Context) : WidgetManager {

    override fun updateUnreadCount() {
        sendExplicitBroadcast(Intent(), WidgetManager.ACTION_NOTIFY_DATASET_CHANGED)
    }

    override fun updateTheme() {
        val component = ComponentName(context.packageName, "com.moez.QKSMS.feature.widget.WidgetProvider")
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(component)

        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            .setComponent(component)

        try {
            context.sendBroadcast(intent)
        } catch (e: SecurityException) {
            // Ignore — widget may not be placed
        }
    }

    private fun sendExplicitBroadcast(intent: Intent, action: String) {
        intent.action = action
        val resolveInfos = context.packageManager.queryBroadcastReceivers(intent, 0)
        for (info in resolveInfos) {
            val explicit = Intent(intent)
            explicit.component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
            try {
                context.sendBroadcast(explicit)
            } catch (e: SecurityException) {
                // Skip receivers we're not allowed to broadcast to
            }
        }
        if (resolveInfos.isEmpty()) {
            context.sendBroadcast(intent)
        }
    }

}