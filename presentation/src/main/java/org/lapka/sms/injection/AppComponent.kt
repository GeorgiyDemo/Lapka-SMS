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

import org.lapka.sms.common.QKApplication
import org.lapka.sms.common.QkDialog
import org.lapka.sms.common.util.QkChooserTargetService
import org.lapka.sms.common.widget.AvatarView
import org.lapka.sms.common.widget.PagerTitleView
import org.lapka.sms.common.widget.PreferenceView
import org.lapka.sms.common.widget.QkEditText
import org.lapka.sms.common.widget.QkSwitch
import org.lapka.sms.common.widget.QkTextView
import org.lapka.sms.common.widget.RadioPreferenceView
import org.lapka.sms.feature.blocking.BlockingController
import org.lapka.sms.feature.blocking.manager.BlockingManagerController
import org.lapka.sms.feature.blocking.messages.BlockedMessagesController
import org.lapka.sms.feature.blocking.numbers.BlockedNumbersController
import org.lapka.sms.feature.compose.editing.DetailedChipView
import org.lapka.sms.feature.conversationinfo.injection.ConversationInfoComponent
import org.lapka.sms.feature.keysettings.KeySettingsController
import org.lapka.sms.feature.keysettings.injection.KeySettingsComponent
import org.lapka.sms.feature.settings.SettingsController
import org.lapka.sms.feature.settings.about.AboutController
import org.lapka.sms.feature.settings.swipe.SwipeActionsController
import org.lapka.sms.feature.themepicker.injection.ThemePickerComponent
import org.lapka.sms.injection.android.ActivityBuilderModule
import org.lapka.sms.injection.android.BroadcastReceiverBuilderModule
import org.lapka.sms.injection.android.ServiceBuilderModule
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AppModule::class,
        ActivityBuilderModule::class,
        BroadcastReceiverBuilderModule::class,
        ServiceBuilderModule::class]
)
interface AppComponent {

    fun conversationInfoBuilder(): ConversationInfoComponent.Builder
    fun keySettingsBuilder(): KeySettingsComponent.Builder
    fun themePickerBuilder(): ThemePickerComponent.Builder

    fun inject(application: QKApplication)

    fun inject(controller: AboutController)
    fun inject(controller: BlockedMessagesController)
    fun inject(controller: BlockedNumbersController)
    fun inject(controller: BlockingController)
    fun inject(controller: BlockingManagerController)
    fun inject(controller: SettingsController)
    fun inject(controller: SwipeActionsController)

    fun inject(dialog: QkDialog)

    /**
     * This can't use AndroidInjection, or else it will crash on pre-marshmallow devices
     */
    fun inject(service: QkChooserTargetService)

    fun inject(view: AvatarView)
    fun inject(view: DetailedChipView)
    fun inject(view: PagerTitleView)
    fun inject(view: PreferenceView)
    fun inject(view: RadioPreferenceView)
    fun inject(view: QkEditText)
    fun inject(view: QkSwitch)
    fun inject(view: QkTextView)

}
