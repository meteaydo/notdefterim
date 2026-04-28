package com.notdefterim.app.ui.notelist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.ui.notelist.components.NoteCard

/**
 * Not listesi ekranı.
 * Spotlight arama + StaggeredGrid (Masonry layout) + FAB.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
  onNoteClick: (Long) -> Unit,
  onNewNoteClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: NoteListViewModel = hiltViewModel()
) {
  val notes by viewModel.notes.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
  val isEmpty by viewModel.isEmptyState.collectAsStateWithLifecycle()

  // Uzun basma context menüsü için seçili not
  var selectedNote by remember { mutableStateOf<Note?>(null) }
  var showContextMenu by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      AnimatedVisibility(
        visible = !isSearchActive,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
      ) {
        TopAppBar(
          title = {
            Text(
              text = "Notlarım",
              style = MaterialTheme.typography.headlineSmall
            )
          },
          actions = {
            IconButton(onClick = { viewModel.onSearchActiveChange(true) }) {
              Icon(Icons.Rounded.Search, contentDescription = "Ara")
            }
            IconButton(onClick = onSettingsClick) {
              Icon(Icons.Rounded.Settings, contentDescription = "Ayarlar")
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          )
        )
      }
    },
    floatingActionButton = {
      AnimatedVisibility(
        visible = !isSearchActive,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
      ) {
        FloatingActionButton(
          onClick = onNewNoteClick,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
          Icon(Icons.Rounded.Add, contentDescription = "Yeni Not")
        }
      }
    }
  ) { paddingValues ->

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {

      // ── Spotlight Arama Çubuğu ──────────────────────────────────────
      SearchBar(
        query = searchQuery,
        onQueryChange = viewModel::onSearchQueryChange,
        onSearch = {},
        active = isSearchActive,
        onActiveChange = viewModel::onSearchActiveChange,
        leadingIcon = {
          Icon(Icons.Rounded.Search, contentDescription = null)
        },
        trailingIcon = {
          AnimatedVisibility(visible = isSearchActive) {
            IconButton(onClick = { viewModel.onSearchActiveChange(false) }) {
              Icon(Icons.Rounded.Close, contentDescription = "Aramayı kapat")
            }
          }
        },
        placeholder = { Text("Notlarda ara...") },
        colors = SearchBarDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = if (isSearchActive) 0.dp else 16.dp)
      ) {
        // Arama aktifken sonuçları doğrudan göster
        NoteGrid(
          notes = notes,
          onNoteClick = { noteId ->
            viewModel.onSearchActiveChange(false)
            onNoteClick(noteId)
          },
          onNoteLongClick = { note ->
            selectedNote = note
            showContextMenu = true
          }
        )
      }

      // ── Not Grid veya Boş Durum ─────────────────────────────────────
      AnimatedContent(
        targetState = isEmpty && !isSearchActive,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "empty_state_transition"
      ) { isEmptyState ->
        if (isEmptyState) {
          EmptyNotesContent(onNewNoteClick = onNewNoteClick)
        } else if (!isSearchActive) {
          NoteGrid(
            notes = notes,
            onNoteClick = onNoteClick,
            onNoteLongClick = { note ->
              selectedNote = note
              showContextMenu = true
            }
          )
        }
      }
    }

    // ── Context Menüsü (uzun basma) ─────────────────────────────────────
    if (showContextMenu && selectedNote != null) {
      Box {
        DropdownMenu(
          expanded = showContextMenu,
          onDismissRequest = { showContextMenu = false }
        ) {
          DropdownMenuItem(
            text = {
              Text(if (selectedNote!!.isPinned) "Sabitlemeyi Kaldır" else "Sabitle")
            },
            leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
            onClick = {
              selectedNote?.let { viewModel.togglePin(it) }
              showContextMenu = false
            }
          )
          DropdownMenuItem(
            text = { Text("Sil", color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
              Icon(
                Icons.Rounded.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
              )
            },
            onClick = {
              selectedNote?.let { viewModel.deleteNote(it.id) }
              showContextMenu = false
            }
          )
        }
      }
    }
  }
}

// ─── Staggered Grid Bileşeni ─────────────────────────────────────────────────

@Composable
private fun NoteGrid(
  notes: List<Note>,
  onNoteClick: (Long) -> Unit,
  onNoteLongClick: (Note) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
    verticalItemSpacing = 8.dp,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    modifier = modifier.fillMaxSize()
  ) {
    items(notes, key = { it.id }) { note ->
      NoteCard(
        note = note,
        onClick = { onNoteClick(note.id) },
        onLongClick = { onNoteLongClick(note) }
      )
    }
  }
}

// ─── Boş Durum Bileşeni ──────────────────────────────────────────────────────

@Composable
private fun EmptyNotesContent(
  onNewNoteClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = Icons.Rounded.NoteAdd,
      contentDescription = null,
      modifier = Modifier.size(80.dp),
      tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Henüz not yok",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "İlk notunuzu oluşturmak için\n+ düğmesine dokunun",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
      textAlign = TextAlign.Center
    )
  }
}
