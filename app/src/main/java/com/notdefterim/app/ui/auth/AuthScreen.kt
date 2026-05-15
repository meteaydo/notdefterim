package com.notdefterim.app.ui.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.notdefterim.app.R
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.fragment.app.FragmentActivity
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
  // Kilid ikonu için nefes efekti animasyonu
  val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  // Ekran açıldığında otomatik kimlik doğrulama başlat
  LaunchedEffect(biometricSupport) {
    if (biometricSupport is BiometricSupport.Available) {
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
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
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
      // Animasyonlu kilit/parmak izi ikonu
      Box(
        modifier = Modifier
          .size(120.dp)
          .scale(pulseScale)
          .background(
            brush = Brush.radialGradient(
              colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
              )
            ),
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (authState is AuthState.Error) {
            Icons.Rounded.Lock
          } else {
            Icons.Rounded.Fingerprint
          },
          contentDescription = stringResource(R.string.auth_description),
          modifier = Modifier.size(64.dp),
          tint = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(32.dp))

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

      // Hata veya biyometrik donanım yoksa tekrar dene butonu göster
      if (authState is AuthState.Error || authState is AuthState.Unauthenticated) {
        Spacer(modifier = Modifier.height(40.dp))

        Button(
          onClick = onAuthenticate,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Icon(
            imageVector = Icons.Rounded.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(stringResource(R.string.authenticate))
        }
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
