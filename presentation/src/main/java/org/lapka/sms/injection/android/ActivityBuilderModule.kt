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
package org.lapka.sms.injection.android

import org.lapka.sms.feature.blocking.BlockingActivity
import org.lapka.sms.feature.compose.ComposeActivity
import org.lapka.sms.feature.compose.ComposeActivityModule
import org.lapka.sms.feature.contacts.ContactsActivity
import org.lapka.sms.feature.contacts.ContactsActivityModule
import org.lapka.sms.feature.conversationinfo.ConversationInfoActivity
import org.lapka.sms.feature.keysettings.KeySettingsActivity
import org.lapka.sms.feature.main.MainActivity
import org.lapka.sms.feature.main.MainActivityModule
import org.lapka.sms.feature.notificationprefs.NotificationPrefsActivity
import org.lapka.sms.feature.notificationprefs.NotificationPrefsActivityModule
import org.lapka.sms.feature.qkreply.QkReplyActivity
import org.lapka.sms.feature.qkreply.QkReplyActivityModule
import org.lapka.sms.feature.settings.SettingsActivity
import org.lapka.sms.injection.scope.ActivityScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBuilderModule {

    @ActivityScope
    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindMainActivity(): MainActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [ComposeActivityModule::class])
    abstract fun bindComposeActivity(): ComposeActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [ContactsActivityModule::class])
    abstract fun bindContactsActivity(): ContactsActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindConversationInfoActivity(): ConversationInfoActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [NotificationPrefsActivityModule::class])
    abstract fun bindNotificationPrefsActivity(): NotificationPrefsActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [QkReplyActivityModule::class])
    abstract fun bindQkReplyActivity(): QkReplyActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindSettingsActivity(): SettingsActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindKeysSettingsActivity(): KeySettingsActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindBlockingActivity(): BlockingActivity

}
