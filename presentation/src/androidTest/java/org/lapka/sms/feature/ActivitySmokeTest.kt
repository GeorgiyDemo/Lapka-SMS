package org.lapka.sms.feature

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.lapka.sms.feature.blocking.BlockingActivity
import org.lapka.sms.feature.compose.ComposeActivity
import org.lapka.sms.feature.contacts.ContactsActivity
import org.lapka.sms.feature.conversationinfo.ConversationInfoActivity
import org.lapka.sms.feature.keysettings.KeySettingsActivity
import org.lapka.sms.feature.main.MainActivity
import org.lapka.sms.feature.notificationprefs.NotificationPrefsActivity
import org.lapka.sms.feature.qkreply.QkReplyActivity
import org.lapka.sms.feature.settings.SettingsActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivitySmokeTest {

    private inline fun <reified T : android.app.Activity> launchAndVerify(
        allowFinish: Boolean = false,
        intentSetup: Intent.() -> Unit = {}
    ) {
        val intent = Intent(ApplicationProvider.getApplicationContext(), T::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intentSetup()
        }
        val scenario = ActivityScenario.launch<T>(intent)
        if (!allowFinish) {
            scenario.onActivity { activity ->
                assert(activity != null) { "${T::class.simpleName} failed to create" }
                assert(!activity.isFinishing) { "${T::class.simpleName} is finishing immediately" }
            }
        }
        scenario.close()
    }

    @Test
    fun mainActivity_launches() {
        launchAndVerify<MainActivity>()
    }

    @Test
    fun settingsActivity_launches() {
        launchAndVerify<SettingsActivity>()
    }

    @Test
    fun keySettingsActivity_launches() {
        launchAndVerify<KeySettingsActivity> {
            putExtra("threadId", -1L)
        }
    }

    @Test
    fun blockingActivity_launches() {
        launchAndVerify<BlockingActivity>()
    }

    @Test
    fun contactsActivity_launches() {
        launchAndVerify<ContactsActivity>()
    }

    @Test
    fun composeActivity_launches() {
        launchAndVerify<ComposeActivity>()
    }

    @Test
    fun conversationInfoActivity_launches() {
        // threadId=0 causes the activity to finish (no such conversation) — that's expected
        launchAndVerify<ConversationInfoActivity>(allowFinish = true) {
            putExtra("threadId", 0L)
        }
    }

    @Test
    fun notificationPrefsActivity_launches() {
        launchAndVerify<NotificationPrefsActivity> {
            putExtra("threadId", 0L)
        }
    }

    @Test
    fun qkReplyActivity_launches() {
        launchAndVerify<QkReplyActivity> {
            putExtra("threadId", 0L)
        }
    }

}
