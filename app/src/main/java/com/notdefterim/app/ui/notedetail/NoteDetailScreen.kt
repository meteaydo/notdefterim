package com.notdefterim.app.ui.notedetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.domain.model.NoteColor
import com.notdefterim.app.ui.theme.LocalNoteCardColors

/**
 * Not ekleme ve düzenleme ekranı.
 * Renk seçici, sabitleme ve silme işlemleri bu ekranda yapılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: NoteDetailViewModel = hiltViewModel()
) {
  val title by viewModel.title.collectAsStateWithLifecycle()
  val content by viewModel.content.collectAsStateWithLifecycle()
  val selectedColor by viewModel.selectedColor.collectAsStateWithLifecycle()
  val isPinned by viewModel.isPinned.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  // Tek seferlik olayları dinle
  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is NoteDetailEvent.NavigateBack -> onNavigateBack()
        is NoteDetailEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
      }
    }
  }

  val noteCardColors = LocalNoteCardColors.current
  val backgroundColor = noteCardColors.colors.getOrElse(selectedColor.index) {
    MaterialTheme.colorScheme.background
  }

  val animatedBackground by animateColorAsState(
    targetValue = backgroundColor,
    animationSpec = tween(durationMillis = 400),
    label = "bg_anim"
  )

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = animatedBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {},
        navigationIcon = {
          IconButton(onClick = { viewModel.saveNote() }) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri")
          }
        },
        actions = {
          // Sabitle / Sabitlemeyi kaldır
          IconButton(onClick = viewModel::onPinToggle) {
            Icon(
              imageVector = Icons.Rounded.PushPin,
              contentDescription = if (isPinned) "Sabitlemeyi kaldır" else "Sabitle",
              tint = if (isPinned) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          // Kaydet
          IconButton(onClick = { viewModel.saveNote() }) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = "Kaydet",
              tint = MaterialTheme.colorScheme.primary
            )
          }
          // Sil
          IconButton(onClick = { viewModel.deleteNote() }) {
            Icon(
              imageVector = Icons.Rounded.Delete,
              contentDescription = "Sil",
              tint = MaterialTheme.colorScheme.error
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent
        )
      )
    }
  ) { paddingValues ->

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .imePadding()
        .navigationBarsPadding()
    ) {

      // ── Başlık ─────────────────────────────────────────────────────
      TextField(
        value = title,
        onValueChange = viewModel::onTitleChange,
        placeholder = {
          Text(
            "Başlık",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          )
        },
        textStyle = MaterialTheme.typography.headlineSmall,
        colors = TextFieldDefaults.colors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
      )

      // ── İçerik ─────────────────────────────────────────────────────
      TextField(
        value = content,
        onValueChange = viewModel::onContentChange,
        placeholder = {
          Text(
            "Notunuzu yazın...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          )
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      )

      // ── Renk Seçici ────────────────────────────────────────────────
      ColorPickerRow(
        noteCardColors = noteCardColors.colors,
        selectedColor = selectedColor,
        onColorSelect = viewModel::onColorSelect,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      )
    }
  }
}

// ─── Renk Seçici ─────────────────────────────────────────────────────────────

@Composable
private fun ColorPickerRow(
  noteCardColors: List<Color>,
  selectedColor: NoteColor,
  onColorSelect: (NoteColor) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    NoteColor.entries.forEachIndexed { _, noteColor ->
      val colorValue = noteCardColors.getOrElse(noteColor.index) { Color.Gray }
      val isSelected = selectedColor == noteColor

      Box(
        modifier = Modifier
          .size(if (isSelected) 36.dp else 30.dp)
          .clip(CircleShape)
          .background(colorValue)
          .then(
            if (isSelected) {
              Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            } else {
              Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            }
          )
          .clickable { onColorSelect(noteColor) }
      )
    }
  }
}
