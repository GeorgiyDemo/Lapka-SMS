package org.lapka.sms.feature.keysettings.injection

import org.lapka.sms.feature.keysettings.KeySettingsController
import org.lapka.sms.injection.scope.ControllerScope
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class KeySettingsModule(private val controller: KeySettingsController) {

    @Provides
    @ControllerScope
    @Named("keySettingsConversationThreadId")
    fun provideThreadId(): Long = controller.threadId

}