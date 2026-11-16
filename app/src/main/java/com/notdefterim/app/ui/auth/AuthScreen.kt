package com.notdefterim.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.notdefterim.app.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.notdefterim.app.core.security.AuthState
import com.notdefterim.app.core.security.BiometricSupport

/**
 * Biyometrik kimlik doğrulama ekranı.
 * Uygulama her açıldığında veya ön plana geldiğinde gösterilir.
 */
@Composable
fun AuthScreen(
  authState: AuthState,
  biometricSupport: BiometricSupport,
  onAuthenticate: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Ekran açıldığında ve uygulama öne geldiğinde otomatik kimlik doğrulama başlat.
  // Ekran kapalıyken açılışta pencere başlayamazsa durum Unauthenticated'a döner; ekran açılınca yeniden denenir.
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    if (biometricSupport is BiometricSupport.Available && authState is AuthState.Unauthenticated) {
      onAuthenticate()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.background,
            // Saydam bırakılmaz: ekran arkasındaki zemine bağlı kalmasın (karanlık temada okunurluk, K4)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
              .compositeOver(MaterialTheme.colorScheme.background),
            MaterialTheme.colorScheme.background
          )
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(32.dp)
    ) {
      Text(
        text = "NotDefterim",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = when (authState) {
          is AuthState.Unauthenticated -> stringResource(R.string.auth_prompt)
          is AuthState.Authenticating -> stringResource(R.string.authenticating)
          is AuthState.Authenticated -> stringResource(R.string.auth_success)
          is AuthState.Error -> authState.message
        },
        style = MaterialTheme.typography.bodyLarge,
        color = if (authState is AuthState.Error) {
          MaterialTheme.colorScheme.error
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = TextAlign.Center
      )

      // Hata, biyometrik donanım yoksa ya da doğrulama penceresi açılamadıysa tekrar dene butonu göster
      if (authState !is AuthState.Authenticated) {
        Spacer(modifier = Modifier.height(40.dp))

        // İçinde parmak izi olan yuvarlak düğme; tek parmak izi bu, üstte ayrıca ikon yok
        FilledIconButton(
          onClick = onAuthenticate,
          shape = CircleShape,
          modifier = Modifier.size(88.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Fingerprint,
            contentDescription = stringResource(R.string.tap_to_unlock),
            modifier = Modifier.size(48.dp)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = stringResource(R.string.tap_to_unlock),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (biometricSupport is BiometricSupport.NoneEnrolled) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = stringResource(R.string.no_biometric_enrolled),
          style = MaterialTheme.typography.bodyLarge,
          color = androidx.compose.ui.graphics.Color.Black,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}
