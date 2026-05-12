package com.notdefterim.app.ui.notelist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AddCategoryDialog(
  onDismiss: () -> Unit,
  onAddCategory: (name: String, colorHex: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  
  // Varsayılan pastel renkler
  val defaultColors = listOf("#FFD5E5", "#FFF0D5", "#E5FFD5", "#D5E5FF", "#E5D5FF", "#FFD5D5")
  var selectedColorHex by remember { mutableStateOf(defaultColors[0]) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Yeni Kategori Ekle") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Kategori Adı") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Renk Seçimi", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          defaultColors.forEach { hex ->
            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .clickable { selectedColorHex = hex }
                .then(
                  if (selectedColorHex == hex) {
                    Modifier.padding(4.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                  } else {
                    Modifier
                  }
                )
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onAddCategory(name.trim(), selectedColorHex)
            onDismiss()
          }
        },
        enabled = name.isNotBlank()
      ) {
        Text("Ekle")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("İptal")
      }
    }
  )
}
