package com.notdefterim.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.ui.auth.AuthViewModel
import com.notdefterim.app.R

@Composable
fun ActionPinDialog(
  pinPromptState: AuthViewModel.PinPromptState,
  onDismiss: () -> Unit,
  onVerify: (String, Boolean) -> Boolean,
  authViewModel: AuthViewModel = hiltViewModel()
) {
  var inputPin by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }
  var errorCount by remember { mutableStateOf(0) }
  var keepUnlocked by remember { mutableStateOf(false) }
  val pinHint by authViewModel.appPinHint.collectAsStateWithLifecycle()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(pinPromptState.title) },
    text = {
      Column {
        Text(pinPromptState.subtitle)
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
          Text(stringResource(R.string.wrong_pin), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          if (errorCount >= 1 && !pinHint.isNullOrBlank()) {
             Spacer(Modifier.height(4.dp))
             Text(stringResource(R.string.hint_format, pinHint.orEmpty()), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
          }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = keepUnlocked,
            onCheckedChange = { keepUnlocked = it }
          )
          Text(stringResource(R.string.keep_unlocked_session), style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(onClick = {
        val success = onVerify(inputPin, keepUnlocked)
        if (!success) {
          isError = true
          errorCount++
          inputPin = ""
        }
      }) {
        Text(stringResource(R.string.unlock))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
      }
    }
  )
}
