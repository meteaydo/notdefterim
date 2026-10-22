package com.notdefterim.app.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdefterim.app.core.security.AuthState
import com.notdefterim.app.R
import com.notdefterim.app.core.security.BiometricSupport
import com.notdefterim.app.core.security.PinCheck
import com.notdefterim.app.core.security.SecurityManager
import com.notdefterim.app.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val securityManager: SecurityManager,
  private val appPreferences: AppPreferences
) : ViewModel() {

  val authState: StateFlow<AuthState> = securityManager.authState
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = AuthState.Unauthenticated
    )

  val appPin: StateFlow<String?> = appPreferences.appPin
  val appPinHint: StateFlow<String?> = appPreferences.appPinHint
  val appPinScope: StateFlow<Int> = appPreferences.appPinScope

  fun setAppPin(pin: String?, hint: String? = null) {
    appPreferences.setAppPin(pin, hint)
  }

  fun setAppPinScope(scope: Int) {
    appPreferences.setAppPinScope(scope)
  }

  /** Girilen PIN'i kayıtlı hash ile doğrular (deneme sınırı dahil, bkz. [AppPreferences.checkPin]). */
  fun checkPin(pin: String): PinCheck = appPreferences.checkPin(pin)

  var isSessionUnlocked: Boolean
    get() = securityManager.isSessionUnlocked
    private set(value) {
      securityManager.isSessionUnlocked = value
    }

  fun lockSession() {
    isSessionUnlocked = false
  }

  data class PinPromptState(
    val title: String,
    val subtitle: String,
    val onSuccess: () -> Unit,
    val onError: (String) -> Unit
  )

  private val _pinPromptState = kotlinx.coroutines.flow.MutableStateFlow<PinPromptState?>(null)
  val pinPromptState: StateFlow<PinPromptState?> = _pinPromptState.asStateFlow()

  // Giriş penceresi her zaman cihaz kilidine de izin verir; kontrol de aynı yöntemle yapılmalı.
  // Yoksa uygulama içi PIN kuruluyken parmak izi olmayan cihazda "PIN kurulu değil" yazıyordu.
  fun checkBiometricSupport(): BiometricSupport =
    securityManager.checkBiometricSupport(useDeviceCredential = true)

  /**
   * BiometricPrompt FragmentActivity gerektirir.
   * Composable'dan activity referansı ile çağrılır.
   */
  fun authenticate(activity: androidx.fragment.app.FragmentActivity) {
    securityManager.authenticate(activity, useDeviceCredential = true)
  }

  /** Uygulama arka plana gittiğinde kimlik doğrulamayı sıfırla. */
  fun onAppBackground() {
    securityManager.resetAuthentication()
  }

  fun authenticateAction(
    activity: androidx.fragment.app.FragmentActivity,
    title: String,
    subtitle: String,
    targetScope: Int = 0,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    if (isSessionUnlocked) {
      onSuccess()
      return
    }

    val isPinApplicable = appPin.value != null && (appPinScope.value == 0 || appPinScope.value == targetScope)
    
    if (isPinApplicable) {
      _pinPromptState.value = PinPromptState(title, subtitle, onSuccess, onError)
    } else {
      // Eğer uygulama içi PIN ayarlanmamışsa, kilitli içerik (notlar/parolalar)
      // biyometrik doğrulama sormadan doğrudan açılsın.
      onSuccess()
    }
  }

  fun dismissPinPrompt() {
    _pinPromptState.value?.onError?.invoke(context.getString(R.string.user_cancelled))
    _pinPromptState.value = null
  }

  fun verifyActionPin(pin: String, keepUnlocked: Boolean = false): PinCheck {
    val prompt = _pinPromptState.value ?: return PinCheck.Wrong
    // Ham PIN ile karşılaştırma YAPILMAZ; PBKDF2 hash doğrulaması kullanılır
    val result = appPreferences.checkPin(pin)
    if (result == PinCheck.Correct) {
      if (keepUnlocked) {
        isSessionUnlocked = true
      }
      prompt.onSuccess()
      _pinPromptState.value = null
    }
    return result
  }
}
