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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notdefterim.app.R

/**
 * Şifreleme anahtarı okunamadığında çökme yerine gösterilen ekran (H5).
 * Önce yeniden denemeyi önerir; sıfırlama onay ister çünkü bu cihazdaki veriler silinir.
 */
@Composable
fun KeyErrorScreen(
  onRetry: () -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showResetConfirm by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(32.dp)
    ) {
      Icon(
        imageVector = Icons.Rounded.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(64.dp)
      )
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = stringResource(R.string.key_error_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = stringResource(R.string.key_error_desc),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(32.dp))
      Button(onClick = onRetry) {
        Text(stringResource(R.string.key_error_retry))
      }
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedButton(onClick = { showResetConfirm = true }) {
        Text(stringResource(R.string.key_error_reset), color = MaterialTheme.colorScheme.error)
      }
    }
  }

  if (showResetConfirm) {
    AlertDialog(
      onDismissRequest = { showResetConfirm = false },
      title = { Text(stringResource(R.string.key_error_reset_confirm_title)) },
      text = { Text(stringResource(R.string.key_error_reset_confirm_desc)) },
      confirmButton = {
        TextButton(onClick = onReset) {
          Text(stringResource(R.string.key_error_reset), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirm = false }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }
}
