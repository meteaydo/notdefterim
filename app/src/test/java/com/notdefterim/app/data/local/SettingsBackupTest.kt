package com.notdefterim.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.notdefterim.app.core.security.PinCheck
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Ayarların yerel yedeğe yazılıp geri yüklenmesi (K7). */
@RunWith(RobolectricTestRunner::class)
class SettingsBackupTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    clearAll()
  }

  private fun clearAll() {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun ayarlar_yedekten_aynen_geri_gelir() {
    val app = AppPreferences(context).apply {
      setAutoLockTimeout(600_000L)
      setPasswordReminderPeriod(7_776_000_000L)
      setStartupBehavior(2)
      setAppPin("1234", "ipucu")
      setAppPinScope(1)
    }
    val theme = ThemePreferences(context).apply { toggleTheme(currentSystemDark = false) }
    val appJson = JSONObject(app.backupSettings().toString())
    val themeJson = JSONObject(theme.backupSettings().toString())

    clearAll()
    AppPreferences(context).restoreSettings(appJson)
    ThemePreferences(context).restoreSettings(themeJson)

    val restored = AppPreferences(context)
    assertEquals(600_000L, restored.autoLockTimeout.value)
    assertEquals(7_776_000_000L, restored.passwordReminderPeriod.value)
    assertEquals(2, restored.startupBehavior.value)
    assertEquals(1, restored.appPinScope.value)
    assertEquals("ipucu", restored.appPinHint.value)
    assertEquals(PinCheck.Correct, restored.checkPin("1234"))
    assertEquals(true, ThemePreferences(context).isDarkTheme.value)
  }

  @Test
  fun yedekte_pin_yoksa_mevcut_pin_kalkar() {
    val backupWithoutPin = JSONObject(AppPreferences(context).backupSettings().toString())
    AppPreferences(context).setAppPin("1234", "ipucu")

    AppPreferences(context).restoreSettings(backupWithoutPin)

    assertNull(AppPreferences(context).appPinHash.value)
  }

  @Test
  fun tema_secilmemisse_sistem_temasina_doner() {
    val backupWithoutTheme = JSONObject(ThemePreferences(context).backupSettings().toString())
    ThemePreferences(context).toggleTheme(currentSystemDark = false)

    ThemePreferences(context).restoreSettings(backupWithoutTheme)

    assertNull(ThemePreferences(context).isDarkTheme.value)
  }
}
