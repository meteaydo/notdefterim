package com.notdefterim.app.core.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Biyometrik + DEVICE_CREDENTIAL (PIN/Pattern/Password) kimlik doğrulama yöneticisi.
 *
 * Neden BIOMETRIC_STRONG | DEVICE_CREDENTIAL kombinasyonu?
 * - Parmak izi/yüz tanıma varsa önce onu dener.
 * - Yoksa veya başarısız olursa otomatik olarak cihaz kilidine (PIN/Pattern) geçer.
 * - Kullanıcı ek bir PIN kurmak zorunda değil; cihaz kilidi yeterli.
 */
class SecurityManager(private val context: Context) {

  private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
  val authState: StateFlow<AuthState> = _authState.asStateFlow()

  /** Desteklenen kimlik doğrulama yöntemlerini kontrol eder. */
  fun checkBiometricSupport(): BiometricSupport {
    val biometricManager = BiometricManager.from(context)
    return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
      BiometricManager.BIOMETRIC_SUCCESS -> BiometricSupport.Available
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricSupport.NoHardware
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricSupport.HardwareUnavailable
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricSupport.NoneEnrolled
      else -> BiometricSupport.Unknown
    }
  }

  /**
   * Biyometrik giriş ekranını gösterir.
   * [activity] FragmentActivity gereklidir; BiometricPrompt lifecycle'a bağlıdır.
   */
  fun authenticate(activity: FragmentActivity) {
    _authState.value = AuthState.Authenticating

    val executor = ContextCompat.getMainExecutor(activity)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        _authState.value = AuthState.Authenticated
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        // Kullanıcı iptal ederse veya cihaz credential girişi başarısız olursa
        _authState.value = AuthState.Error(errString.toString())
      }

      override fun onAuthenticationFailed() {
        // Parmak izi eşleşmedi — sistem kendi retry mekanizmasını yönetir
        // State'i değiştirmiyoruz; BiometricPrompt yeniden deneme imkânı sunar
      }
    }

    val biometricPrompt = BiometricPrompt(activity, executor, callback)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle("NotDefterim'e Giriş")
      .setSubtitle("Notlarınıza erişmek için kimliğinizi doğrulayın")
      .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
      // DEVICE_CREDENTIAL kullanıldığında setNegativeButtonText çağrılamaz
      .build()

    biometricPrompt.authenticate(promptInfo)
  }

  /** Manuel olarak kimlik doğrulamasını sıfırla (uygulama arka plana geçince). */
  fun resetAuthentication() {
    _authState.value = AuthState.Unauthenticated
  }
}

/** Kimlik doğrulama durumu — UI bu sealed class'ı gözlemler. */
sealed class AuthState {
  object Unauthenticated : AuthState()
  object Authenticating : AuthState()
  object Authenticated : AuthState()
  data class Error(val message: String) : AuthState()
}

/** BiometricManager kontrol sonucu. */
sealed class BiometricSupport {
  object Available : BiometricSupport()
  object NoHardware : BiometricSupport()
  object HardwareUnavailable : BiometricSupport()
  object NoneEnrolled : BiometricSupport()
  object Unknown : BiometricSupport()
}
