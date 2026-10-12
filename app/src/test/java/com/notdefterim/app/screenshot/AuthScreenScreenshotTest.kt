package com.notdefterim.app.screenshot

import com.notdefterim.app.core.security.AuthState
import com.notdefterim.app.core.security.BiometricSupport
import com.notdefterim.app.ui.auth.AuthScreen
import org.junit.Test

class AuthScreenScreenshotTest : ScreenshotTest() {

  @Test
  fun girisBekleniyor() = snapshotScreen {
    AuthScreen(
      authState = AuthState.Unauthenticated,
      biometricSupport = BiometricSupport.Available,
      onAuthenticate = {}
    )
  }

  @Test
  fun girisBekleniyor_koyuTema() = snapshotScreen(darkTheme = true) {
    AuthScreen(
      authState = AuthState.Unauthenticated,
      biometricSupport = BiometricSupport.Available,
      onAuthenticate = {}
    )
  }

  @Test
  fun kimlikDogrulaniyor() = snapshotScreen {
    AuthScreen(
      authState = AuthState.Authenticating,
      biometricSupport = BiometricSupport.Available,
      onAuthenticate = {}
    )
  }

  @Test
  fun hata() = snapshotScreen {
    AuthScreen(
      authState = AuthState.Error("Kimlik doğrulama iptal edildi"),
      biometricSupport = BiometricSupport.Available,
      onAuthenticate = {}
    )
  }

  @Test
  fun ekranKilidiYok() = snapshotScreen {
    AuthScreen(
      authState = AuthState.Unauthenticated,
      biometricSupport = BiometricSupport.NoneEnrolled,
      onAuthenticate = {}
    )
  }
}
