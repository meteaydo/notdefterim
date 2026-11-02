package com.notdefterim.app.ui.notedetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.notdefterim.app.R
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import com.notdefterim.app.util.matchesSearch
import com.notdefterim.app.util.searchMatchRanges
import com.notdefterim.app.domain.model.ChecklistItem

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester

@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    searchQuery: String = "",
    onItemChange: (ChecklistItem) -> Unit,
    /** Boş madde ekler (afterId'nin altına, null ise sona) ve yeni maddenin id'sini döner */
    onAddItem: (afterId: String?) -> String,
    onRemoveItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Yeni eklenen madde, listenin ortasında da olsa odak alsın
    var focusItemId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            // Araya madde girince odak ve satır durumu doğru maddede kalsın
            key(item.id) {
                ChecklistItemRow(
                    item = item,
                    searchQuery = searchQuery,
                    onItemChange = onItemChange,
                    onRemove = { onRemoveItem(item.id) },
                    onNext = { focusItemId = onAddItem(item.id) },
                    isLastItem = index == items.size - 1,
                    requestFocus = item.id == focusItemId,
                    onFocusRequested = { focusItemId = null }
                )
            }
        }

        // Onay kutusu sütunuyla hizalı "+ Madde ekle" satırı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button) { focusItemId = onAddItem(null) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(R.string.checklist_add_item),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    isLastItem: Boolean,
    requestFocus: Boolean,
    onFocusRequested: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // İmlecin maddenin başında olup olmadığını bilmek için TextFieldValue tutulur;
    // metin dışarıdan değişirse imleç sona alınır
    var fieldValue by remember { mutableStateOf(TextFieldValue(item.text, TextRange(item.text.length))) }
    val value = if (fieldValue.text == item.text) fieldValue else TextFieldValue(item.text, TextRange(item.text.length))

    LaunchedEffect(item.id) {
        if (item.text.isEmpty() && isLastItem) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) { }
            onFocusRequested()
        }
    }
    val density = LocalDensity.current

    // Klavye açılıp görünür alan daralınca odaklı madde klavyenin altında kalmasın
    var isFocused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(density)
    LaunchedEffect(isFocused, imeBottom) {
        if (isFocused && imeBottom > 0) {
            try {
                bringIntoViewRequester.bringIntoView()
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(searchQuery, item.text) {
        if (searchQuery.isNotBlank() && item.text.matchesSearch(searchQuery)) {
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
                text.toString().searchMatchRanges(searchQuery).forEach { range ->
                    addStyle(
                        style = SpanStyle(
                            background = primaryContainer,
                            color = onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        ),
                        start = range.first,
                        end = range.last + 1
                    )
                }
            }
            TransformedText(annotated, OffsetMapping.Identity)
        }
    }

    // Onay kutusu ve kaldır düğmesi en az 48 dp dokunma alanı alır (erişilebilirlik, K5)
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                )
            )
        } else if (isFocused) {
            // Kutusuz satırda yazarken madde imini geri koyma düğmesi
            IconButton(onClick = { onItemChange(item.copy(hasCheckbox = true)) }) {
                Icon(
                    Icons.Rounded.CheckBoxOutlineBlank,
                    contentDescription = stringResource(R.string.checklist_add_checkbox),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }
    
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                fieldValue = newValue
                if (newValue.text != item.text) onItemChange(item.copy(text = newValue.text))
            },
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
                .onFocusChanged { isFocused = it.isFocused }
                // İmleç maddenin başındayken silme tuşu madde imini kaldırır (metne dokunmaz)
                .onPreviewKeyEvent { event ->
                    val atStart = value.selection.collapsed && value.selection.start == 0
                    if (item.hasCheckbox && atStart && event.type == KeyEventType.KeyDown && event.key == Key.Backspace) {
                        onItemChange(item.copy(hasCheckbox = false, isChecked = false))
                        true
                    } else {
                        false
                    }
                }
                // Tek satır metin 48 dp yüksekliğindeki onay kutusuyla aynı hizada dursun
                .padding(end = 8.dp, top = 12.dp, bottom = 12.dp),
            decorationBox = { innerTextField ->
                if (item.text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.checklist_item_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        )
    
        IconButton(
            onClick = {
                // Boş madde hemen, dolu madde onaydan sonra silinir
                if (item.text.isBlank()) {
                    onRemove()
                } else {
                    showDeleteDialog = true
                }
            }
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_row_title)) },
            text = { Text(stringResource(R.string.delete_row_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onRemove()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
