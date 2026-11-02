package com.notdefterim.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.notdefterim.app.R

/**
 * Sayfaların üstündeki "Herhangi Birşey Ara" kutusu.
 * Hazır OutlinedTextField en az 56 dp yüksekliğinde ve iç boşluğu sabit; daha alçak (50 dp) durabilsin diye
 * aynı görünüş BasicTextField ile kurulur. Metin varsa temizleme düğmesi, ardından [trailing] gelir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSearchField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  trailing: @Composable RowScope.() -> Unit = {}
) {
  val interactionSource = remember { MutableInteractionSource() }
  val colors = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
  )
  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    singleLine = true,
    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    interactionSource = interactionSource,
    modifier = modifier.height(50.dp)
  ) { innerTextField ->
    OutlinedTextFieldDefaults.DecorationBox(
      value = value,
      innerTextField = innerTextField,
      enabled = true,
      singleLine = true,
      visualTransformation = VisualTransformation.None,
      interactionSource = interactionSource,
      placeholder = { Text(stringResource(R.string.search_anything)) },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      trailingIcon = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search_desc))
            }
          }
          trailing()
        }
      },
      colors = colors,
      contentPadding = OutlinedTextFieldDefaults.contentPadding(top = 0.dp, bottom = 0.dp),
      container = {
        OutlinedTextFieldDefaults.Container(
          enabled = true,
          isError = false,
          interactionSource = interactionSource,
          colors = colors,
          shape = CircleShape
        )
      }
    )
  }
}
