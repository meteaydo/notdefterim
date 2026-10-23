package com.notdefterim.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notdefterim.app.R

/**
 * Arama çubuğundaki sıralama düğmesi ve menüsü.
 * Varsayılandan ([default]) farklı bir sıra seçiliyse simge vurgulanır.
 */
@Composable
fun <T> SortMenuButton(
  options: List<T>,
  selected: T,
  default: T,
  labelResId: (T) -> Int,
  onSelect: (T) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { expanded = true }) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.Sort,
        contentDescription = stringResource(R.string.sort_title),
        tint = if (selected != default) MaterialTheme.colorScheme.primary else LocalContentColor.current
      )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      Text(
        text = stringResource(R.string.sort_title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      )
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(stringResource(labelResId(option))) },
          trailingIcon = {
            if (option == selected) {
              Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
          },
          onClick = {
            onSelect(option)
            expanded = false
          }
        )
      }
    }
  }
}
