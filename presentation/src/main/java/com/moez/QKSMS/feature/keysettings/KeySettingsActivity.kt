package com.moez.QKSMS.feature.keysettings

import android.content.Intent
import android.os.Bundle
import android.view.Menu
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

class KeySettingsActivity : QkThemedActivity() {

    private lateinit var router: Router

    var onQrResult: ((Intent?) -> Unit)? = null

    val qrScanLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onQrResult?.invoke(result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.container_activity)

        router = Conductor.attachRouter(this, findViewById<FrameLayout>(R.id.container), savedInstanceState)
        if (!router.hasRootController()) {
            val threadId = intent.extras?.getLong("threadId") ?: -1L
            router.setRoot(RouterTransaction.with(KeySettingsController(threadId)))
        }

        onBackPressedDispatcher.addCallback(this) {
            if (!router.handleBack()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.key_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

}
