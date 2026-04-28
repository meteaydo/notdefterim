package com.notdefterim.app.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.usecase.note.DeleteNoteUseCase
import com.notdefterim.app.domain.usecase.note.GetNotesUseCase
import com.notdefterim.app.domain.usecase.note.SearchNotesUseCase
import com.notdefterim.app.domain.usecase.note.UpdateNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteListViewModel @Inject constructor(
  private val getNotesUseCase: GetNotesUseCase,
  private val searchNotesUseCase: SearchNotesUseCase,
  private val deleteNoteUseCase: DeleteNoteUseCase,
  private val updateNoteUseCase: UpdateNoteUseCase
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _isSearchActive = MutableStateFlow(false)
  val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

  /**
   * Spotlight arama: debounce ile gereksiz DB sorgularını önler.
   * 300ms bekledikten sonra query değerlendirmeye alınır.
   */
  @OptIn(FlowPreview::class)
  val notes: StateFlow<List<Note>> = _searchQuery
    .debounce(300L)
    .flatMapLatest { query ->
      if (query.isBlank()) getNotesUseCase() else searchNotesUseCase(query)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList()
    )

  val isEmptyState: StateFlow<Boolean> = notes.map { it.isEmpty() }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = false
    )

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query
  }

  fun onSearchActiveChange(active: Boolean) {
    _isSearchActive.value = active
    if (!active) _searchQuery.value = ""
  }

  fun deleteNote(noteId: Long) {
    viewModelScope.launch {
      deleteNoteUseCase(noteId)
    }
  }

  fun togglePin(note: Note) {
    viewModelScope.launch {
      updateNoteUseCase(note.copy(isPinned = !note.isPinned))
    }
  }
}
