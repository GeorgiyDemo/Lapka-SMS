package com.moez.QKSMS.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Stub receiver for WAP_PUSH_DELIVER.
 * Required for the app to be eligible as default SMS app on Android.
 * MMS messages are not processed.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: MMS is not supported
    }
}
