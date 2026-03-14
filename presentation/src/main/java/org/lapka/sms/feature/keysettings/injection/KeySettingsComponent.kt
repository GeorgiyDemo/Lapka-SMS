package org.lapka.sms.feature.keysettings.injection

import org.lapka.sms.feature.keysettings.KeySettingsController
import org.lapka.sms.injection.scope.ControllerScope
import dagger.Subcomponent

@ControllerScope
@Subcomponent(modules = [KeySettingsModule::class])
interface KeySettingsComponent {

    fun inject(controller: KeySettingsController)

    @Subcomponent.Builder
    interface Builder {
        fun keySettingsModule(module: KeySettingsModule): Builder
        fun build(): KeySettingsComponent
    }

}