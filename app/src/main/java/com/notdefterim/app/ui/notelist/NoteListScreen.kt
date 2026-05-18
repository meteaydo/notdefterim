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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.notdefterim.app.ui.notelist.components.SmartFlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.notdefterim.app.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.model.Category
import com.notdefterim.app.ui.notelist.components.NoteCard

/**
 * Not listesi ekranı.
 * Spotlight arama + StaggeredGrid (Masonry layout) + FAB.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
  onNoteClick: (Long) -> Unit,
  onNewNoteClick: (Long?) -> Unit,
  modifier: Modifier = Modifier,
  hideTopBar: Boolean = false,
  viewModel: NoteListViewModel = hiltViewModel()
) {
  val notes by viewModel.notes.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
  val isEmpty by viewModel.isEmptyState.collectAsStateWithLifecycle()
  
  val categories by viewModel.categories.collectAsStateWithLifecycle()
  val selectedFilterType by viewModel.selectedFilterType.collectAsStateWithLifecycle()
  val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
  var isCategoriesExpanded by remember { mutableStateOf(false) }

  var showDeleteDialog by remember { mutableStateOf<Note?>(null) }

  // Kategori düzenleme/silme state'leri
  var editingCategory by remember { mutableStateOf<Category?>(null) }
  var editCategoryName by remember { mutableStateOf("") }
  var editCategoryColor by remember { mutableStateOf("") }
  var showEditCategoryDialog by remember { mutableStateOf(false) }
  var deletingCategory by remember { mutableStateOf<Category?>(null) }
  var showDeleteCategoryDialog by remember { mutableStateOf(false) }

  // Yeni kategori ekleme state'leri
  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var newCategoryName by remember { mutableStateOf("") }
  val defaultCategoryColors = listOf("#4CAF50", "#2196F3", "#FF9800", "#E91E63", "#9C27B0", "#00BCD4", "#795548")
  var newCategoryColor by remember { mutableStateOf(defaultCategoryColors.first()) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    topBar = {
      if (!hideTopBar) {
        AnimatedVisibility(
          visible = !isSearchActive,
          enter = slideInVertically() + fadeIn(),
          exit = slideOutVertically() + fadeOut()
        ) {
          TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.my_notes),
              style = MaterialTheme.typography.headlineSmall
            )
          },
          actions = {
            IconButton(onClick = { viewModel.onSearchActiveChange(true) }) {
              Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search))
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          )
        )
      }
    }},
    floatingActionButton = {
      if (!isSearchActive) {
        FloatingActionButton(
          onClick = { 
            val catId = if (selectedFilterType == NoteListViewModel.FilterType.CATEGORY) selectedCategoryId else null
            onNewNoteClick(catId) 
          },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
          Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.new_note))
        }
      }
    }
  ) { paddingValues ->

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {

      // ── Spotlight Arama Çubuğu (Yeni Tasarım) ──────────────────────────────────────
      OutlinedTextField(
        value = searchQuery,
        onValueChange = viewModel::onSearchQueryChange,
        placeholder = { Text("Herhangi Birşey Ara") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search_desc))
            }
          }
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
          .fillMaxWidth()
          .alpha(0.7f)
          .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
      )

      // ── Kategori Filtre Çubuğu ──────────────────────────────────────
      if (!isSearchActive) {
        val maxChipsWhenCollapsed = 7
        val visibleCategories = if (isCategoriesExpanded) categories else categories.take(maxChipsWhenCollapsed)
        val showExpandChip = !isCategoriesExpanded && categories.size > maxChipsWhenCollapsed

        SmartFlowRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
          horizontalSpacing = 8.dp,
          verticalSpacing = 8.dp
        ) {
          // Tümü
          FilterChip(
            selected = selectedFilterType == NoteListViewModel.FilterType.ALL,
            onClick = { viewModel.setFilter(NoteListViewModel.FilterType.ALL) },
            label = { Text("Tümü", style = MaterialTheme.typography.labelMedium) },
            // leadingIcon = null,
            modifier = Modifier
              .height(28.dp)
              .alpha(if (selectedFilterType == NoteListViewModel.FilterType.ALL) 1f else 0.4f),
            colors = FilterChipDefaults.filterChipColors(
              labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
              selectedLabelColor = MaterialTheme.colorScheme.onSurface
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selectedFilterType == NoteListViewModel.FilterType.ALL,
              borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
              selectedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
              borderWidth = 1.dp,
              selectedBorderWidth = 1.dp
            )
          )

          // Kilitli
          FilterChip(
            selected = selectedFilterType == NoteListViewModel.FilterType.LOCKED,
            onClick = { viewModel.setFilter(NoteListViewModel.FilterType.LOCKED) },
            label = { 
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("Kilitli", style = MaterialTheme.typography.labelMedium)
              }
            },
            modifier = Modifier
              .height(28.dp)
              .alpha(if (selectedFilterType == NoteListViewModel.FilterType.LOCKED) 1f else 0.4f),
            colors = FilterChipDefaults.filterChipColors(
              labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
              selectedLabelColor = MaterialTheme.colorScheme.onSurface
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selectedFilterType == NoteListViewModel.FilterType.LOCKED,
              borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
              selectedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
              borderWidth = 1.dp,
              selectedBorderWidth = 1.dp
            )
          )
          
          // Kategoriler
          visibleCategories.forEach { cat ->
            val isSelected = selectedFilterType == NoteListViewModel.FilterType.CATEGORY && selectedCategoryId == cat.id
            val isProtected = viewModel.isCategoryProtected(cat)
            
            
            var showCatMenu by remember { mutableStateOf(false) }
            
            Box {
              FilterChip(
                selected = isSelected,
                onClick = { },
                label = { 
                   Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      if (cat.name.equals("Alışveriş", ignoreCase = true)) {
                          Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                      } else if (cat.name.equals("Yapılacaklar", ignoreCase = true)) {
                          Icon(Icons.Rounded.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp))
                      } else if (cat.name.equals("Fikirler", ignoreCase = true)) {
                          Icon(Icons.Outlined.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                      }
                      Text(cat.name, style = MaterialTheme.typography.labelMedium)
                   }
                },
                modifier = Modifier
                  .height(28.dp)
                  .alpha(if (isSelected) 1f else 0.4f),
                colors = FilterChipDefaults.filterChipColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  labelColor = MaterialTheme.colorScheme.onSurface,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isSelected,
                  borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  selectedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                  borderWidth = 1.dp,
                  selectedBorderWidth = 1.dp
                )
              )

              // Şeffaf üst katman — FilterChip'in dokunma olaylarını yakalar
              Box(
                modifier = Modifier
                  .matchParentSize()
                  .combinedClickable(
                    onClick = {
                      if (isSelected) viewModel.setFilter(NoteListViewModel.FilterType.ALL)
                      else viewModel.setFilter(NoteListViewModel.FilterType.CATEGORY, cat.id)
                    },
                    onLongClick = {
                      if (!isProtected) showCatMenu = true
                    },
                    onDoubleClick = {
                      if (!isProtected) showCatMenu = true
                    }
                  )
              )
              
              // Uzun basma menüsü (sadece korumasız kategoriler için)
              DropdownMenu(
                expanded = showCatMenu,
                onDismissRequest = { showCatMenu = false },
                offset = DpOffset(0.dp, (-160).dp)
              ) {
                DropdownMenuItem(
                  text = { Text("Düzenle") },
                  onClick = {
                    showCatMenu = false
                    editingCategory = cat
                    editCategoryName = cat.name
                    editCategoryColor = cat.colorHex
                    showEditCategoryDialog = true
                  },
                  leadingIcon = { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                DropdownMenuItem(
                  text = { Text("Sil", color = MaterialTheme.colorScheme.error) },
                  onClick = {
                    showCatMenu = false
                    deletingCategory = cat
                    showDeleteCategoryDialog = true
                  },
                  leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                )
              }
            }
          }

          if (showExpandChip) {
            FilterChip(
              selected = false,
              onClick = { isCategoriesExpanded = true },
              label = { Text("...", style = MaterialTheme.typography.labelMedium) },
              modifier = Modifier.height(28.dp),
              colors = FilterChipDefaults.filterChipColors(
                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                borderWidth = 1.dp,
              )
            )
          } else if (isCategoriesExpanded && categories.size > maxChipsWhenCollapsed) {
            FilterChip(
              selected = false,
              onClick = { isCategoriesExpanded = false },
              label = { Text("Gizle", style = MaterialTheme.typography.labelMedium) },
              modifier = Modifier.height(28.dp),
              colors = FilterChipDefaults.filterChipColors(
                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                borderWidth = 1.dp,
              )
            )
          }

          // Yeni kategori ekleme butonu
          InputChip(
            selected = false,
            onClick = {
              newCategoryName = ""
              newCategoryColor = defaultCategoryColors.first()
              showAddCategoryDialog = true
            },
            label = { Text("+ Kategori Ekle", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.height(28.dp).alpha(0.4f)
          )
        }
      }

      Box(modifier = Modifier.weight(1f)) {
        // Kaydırma geçiş efekti (sabit alanın alt yüzeyinin erimesi)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(
              brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                  androidx.compose.ui.graphics.Color.Transparent
                )
              )
            )
            .align(Alignment.TopCenter)
            .zIndex(1f)
        )

        // ── Not Grid veya Boş Durum ─────────────────────────────────────
        AnimatedContent(
          targetState = isEmpty && searchQuery.isEmpty(),
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "empty_state_transition"
        ) { isEmptyState ->
          if (isEmptyState) {
            EmptyNotesContent(
              selectedFilterType = selectedFilterType,
              onNewNoteClick = {
                val catId = if (selectedFilterType == NoteListViewModel.FilterType.CATEGORY) selectedCategoryId else null
                onNewNoteClick(catId)
              }
            )
          } else {
            Box(modifier = Modifier.fillMaxSize()) {
              NoteGrid(
                notes = notes,
                selectedCategoryId = selectedCategoryId,
                selectedFilterType = selectedFilterType,
                onNoteClick = { noteId ->
                  onNoteClick(noteId)
                },
                onDeleteClick = { note ->
                  showDeleteDialog = note
                },
                onTogglePin = { note ->
                  viewModel.togglePin(note)
                }
              )

              val showCategoryEmpty = selectedFilterType == NoteListViewModel.FilterType.CATEGORY && selectedCategoryId != null && notes.none { it.category?.id == selectedCategoryId }
              val showLockedEmpty = selectedFilterType == NoteListViewModel.FilterType.LOCKED && notes.none { it.isLocked }

              if (showCategoryEmpty || showLockedEmpty) {
                Text(
                  text = if (showLockedEmpty) "Hiç kilitli notunuz yok" else "Bu kategoride not yok",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                  textAlign = TextAlign.Center,
                  modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 140.dp, start = 32.dp, end = 32.dp)
                )
              }
            }
          }
        }
      }
    }


    if (showDeleteDialog != null) {
      AlertDialog(
        onDismissRequest = { showDeleteDialog = null },
        title = { Text(stringResource(R.string.delete_note_title)) },
        text = { Text(stringResource(R.string.delete_note_desc)) },
        confirmButton = {
          TextButton(
            onClick = {
              showDeleteDialog?.let { viewModel.deleteNote(it.id) }
              showDeleteDialog = null
            }
          ) {
            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteDialog = null }) {
            Text(stringResource(R.string.cancel))
          }
        }
      )
    }

    // ── Kategori Düzenleme Diyalogu ──────────────────────────────────────────
    if (showEditCategoryDialog && editingCategory != null) {
      AlertDialog(
        onDismissRequest = {
          showEditCategoryDialog = false
          editingCategory = null
        },
        title = { Text("Kategoriyi Düzenle") },
        text = {
          OutlinedTextField(
            value = editCategoryName,
            onValueChange = { editCategoryName = it },
            label = { Text("Kategori Adı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        },
        confirmButton = {
          TextButton(
            onClick = {
              editingCategory?.let { cat ->
                if (editCategoryName.isNotBlank()) {
                  viewModel.updateCategory(cat.copy(name = editCategoryName.trim()))
                }
              }
              showEditCategoryDialog = false
              editingCategory = null
            }
          ) {
            Text("Kaydet")
          }
        },
        dismissButton = {
          TextButton(onClick = {
            showEditCategoryDialog = false
            editingCategory = null
          }) {
            Text("İptal")
          }
        }
      )
    }

    // ── Kategori Silme Onay Diyalogu ──────────────────────────────────────────
    if (showDeleteCategoryDialog && deletingCategory != null) {
      AlertDialog(
        onDismissRequest = {
          showDeleteCategoryDialog = false
          deletingCategory = null
        },
        title = { Text("Kategoriyi Sil") },
        text = { Text("\"${deletingCategory?.name}\" kategorisini silmek istediğinize emin misiniz? Bu kategoriye atanmış notlar etkilenmez.") },
        confirmButton = {
          TextButton(
            onClick = {
              deletingCategory?.let { viewModel.deleteCategory(it) }
              showDeleteCategoryDialog = false
              deletingCategory = null
            }
          ) {
            Text("Sil", color = MaterialTheme.colorScheme.error)
          }
        },
        dismissButton = {
          TextButton(onClick = {
            showDeleteCategoryDialog = false
            deletingCategory = null
          }) {
            Text("İptal")
          }
        }
      )
    }

    // ── Yeni Kategori Ekleme Diyalogu ──────────────────────────────────────────
    if (showAddCategoryDialog) {
      AlertDialog(
        onDismissRequest = { showAddCategoryDialog = false },
        title = { Text("Yeni Kategori") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
              value = newCategoryName,
              onValueChange = { newCategoryName = it },
              label = { Text("Kategori Adı") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Text("Renk Seçin", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              defaultCategoryColors.forEach { colorHex ->
                val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .background(color, CircleShape)
                    .then(
                      if (newCategoryColor == colorHex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                      else Modifier
                    )
                    .combinedClickable(onClick = { newCategoryColor = colorHex })
                )
              }
            }
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
              if (newCategoryName.isNotBlank()) {
                viewModel.addCategory(newCategoryName.trim(), newCategoryColor)
              }
              showAddCategoryDialog = false
            }
          ) {
            Text("Ekle")
          }
        },
        dismissButton = {
          TextButton(onClick = { showAddCategoryDialog = false }) {
            Text("İptal")
          }
        }
      )
    }
  }
}

// ─── Staggered Grid Bileşeni ─────────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NoteGrid(
  notes: List<Note>,
  selectedCategoryId: Long?,
  selectedFilterType: NoteListViewModel.FilterType,
  onNoteClick: (Long) -> Unit,
  onDeleteClick: (Note) -> Unit,
  onTogglePin: (Note) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
    verticalItemSpacing = 8.dp,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    modifier = modifier.fillMaxSize()
  ) {
    items(notes, key = { it.id }) { note ->
      val isCategoryMode = selectedFilterType == NoteListViewModel.FilterType.CATEGORY && selectedCategoryId != null
      val isLockedMode = selectedFilterType == NoteListViewModel.FilterType.LOCKED

      val isHighlighted = when {
        isCategoryMode -> note.category?.id == selectedCategoryId
        isLockedMode -> note.isLocked
        else -> false
      }

      val isDisabled = when {
        isCategoryMode -> note.category?.id != selectedCategoryId
        isLockedMode -> !note.isLocked
        else -> false
      }

      var showMenu by remember { androidx.compose.runtime.mutableStateOf(false) }

      Box {
        NoteCard(
          note = note,
          isDisabled = isDisabled,
          isHighlighted = isHighlighted,
          onClick = { onNoteClick(note.id) },
          onLongClick = { showMenu = true },
          onTogglePin = { onTogglePin(note) }
        )

        if (showMenu) {
          androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showMenu = false }
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
            ) {
              Text(
                text = "Not Seçenekleri",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
              )
              androidx.compose.material3.ListItem(
                headlineContent = { Text(if (note.isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin)) },
                leadingContent = { Icon(Icons.Rounded.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                modifier = Modifier.clickable {
                  onTogglePin(note)
                  showMenu = false
                }
              )
              androidx.compose.material3.ListItem(
                headlineContent = { Text("Yüzen Pencerede Aç") },
                leadingContent = { Icon(Icons.Rounded.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                modifier = Modifier.clickable {
                  val displayContent = if (note.isChecklist) {
                      try {
                          val items = kotlinx.serialization.json.Json.decodeFromString<List<com.notdefterim.app.domain.model.ChecklistItem>>(note.content)
                          items.joinToString("\n") { (if (it.isChecked) "☑ " else "☐ ") + it.text }
                      } catch (e: Exception) {
                          note.content
                      }
                  } else {
                      note.content
                  }
                  com.notdefterim.app.service.FloatingNoteManager.show(
                    context,
                    note.title,
                    displayContent
                  )
                  showMenu = false
                }
              )
              androidx.compose.material3.ListItem(
                headlineContent = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable {
                  onDeleteClick(note)
                  showMenu = false
                }
              )
            }
          }
        }
      }
    }
  }
}

// ─── Boş Durum Bileşeni ──────────────────────────────────────────────────────

@Composable
private fun EmptyNotesContent(
  selectedFilterType: NoteListViewModel.FilterType,
  onNewNoteClick: (Long?) -> Unit,
  modifier: Modifier = Modifier
) {
  val emptyText = when (selectedFilterType) {
    NoteListViewModel.FilterType.CATEGORY -> "Bu kategoride not yok"
    NoteListViewModel.FilterType.LOCKED -> "Hiç kilitli notunuz yok"
    else -> stringResource(R.string.no_notes_yet)
  }
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
      text = emptyText,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
      textAlign = TextAlign.Center
    )
  }
}
