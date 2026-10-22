package com.notdefterim.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.notdefterim.app.R
import com.notdefterim.app.core.security.PinCheck

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SetupAppPinDialog(
  appPin: String?,
  appPinHint: String?,
  appPinScope: Int,
  targetScope: Int = 0,
  // Kayıtlı PIN yalnızca hash olarak tutulur; girilen PIN bununla doğrulanır (deneme sınırı dahil)
  checkCurrentPin: (String) -> PinCheck,
  onDismiss: () -> Unit,
  onSave: (String?, String?, Int) -> Unit
) {
  var currentPinInput by remember { mutableStateOf("") }
  var newPinInput by remember { mutableStateOf("") }
  var newPinConfirmInput by remember { mutableStateOf("") }
  var pinHintInput by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var step by remember { mutableStateOf(if (appPin == null) 1 else 0) }
  var isNotesSelected by remember { mutableStateOf(if (appPin == null) targetScope == 0 || targetScope == 1 else appPinScope == 0 || appPinScope == 1) }
  var isPasswordsSelected by remember { mutableStateOf(if (appPin == null) targetScope == 0 || targetScope == 2 else appPinScope == 0 || appPinScope == 2) }
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { 
      Text(
        text = stringResource(if (appPin == null) R.string.set_new_pin else R.string.change_remove_pin),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
      ) 
    },
    text = {
      Column {
        if (step == 0) {
          OutlinedTextField(
            value = currentPinInput,
            onValueChange = { 
              if (it.length <= 6) currentPinInput = it
              errorMessage = null 
            },
            label = { Text(stringResource(R.string.enter_current_pin)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = errorMessage != null
          )
          if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (!appPinHint.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(stringResource(R.string.hint_format, appPinHint), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
          }
        } else {
          OutlinedTextField(
            value = newPinInput,
            onValueChange = { 
              if (it.length <= 6) newPinInput = it
              errorMessage = null 
            },
            label = { Text(stringResource(R.string.enter_new_pin)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            },
            singleLine = true,
            isError = errorMessage != null
          )
          if (!passwordVisible) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = newPinConfirmInput,
              onValueChange = { 
                if (it.length <= 6) newPinConfirmInput = it
                errorMessage = null 
              },
              label = { Text(stringResource(R.string.pin_new_repeat)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              visualTransformation = PasswordVisualTransformation(),
              singleLine = true,
              isError = errorMessage != null
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = pinHintInput,
            onValueChange = { 
              if (it.length <= 30) pinHintInput = it
              errorMessage = null
            },
            label = { Text(stringResource(R.string.pin_hint_label)) },
            singleLine = true
          )
          
          Spacer(modifier = Modifier.height(16.dp))
          Text(stringResource(R.string.pin_scope_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isNotesSelected, onCheckedChange = { 
              isNotesSelected = it
              errorMessage = null
            })
            Text(stringResource(R.string.pin_scope_notes), style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { 
              isNotesSelected = !isNotesSelected
              errorMessage = null
            })
            Spacer(modifier = Modifier.width(16.dp))
            Checkbox(checked = isPasswordsSelected, onCheckedChange = { 
              isPasswordsSelected = it
              errorMessage = null
            })
            Text(stringResource(R.string.pin_scope_passwords), style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { 
              isPasswordsSelected = !isPasswordsSelected
              errorMessage = null
            })
          }
          
          if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            stringResource(R.string.pin_forget_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (step == 0) {
            when (val result = checkCurrentPin(currentPinInput)) {
              PinCheck.Correct -> step = 1
              PinCheck.Wrong -> errorMessage = context.getString(R.string.pin_entered_wrong)
              is PinCheck.LockedOut -> errorMessage = context.getString(R.string.pin_locked_out, result.seconds)
            }
          } else {
            if (newPinInput.length >= 4) {
              if (!isNotesSelected && !isPasswordsSelected) {
                errorMessage = context.getString(R.string.pin_scope_required)
              } else if (pinHintInput.isBlank()) {
                errorMessage = context.getString(R.string.pin_hint_required)
              } else if (passwordVisible || newPinInput == newPinConfirmInput) {
                val newScope = if (isNotesSelected && isPasswordsSelected) 0 else if (isNotesSelected) 1 else 2
                onSave(newPinInput, pinHintInput, newScope)
                onDismiss()
              } else {
                errorMessage = context.getString(R.string.pin_mismatch)
              }
            } else {
              errorMessage = context.getString(R.string.pin_too_short)
            }
          }
        }
      ) {
        Text(stringResource(if (step == 0) R.string.next else R.string.save))
      }
    },
    dismissButton = {
      Row {
        if (step == 0 && appPin != null) {
          TextButton(
            onClick = {
              when (val result = checkCurrentPin(currentPinInput)) {
                PinCheck.Correct -> {
                  onSave(null, null, 0)
                  onDismiss()
                }
                PinCheck.Wrong -> errorMessage = context.getString(R.string.pin_entered_wrong)
                is PinCheck.LockedOut -> errorMessage = context.getString(R.string.pin_locked_out, result.seconds)
              }
            }
          ) {
            Text(stringResource(R.string.remove_pin), color = MaterialTheme.colorScheme.error)
          }
        }
        TextButton(onClick = onDismiss) {
          Text(stringResource(R.string.cancel))
        }
      }
    }
  )
}
