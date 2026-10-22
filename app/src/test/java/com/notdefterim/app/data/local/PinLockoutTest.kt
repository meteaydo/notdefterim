package com.notdefterim.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.notdefterim.app.core.security.PinCheck
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Uygulama içi PIN deneme sınırı (G7). */
@RunWith(RobolectricTestRunner::class)
class PinLockoutTest {

  private lateinit var prefs: AppPreferences

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    prefs = AppPreferences(context)
    prefs.setAppPin("1234", "ipucu")
  }

  @Test
  fun dogru_pin_kabul_edilir() {
    assertEquals(PinCheck.Correct, prefs.checkPin("1234"))
  }

  @Test
  fun bes_hatadan_sonra_bekleme_baslar_ve_dogru_pin_de_reddedilir() {
    repeat(4) { assertEquals(PinCheck.Wrong, prefs.checkPin("0000")) }
    assertEquals(PinCheck.LockedOut(30), prefs.checkPin("0000"))
    assertEquals(PinCheck.LockedOut(30), prefs.checkPin("1234"))
  }

  @Test
  fun dogru_pin_sayaci_sifirlar() {
    repeat(4) { prefs.checkPin("0000") }
    assertEquals(PinCheck.Correct, prefs.checkPin("1234"))
    repeat(4) { assertEquals(PinCheck.Wrong, prefs.checkPin("0000")) }
  }

  @Test
  fun yeni_pin_kilidi_kaldirir() {
    repeat(5) { prefs.checkPin("0000") }
    prefs.setAppPin("5678", "ipucu")
    assertEquals(PinCheck.Correct, prefs.checkPin("5678"))
  }

  @Test
  fun kilit_uygulama_yeniden_acilinca_surer() {
    repeat(5) { prefs.checkPin("0000") }
    val reopened = AppPreferences(ApplicationProvider.getApplicationContext())
    assertEquals(PinCheck.LockedOut(30), reopened.checkPin("1234"))
  }
}
