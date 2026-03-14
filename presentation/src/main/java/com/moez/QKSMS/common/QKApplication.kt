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
package com.moez.QKSMS.common

import android.app.Application
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.moez.QKSMS.R
import com.moez.QKSMS.crypto.ConversationKeyStore
import com.moez.QKSMS.injection.AppComponentManager
import com.moez.QKSMS.injection.appComponent
import com.moez.QKSMS.manager.AnalyticsManager
import com.moez.QKSMS.manager.ReferralManager
import com.moez.QKSMS.migration.QkMigration
import com.moez.QKSMS.migration.QkRealmMigration
import com.moez.QKSMS.util.NightModeManager
import com.uber.rxdogtag.RxDogTag
import com.uber.rxdogtag.autodispose.AutoDisposeConfigurer
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import com.moez.QKSMS.model.Conversation
import java.io.File
import io.realm.Realm
import org.lapka.sms.NonceCache
import io.realm.RealmConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

class QKApplication : Application(), HasAndroidInjector {

    /**
     * Inject these so that they are forced to initialize
     */
    @Suppress("unused")
    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Suppress("unused")
    @Inject
    lateinit var qkMigration: QkMigration

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var nightModeManager: NightModeManager

    @Inject
    lateinit var realmMigration: QkRealmMigration

    @Inject
    lateinit var referralManager: ReferralManager

    override fun onCreate() {
        super.onCreate()

        AppComponentManager.init(this)
        appComponent.inject(this)

        Realm.init(this)
        val realmKey = com.moez.QKSMS.common.util.RealmKeyProvider.getOrCreateRealmKey(this)
        val realmConfigBuilder = RealmConfiguration.Builder()
            .compactOnLaunch()
            .migration(realmMigration)
            .schemaVersion(QkRealmMigration.SchemaVersion)

        if (realmKey != null) {
            realmConfigBuilder.encryptionKey(realmKey)
        }
        Realm.setDefaultConfiguration(realmConfigBuilder.build())

        qkMigration.performMigration()
        migrateConversationKeys()
        loadNonceCache()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                saveNonceCache()
            }
        })

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            referralManager.trackReferrer()
        }

        nightModeManager.updateCurrentTheme()

        EmojiCompat.init(BundledEmojiCompatConfig(this))

        RxDogTag.builder()
            .configureWith(AutoDisposeConfigurer::configure)
            .install()
    }

    private val nonceCacheFile: File get() = File(filesDir, "nonce_cache.bin")

    private fun loadNonceCache() {
        try {
            val file = nonceCacheFile
            if (file.exists()) {
                file.inputStream().use { NonceCache.getDefault().loadFrom(it) }
            }
        } catch (_: Exception) {
        }
    }

    private fun saveNonceCache() {
        try {
            nonceCacheFile.outputStream().use { NonceCache.getDefault().saveTo(it) }
        } catch (_: Exception) {
        }
    }

    private fun migrateConversationKeys() {
        try {
            Realm.getDefaultInstance().use { realm ->
                val conversations = realm.where(Conversation::class.java)
                    .isNotEmpty("encryptionKey")
                    .findAll()
                realm.executeTransaction {
                    for (conv in conversations) {
                        if (!ConversationKeyStore.isWrapped(conv.encryptionKey)) {
                            conv.encryptionKey = ConversationKeyStore.wrapKey(conv.encryptionKey)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Non-fatal: legacy keys still work via unwrapKeyBytes fallback
        }
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

}