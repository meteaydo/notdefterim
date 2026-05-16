package com.notdefterim.app.ui.notedetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import com.notdefterim.app.util.normalizeForSearch
import com.notdefterim.app.domain.model.ChecklistItem

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester

@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    searchQuery: String = "",
    onItemChange: (ChecklistItem) -> Unit,
    onAddItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            ChecklistItemRow(
                item = item,
                searchQuery = searchQuery,
                onItemChange = onItemChange,
                onRemove = { onRemoveItem(item.id) },
                onNext = { onAddItem("") },
                isLastItem = index == items.size - 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    searchQuery: String,
    onItemChange: (ChecklistItem) -> Unit,
    onRemove: () -> Unit,
    onNext: () -> Unit,
    isLastItem: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(item.id) {
        if (item.text.isEmpty() && isLastItem) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) { }
        }
    }
    val density = LocalDensity.current
    
    LaunchedEffect(searchQuery, item.text) {
        if (searchQuery.isNotBlank() && item.text.normalizeForSearch().contains(searchQuery.normalizeForSearch())) {
            try {
                // Yaklaşık 4-5 satır (200dp) yukarıdan pay bırakarak kaydır (Rect kullanarak)
                val offset = with(density) { 200.dp.toPx() }
                bringIntoViewRequester.bringIntoView(androidx.compose.ui.geometry.Rect(0f, -offset, 1f, 1f))
            } catch (e: Exception) { }
        }
    }

    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    
    val visualTransformation = remember(searchQuery, primaryContainer, onPrimaryContainer) {
        if (searchQuery.isBlank()) VisualTransformation.None
        else VisualTransformation { text ->
            val annotated = buildAnnotatedString {
                append(text)
                val lowerText = text.toString().normalizeForSearch()
                val lowerQuery = searchQuery.normalizeForSearch()
                var startIndex = 0
                while (startIndex < lowerText.length) {
                    val index = lowerText.indexOf(lowerQuery, startIndex)
                    if (index < 0) break
                    addStyle(
                        style = SpanStyle(
                            background = primaryContainer,
                            color = onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        ),
                        start = index,
                        end = index + lowerQuery.length
                    )
                    startIndex = index + lowerQuery.length
                }
            }
            TransformedText(annotated, OffsetMapping.Identity)
        }
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .bringIntoViewRequester(bringIntoViewRequester),
            verticalAlignment = Alignment.Top
        ) {
            if (item.hasCheckbox) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { isChecked -> onItemChange(item.copy(isChecked = isChecked)) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(end = 4.dp).size(24.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        
            BasicTextField(
                value = item.text,
                onValueChange = { newText -> onItemChange(item.copy(text = newText)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = if (item.isChecked && item.hasCheckbox) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (item.isChecked && item.hasCheckbox) TextDecoration.LineThrough else null
                ),
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .padding(horizontal = 8.dp),
                decorationBox = { innerTextField ->
                    if (item.text.isEmpty()) {
                        Text(
                            text = "Madde...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )
        
            IconButton(
                onClick = {
                    if (item.text.isBlank()) {
                        onRemove()
                    } else if (item.hasCheckbox) {
                        onItemChange(item.copy(hasCheckbox = false, isChecked = false))
                    } else {
                        showDeleteDialog = true
                    }
                }, 
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Kaldır",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Satırı Sil") },
            text = { Text("Bu satırı silmek istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onRemove()
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
