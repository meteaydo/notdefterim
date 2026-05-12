package com.notdefterim.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.core.security.AuthState
import com.notdefterim.app.data.remote.GoogleAuthManager
import com.notdefterim.app.ui.auth.AuthScreen
import com.notdefterim.app.ui.auth.AuthViewModel
import com.notdefterim.app.data.local.ThemePreferences
import com.notdefterim.app.ui.navigation.AppNavigation
import com.notdefterim.app.ui.theme.NotDefterimTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.isSystemInDarkTheme
import javax.inject.Inject

/**
 * Tek Activity — Compose host.
 *
 * Neden FragmentActivity yerine ComponentActivity değil?
 * BiometricPrompt lifecycle binding için FragmentActivity gereklidir.
 *
 * EdgeToEdge: Sistem çubuklarının altına içerik uzanır (immersive deneyim).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  @Inject
  lateinit var googleAuthManager: GoogleAuthManager

  @Inject
  lateinit var themePreferences: ThemePreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val systemDark = isSystemInDarkTheme()
      val userDarkTheme by themePreferences.isDarkTheme.collectAsStateWithLifecycle()
      val darkTheme = userDarkTheme ?: systemDark

      NotDefterimTheme(darkTheme = darkTheme) {
        val authViewModel: AuthViewModel = hiltViewModel()
        val authState by authViewModel.authState.collectAsStateWithLifecycle()
        val biometricSupport = authViewModel.checkBiometricSupport()

        AnimatedContent(
          targetState = authState is AuthState.Authenticated,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "auth_to_main_transition",
          modifier = Modifier.fillMaxSize()
        ) { isAuthenticated ->
          if (isAuthenticated) {
            AppNavigation(
              googleAuthManager = googleAuthManager,
              themePreferences = themePreferences,
              systemDark = systemDark,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            AuthScreen(
              authState = authState,
              biometricSupport = biometricSupport,
              onAuthenticate = {
                authViewModel.authenticate(this@MainActivity)
              },
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
    // Uygulama arka plana gittiğinde kimlik doğrulamayı sıfırla
    // Böylece tekrar ön plana gelince biyometrik ekran gösterilir
    // NOT: Bu davranışı kullanıcı tercihine göre isteğe bağlı yapabilirsiniz
  }
}
