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
package org.lapka.sms.injection

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.lifecycle.ViewModelProvider
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.zxing.qrcode.QRCodeWriter
import org.lapka.sms.blocking.BlockingClient
import org.lapka.sms.blocking.BlockingManager
import org.lapka.sms.common.ViewModelFactory
import org.lapka.sms.common.util.NotificationManagerImpl
import org.lapka.sms.common.util.ShortcutManagerImpl
import org.lapka.sms.feature.conversationinfo.injection.ConversationInfoComponent
import org.lapka.sms.feature.keysettings.injection.KeySettingsComponent
import org.lapka.sms.feature.themepicker.injection.ThemePickerComponent
import org.lapka.sms.listener.ContactAddedListener
import org.lapka.sms.listener.ContactAddedListenerImpl
import org.lapka.sms.manager.ActiveConversationManager
import org.lapka.sms.manager.ActiveConversationManagerImpl
import org.lapka.sms.manager.AlarmManager
import org.lapka.sms.manager.AlarmManagerImpl
import org.lapka.sms.manager.AnalyticsManager
import org.lapka.sms.manager.AnalyticsManagerImpl
import org.lapka.sms.manager.ChangelogManager
import org.lapka.sms.manager.ChangelogManagerImpl
import org.lapka.sms.manager.KeyManager
import org.lapka.sms.manager.KeyManagerImpl
import org.lapka.sms.manager.NotificationManager
import org.lapka.sms.manager.PermissionManager
import org.lapka.sms.manager.PermissionManagerImpl
import org.lapka.sms.manager.RatingManager
import org.lapka.sms.manager.ReferralManager
import org.lapka.sms.manager.ReferralManagerImpl
import org.lapka.sms.manager.ShortcutManager
import org.lapka.sms.mapper.CursorToContact
import org.lapka.sms.mapper.CursorToContactGroup
import org.lapka.sms.mapper.CursorToContactGroupImpl
import org.lapka.sms.mapper.CursorToContactGroupMember
import org.lapka.sms.mapper.CursorToContactGroupMemberImpl
import org.lapka.sms.mapper.CursorToContactImpl
import org.lapka.sms.mapper.CursorToConversation
import org.lapka.sms.mapper.CursorToConversationImpl
import org.lapka.sms.mapper.CursorToMessage
import org.lapka.sms.mapper.CursorToMessageImpl
import org.lapka.sms.mapper.CursorToRecipient
import org.lapka.sms.mapper.CursorToRecipientImpl
import org.lapka.sms.mapper.RatingManagerImpl
import org.lapka.sms.repository.BlockingRepository
import org.lapka.sms.repository.BlockingRepositoryImpl
import org.lapka.sms.repository.ContactRepository
import org.lapka.sms.repository.ContactRepositoryImpl
import org.lapka.sms.repository.ConversationRepository
import org.lapka.sms.repository.ConversationRepositoryImpl
import org.lapka.sms.repository.MessageRepository
import org.lapka.sms.repository.MessageRepositoryImpl
import org.lapka.sms.repository.SyncRepository
import org.lapka.sms.repository.SyncRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    subcomponents = [
        ConversationInfoComponent::class,
        KeySettingsComponent::class,
        ThemePickerComponent::class]
)
class AppModule(private var application: Application) {

    @Provides
    @Singleton
    fun provideContext(): Context = application

    @Provides
    fun provideContentResolver(context: Context): ContentResolver = context.contentResolver

    @Provides
    @Singleton
    fun provideSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideRxPreferences(preferences: SharedPreferences): RxSharedPreferences {
        return RxSharedPreferences.create(preferences)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    fun provideViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory = factory

    // Listener

    @Provides
    fun provideContactAddedListener(listener: ContactAddedListenerImpl): ContactAddedListener = listener

    // Manager

    @Provides
    fun provideActiveConversationManager(manager: ActiveConversationManagerImpl): ActiveConversationManager = manager

    @Provides
    fun provideAlarmManager(manager: AlarmManagerImpl): AlarmManager = manager

    @Provides
    fun provideAnalyticsManager(manager: AnalyticsManagerImpl): AnalyticsManager = manager

    @Provides
    fun blockingClient(manager: BlockingManager): BlockingClient = manager

    @Provides
    fun changelogManager(manager: ChangelogManagerImpl): ChangelogManager = manager

    @Provides
    fun provideKeyManager(manager: KeyManagerImpl): KeyManager = manager

    @Provides
    fun provideNotificationsManager(manager: NotificationManagerImpl): NotificationManager = manager

    @Provides
    fun providePermissionsManager(manager: PermissionManagerImpl): PermissionManager = manager

    @Provides
    fun provideRatingManager(manager: RatingManagerImpl): RatingManager = manager

    @Provides
    fun provideShortcutManager(manager: ShortcutManagerImpl): ShortcutManager = manager

    @Provides
    fun provideReferralManager(manager: ReferralManagerImpl): ReferralManager = manager

// Mapper

    @Provides
    fun provideCursorToContact(mapper: CursorToContactImpl): CursorToContact = mapper

    @Provides
    fun provideCursorToContactGroup(mapper: CursorToContactGroupImpl): CursorToContactGroup = mapper

    @Provides
    fun provideCursorToContactGroupMember(mapper: CursorToContactGroupMemberImpl): CursorToContactGroupMember = mapper

    @Provides
    fun provideCursorToConversation(mapper: CursorToConversationImpl): CursorToConversation = mapper

    @Provides
    fun provideCursorToMessage(mapper: CursorToMessageImpl): CursorToMessage = mapper

    @Provides
    fun provideCursorToRecipient(mapper: CursorToRecipientImpl): CursorToRecipient = mapper

    // Repository

    @Provides
    fun provideBlockingRepository(repository: BlockingRepositoryImpl): BlockingRepository = repository

    @Provides
    fun provideContactRepository(repository: ContactRepositoryImpl): ContactRepository = repository

    @Provides
    fun provideConversationRepository(repository: ConversationRepositoryImpl): ConversationRepository = repository

    @Provides
    fun provideMessageRepository(repository: MessageRepositoryImpl): MessageRepository = repository

    @Provides
    fun provideSyncRepository(repository: SyncRepositoryImpl): SyncRepository = repository

    @Provides
    @Singleton
    fun providesQRCodeWriter(): QRCodeWriter = QRCodeWriter()

}