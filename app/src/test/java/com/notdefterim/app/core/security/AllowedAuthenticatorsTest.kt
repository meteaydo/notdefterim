package com.notdefterim.app.core.security

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * androidx.biometric 1.1.0 (AuthenticatorUtils.isSupportedCombination):
 * BIOMETRIC_STRONG | DEVICE_CREDENTIAL yalnızca API 28–29'da reddedilir;
 * BIOMETRIC_WEAK | DEVICE_CREDENTIAL her sürümde kabul edilir.
 */
class AllowedAuthenticatorsTest {

  @Test
  fun android9_ve_10_zayif_biyometri_ve_cihaz_kilidi() {
    assertEquals(BIOMETRIC_WEAK or DEVICE_CREDENTIAL, SecurityManager.allowedAuthenticators(true, sdkInt = 28))
    assertEquals(BIOMETRIC_WEAK or DEVICE_CREDENTIAL, SecurityManager.allowedAuthenticators(true, sdkInt = 29))
  }

  @Test
  fun diger_surumler_guclu_biyometri_ve_cihaz_kilidi() {
    for (sdk in listOf(26, 27, 30, 35, 36)) {
      assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, SecurityManager.allowedAuthenticators(true, sdkInt = sdk))
    }
  }

  @Test
  fun cihaz_kilidi_istenmezse_yalnizca_guclu_biyometri() {
    assertEquals(BIOMETRIC_STRONG, SecurityManager.allowedAuthenticators(false, sdkInt = 29))
  }
}
