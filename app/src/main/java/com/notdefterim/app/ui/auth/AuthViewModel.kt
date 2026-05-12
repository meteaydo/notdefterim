package com.notdefterim.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdefterim.app.core.security.AuthState
import com.notdefterim.app.core.security.BiometricSupport
import com.notdefterim.app.core.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
  private val securityManager: SecurityManager
) : ViewModel() {

  val authState: StateFlow<AuthState> = securityManager.authState
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = AuthState.Unauthenticated
    )

  fun checkBiometricSupport(): BiometricSupport =
    securityManager.checkBiometricSupport()

  /**
   * BiometricPrompt FragmentActivity gerektirir.
   * Composable'dan activity referansı ile çağrılır.
   */
  fun authenticate(activity: androidx.fragment.app.FragmentActivity) {
    securityManager.authenticate(activity)
  }

  /** Uygulama arka plana gittiğinde kimlik doğrulamayı sıfırla. */
  fun onAppBackground() {
    securityManager.resetAuthentication()
  }

  fun authenticateAction(
    activity: androidx.fragment.app.FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    securityManager.authenticateAction(activity, title, subtitle, onSuccess, onError)
  }
}
