package com.moez.QKSMS.interactor

import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.moez.QKSMS.util.NightModeManager
import com.moez.QKSMS.util.Preferences
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Tests for the MigratePreferences interactor.
 *
 * Preferences.theme() uses Kotlin default parameters that access the private rxPrefs field.
 * We inject rxPrefs into the mock via reflection and stub theme(Long, Int) instead of theme()
 * so that Mockito doesn't encounter nested mock interactions during stubbing.
 */
class MigratePreferencesTest {

    private lateinit var nightModeManager: NightModeManager
    private lateinit var prefs: Preferences
    private lateinit var rxPrefs: RxSharedPreferences
    private lateinit var migratePreferences: MigratePreferences

    private lateinit var welcomeSeenPref: Preference<Boolean>
    private lateinit var themePref: Preference<Int>
    private lateinit var themeStringPref: Preference<String>
    private lateinit var backgroundPref: Preference<String>
    private lateinit var autoNightPref: Preference<Boolean>
    private lateinit var blackPref: Preference<Boolean>
    private lateinit var deliveryPref: Preference<Boolean>
    private lateinit var deliveryOldPref: Preference<Boolean>
    private lateinit var qkreplyPref: Preference<Boolean>
    private lateinit var qkreplyOldPref: Preference<Boolean>
    private lateinit var qkreplyTapDismissPref: Preference<Boolean>
    private lateinit var qkreplyTapDismissOldPref: Preference<Boolean>
    private lateinit var textSizePref: Preference<Int>
    private lateinit var textSizeStringPref: Preference<String>
    private lateinit var unicodePref: Preference<Boolean>
    private lateinit var unicodeOldPref: Preference<Boolean>

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setUp() {
        nightModeManager = mock(NightModeManager::class.java)
        rxPrefs = mock(RxSharedPreferences::class.java)

        // Create Preferences mock and inject rxPrefs for default parameter resolution
        prefs = mock(Preferences::class.java)
        try {
            val field = Preferences::class.java.getDeclaredField("rxPrefs")
            field.isAccessible = true
            field.set(prefs, rxPrefs)
        } catch (_: Exception) {
        }

        welcomeSeenPref = mock(Preference::class.java) as Preference<Boolean>

        // Theme: rxPrefs.getInteger("theme", 0) is called by theme()'s default param
        themePref = mock(Preference::class.java) as Preference<Int>
        themeStringPref = mock(Preference::class.java) as Preference<String>
        val themeDefaultPref = mock(Preference::class.java) as Preference<Int>
        doReturn(themeDefaultPref).`when`(rxPrefs).getInteger("theme", Preferences.THEME_DEFAULT_DYNAMIC)
        doReturn(0xFF0088FF.toInt()).`when`(themeDefaultPref).get()
        // Stub the 2-arg version which is what gets called after default param resolution
        doReturn(themePref).`when`(prefs).theme(anyLong(), anyInt())
        doReturn(0xFF0088FF.toInt()).`when`(themePref).get()

        backgroundPref = mock(Preference::class.java) as Preference<String>
        autoNightPref = mock(Preference::class.java) as Preference<Boolean>

        blackPref = mock(Preference::class.java) as Preference<Boolean>
        doReturn(blackPref).`when`(prefs).black

        deliveryPref = mock(Preference::class.java) as Preference<Boolean>
        deliveryOldPref = mock(Preference::class.java) as Preference<Boolean>
        doReturn(deliveryPref).`when`(prefs).delivery
        doReturn(false).`when`(deliveryPref).get()

        qkreplyPref = mock(Preference::class.java) as Preference<Boolean>
        qkreplyOldPref = mock(Preference::class.java) as Preference<Boolean>
        doReturn(qkreplyPref).`when`(prefs).qkreply
        doReturn(false).`when`(qkreplyPref).get()

        qkreplyTapDismissPref = mock(Preference::class.java) as Preference<Boolean>
        qkreplyTapDismissOldPref = mock(Preference::class.java) as Preference<Boolean>
        doReturn(qkreplyTapDismissPref).`when`(prefs).qkreplyTapDismiss
        doReturn(true).`when`(qkreplyTapDismissPref).get()

        textSizePref = mock(Preference::class.java) as Preference<Int>
        textSizeStringPref = mock(Preference::class.java) as Preference<String>
        doReturn(textSizePref).`when`(prefs).textSize
        doReturn(1).`when`(textSizePref).get()

        unicodePref = mock(Preference::class.java) as Preference<Boolean>
        unicodeOldPref = mock(Preference::class.java) as Preference<Boolean>
        doReturn(unicodePref).`when`(prefs).unicode
        doReturn(false).`when`(unicodePref).get()

        migratePreferences = MigratePreferences(nightModeManager, prefs, rxPrefs)
    }

    private fun setupWelcomeSeen(seen: Boolean) {
        doReturn(welcomeSeenPref).`when`(rxPrefs).getBoolean("pref_key_welcome_seen", false)
        doReturn(seen).`when`(welcomeSeenPref).get()
    }

    private fun setupOldPreferences(
        theme: String = "0",
        background: String = "light",
        autoNight: Boolean = false,
        delivery: Boolean = false,
        qkreply: Boolean = false,
        qkreplyDismiss: Boolean = true,
        fontSize: String = "1",
        unicode: Boolean = false
    ) {
        doReturn(themeStringPref).`when`(rxPrefs).getString(eq("pref_key_theme"), anyString())
        doReturn(theme).`when`(themeStringPref).get()

        doReturn(backgroundPref).`when`(rxPrefs).getString("pref_key_background", "light")
        doReturn(background).`when`(backgroundPref).get()

        doReturn(autoNightPref).`when`(rxPrefs).getBoolean("pref_key_night_auto", false)
        doReturn(autoNight).`when`(autoNightPref).get()

        doReturn(deliveryOldPref).`when`(rxPrefs).getBoolean(eq("pref_key_delivery"), anyBoolean())
        doReturn(delivery).`when`(deliveryOldPref).get()

        doReturn(qkreplyOldPref).`when`(rxPrefs).getBoolean(eq("pref_key_quickreply_enabled"), anyBoolean())
        doReturn(qkreply).`when`(qkreplyOldPref).get()

        doReturn(qkreplyTapDismissOldPref).`when`(rxPrefs).getBoolean(eq("pref_key_quickreply_dismiss"), anyBoolean())
        doReturn(qkreplyDismiss).`when`(qkreplyTapDismissOldPref).get()

        doReturn(textSizeStringPref).`when`(rxPrefs).getString(eq("pref_key_font_size"), anyString())
        doReturn(fontSize).`when`(textSizeStringPref).get()

        doReturn(unicodeOldPref).`when`(rxPrefs).getBoolean(eq("pref_key_strip_unicode"), anyBoolean())
        doReturn(unicode).`when`(unicodeOldPref).get()
    }

    @Test
    fun `does not migrate when welcome not seen`() {
        setupWelcomeSeen(false)

        val subscriber = TestSubscriber<Any>()
        migratePreferences.buildObservable(Unit).subscribe(subscriber)
        subscriber.awaitTerminalEvent()

        subscriber.assertComplete()
        subscriber.assertNoErrors()
        verify(themePref, never()).set(anyInt())
    }

    @Test
    fun `migrates theme when welcome was seen`() {
        setupWelcomeSeen(true)
        setupOldPreferences(theme = "12345")

        val subscriber = TestSubscriber<Any>()
        migratePreferences.buildObservable(Unit).subscribe(subscriber)
        subscriber.awaitTerminalEvent()

        subscriber.assertComplete()
        verify(themePref).set(12345)
    }

    @Test
    fun `sets night mode auto when autoNight is true`() {
        setupWelcomeSeen(true)
        setupOldPreferences(autoNight = true)

        val subscriber = TestSubscriber<Any>()
        migratePreferences.buildObservable(Unit).subscribe(subscriber)
        subscriber.awaitTerminalEvent()

        verify(nightModeManager).updateNightMode(Preferences.NIGHT_MODE_AUTO)
    }

    @Test
    fun `sets night mode off when background is light`() {
        setupWelcomeSeen(true)
        setupOldPreferences(background = "light", autoNight = false)

        val subscriber = TestSubscriber<Any>()
        migratePreferences.buildObservable(Unit).subscribe(subscriber)
        subscriber.awaitTerminalEvent()

        verify(nightModeManager).updateNightMode(Preferences.NIGHT_MODE_OFF)
    }

    @Test
    fun `sets night mode on when background is grey`() {
        setupWelcomeSeen(true)
        setupOldPreferences(background = "grey", autoNight = false)

        val subscriber = TestSubscriber<Any>()
        migratePreferences.buildObservable(Unit).subscribe(subscriber)
        subscriber.awaitTerminalEvent()

        verify(nightModeManager).updateNightMode(Preferences.NIGHT_MODE_ON)
    }

    @Test
    fun `sets night mode on and black when background is black`() {
        setupWelcomeSeen(true)
        setupOldPreferences(background = "black", autoNight = false)

        val subscriber = TestSubscriber<Any>()
        migratePreferences.buildObservable(Unit).subscribe(subscriber)
        subscriber.awaitTerminalEvent()

        verify(nightModeManager).updateNightMode(Preferences.NIGHT_MODE_ON)
        verify(blackPref).set(true)
    }

    @Test
    fun `migrates delivery preference`() {
        setupWelcomeSeen(true)
        setupOldPreferences(delivery = true)

        val subscriber = TestSubscriber<Any>()
        migratePreferences.buildObservable(Unit).subscribe(subscriber)
        subscriber.awaitTerminalEvent()

        verify(deliveryPref).set(true)
    }

    @Test
    fun `migrates unicode preference`() {
        setupWelcomeSeen(true)
        setupOldPreferences(unicode = true)

        val subscriber = TestSubscriber<Any>()
        migratePreferences.buildObservable(Unit).subscribe(subscriber)
        subscriber.awaitTerminalEvent()

        verify(unicodePref).set(true)
    }

    @Test
    fun `deletes welcome seen flag after migration`() {
        setupWelcomeSeen(true)
        setupOldPreferences()

        val subscriber = TestSubscriber<Any>()
        migratePreferences.buildObservable(Unit).subscribe(subscriber)
        subscriber.awaitTerminalEvent()

        verify(welcomeSeenPref).delete()
    }

    @Test
    fun `migrates text size preference`() {
        setupWelcomeSeen(true)
        setupOldPreferences(fontSize = "2")

        val subscriber = TestSubscriber<Any>()
        migratePreferences.buildObservable(Unit).subscribe(subscriber)
        subscriber.awaitTerminalEvent()

        verify(textSizePref).set(2)
    }
}
