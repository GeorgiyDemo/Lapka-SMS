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
package com.moez.QKSMS.feature.backup

import android.Manifest
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkThemedActivity
import dagger.android.AndroidInjection
import android.widget.FrameLayout


class BackupActivity : QkThemedActivity() {

    private lateinit var router: Router

    val storagePermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.container_activity)

        router = Conductor.attachRouter(this, findViewById<FrameLayout>(R.id.container), savedInstanceState)
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(BackupController()))
        }

        onBackPressedDispatcher.addCallback(this) {
            if (!router.handleBack()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

}