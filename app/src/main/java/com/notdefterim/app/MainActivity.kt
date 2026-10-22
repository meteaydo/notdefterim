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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
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
import com.notdefterim.app.core.security.SecurityManager
import com.notdefterim.app.core.security.PinCheck
import com.notdefterim.app.core.security.EncryptionHelper
import com.notdefterim.app.data.local.NoteDatabase
import com.notdefterim.app.ui.auth.KeyErrorScreen
import com.notdefterim.app.data.local.AppPreferences
import com.notdefterim.app.service.ReminderRestorer
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

  @Inject
  lateinit var appPreferences: AppPreferences

  @Inject
  lateinit var securityManager: SecurityManager

  @Inject
  lateinit var reminderRestorer: dagger.Lazy<ReminderRestorer>

  @Inject
  lateinit var encryptionHelper: EncryptionHelper

  private var backgroundTime: Long = 0L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Anahtar okunamıyorsa veritabanına dokunmadan açıklama ekranı göster; yoksa her açılışta çöker (H5)
    if (!encryptionHelper.isKeyUsable(databaseExists = getDatabasePath(NoteDatabase.DATABASE_NAME).exists())) {
      setContent {
        NotDefterimTheme {
          KeyErrorScreen(onRetry = { recreate() }, onReset = { resetAfterKeyLoss() })
        }
      }
      return
    }

    if (savedInstanceState == null) {
      // Yeniden başlatma bildirimi gelmediyse (ör. otomatik başlatma izni yok) hatırlatıcıları burada kur
      ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
        reminderRestorer.get().restore()
      }
    }

    setContent {
      val initialNoteId = remember { intent.getLongExtra("NOTE_ID", -1L).takeIf { it != -1L } }
      val initialShortcutAction = remember { intent.action }
      val systemDark = isSystemInDarkTheme()
      val userDarkTheme by themePreferences.isDarkTheme.collectAsStateWithLifecycle()
      val darkTheme = userDarkTheme ?: systemDark

      NotDefterimTheme(darkTheme = darkTheme) {
        val authViewModel: AuthViewModel = hiltViewModel()
        val authState by authViewModel.authState.collectAsStateWithLifecycle()
        val biometricSupport = authViewModel.checkBiometricSupport()
        val pinPromptState by authViewModel.pinPromptState.collectAsStateWithLifecycle()

        val gradientBrush = if (darkTheme) {
          androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(androidx.compose.ui.graphics.Color(0xFF232528), androidx.compose.ui.graphics.Color(0xFF141517))
          )
        } else {
          androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(androidx.compose.ui.graphics.Color(0xFFFAFAFA), androidx.compose.ui.graphics.Color(0xFFE2E2E2))
          )
        }

        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(gradientBrush)) {


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
              appPreferences = appPreferences,
              systemDark = systemDark,
              initialNoteId = initialNoteId,
              initialShortcutAction = initialShortcutAction,
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

        if (pinPromptState != null) {
          var inputPin by remember { mutableStateOf("") }
          var isError by remember { mutableStateOf(false) }
          var lockoutSeconds by remember { mutableStateOf(0L) }

          AlertDialog(
            onDismissRequest = { authViewModel.dismissPinPrompt() },
            title = { Text(pinPromptState!!.title) },
            text = {
              Column {
                Text(pinPromptState!!.subtitle)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                  value = inputPin,
                  onValueChange = { inputPin = it; isError = false },
                  label = { Text(stringResource(R.string.app_pin_label)) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                  visualTransformation = PasswordVisualTransformation(),
                  singleLine = true,
                  isError = isError
                )
                if (isError) {
                  Text(if (lockoutSeconds > 0) stringResource(R.string.pin_locked_out, lockoutSeconds) else stringResource(R.string.wrong_pin), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
              }
            },
            confirmButton = {
              Button(onClick = {
                val result = authViewModel.verifyActionPin(inputPin)
                if (result != PinCheck.Correct) {
                  isError = true
                  lockoutSeconds = (result as? PinCheck.LockedOut)?.seconds ?: 0L
                  inputPin = ""
                }
              }) {
                Text(stringResource(R.string.unlock))
              }
            },
            dismissButton = {
              TextButton(onClick = { authViewModel.dismissPinPrompt() }) {
                Text(stringResource(R.string.cancel))
              }
            }
          )
        }

        } // Box End
      }
    }
  }

  /**
   * Anahtar kaybından sonra uygulamayı ilk kurulum haline getirir. Bu anahtar olmadan açılamayan
   * veritabanı silinir; notlar Ayarlar'dan yedekten geri yüklenir.
   */
  private fun resetAfterKeyLoss() {
    encryptionHelper.resetAfterKeyLoss()
    deleteDatabase(NoteDatabase.DATABASE_NAME)
    // Tekiller bozuk anahtarla kurulmuş olabilir; süreci baştan başlat
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
      intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
      startActivity(intent)
      Runtime.getRuntime().exit(0)
    }
  }

  override fun onStop() {
    super.onStop()
    backgroundTime = System.currentTimeMillis()
  }

  override fun onStart() {
    super.onStart()
    if (backgroundTime > 0) {
      val timeout = appPreferences.autoLockTimeout.value
      if (timeout != -1L) {
        val elapsed = System.currentTimeMillis() - backgroundTime
        if (elapsed >= timeout) {
          securityManager.resetAuthentication()
        }
      }
      backgroundTime = 0L
    }
  }
}
