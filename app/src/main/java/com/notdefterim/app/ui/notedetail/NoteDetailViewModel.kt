package com.notdefterim.app.ui.notedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.model.NoteColor
import com.notdefterim.app.domain.usecase.note.AddNoteUseCase
import com.notdefterim.app.domain.usecase.note.DeleteNoteUseCase
import com.notdefterim.app.domain.usecase.note.GetNoteByIdUseCase
import com.notdefterim.app.domain.usecase.note.UpdateNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
  private val getNoteByIdUseCase: GetNoteByIdUseCase,
  private val addNoteUseCase: AddNoteUseCase,
  private val updateNoteUseCase: UpdateNoteUseCase,
  private val deleteNoteUseCase: DeleteNoteUseCase,
  savedStateHandle: SavedStateHandle
) : ViewModel() {

  // Navigation argümanı: 0 = yeni not, >0 = düzenleme
  private val noteId: Long = savedStateHandle["noteId"] ?: 0L

  private val _title = MutableStateFlow("")
  val title: StateFlow<String> = _title.asStateFlow()

  private val _content = MutableStateFlow("")
  val content: StateFlow<String> = _content.asStateFlow()

  private val _selectedColor = MutableStateFlow(NoteColor.DEFAULT)
  val selectedColor: StateFlow<NoteColor> = _selectedColor.asStateFlow()

  private val _isPinned = MutableStateFlow(false)
  val isPinned: StateFlow<Boolean> = _isPinned.asStateFlow()

  // Tek seferlik olaylar (navigasyon geri dönüşü, hata mesajı)
  private val _events = MutableSharedFlow<NoteDetailEvent>()
  val events = _events.asSharedFlow()

  private var currentNote: Note? = null

  init {
    if (noteId > 0L) {
      loadNote()
    }
  }

  private fun loadNote() {
    viewModelScope.launch {
      currentNote = getNoteByIdUseCase(noteId)
      currentNote?.let { note ->
        _title.value = note.title
        _content.value = note.content
        _selectedColor.value = note.color
        _isPinned.value = note.isPinned
      }
    }
  }

  fun onTitleChange(newTitle: String) {
    _title.value = newTitle
  }

  fun onContentChange(newContent: String) {
    _content.value = newContent
  }

  fun onColorSelect(color: NoteColor) {
    _selectedColor.value = color
  }

  fun onPinToggle() {
    _isPinned.value = !_isPinned.value
  }

  fun saveNote() {
    viewModelScope.launch {
      if (_title.value.isBlank() && _content.value.isBlank()) {
        // Boş not — sessizce geri dön
        _events.emit(NoteDetailEvent.NavigateBack)
        return@launch
      }

      if (noteId > 0L && currentNote != null) {
        // Düzenleme
        updateNoteUseCase(
          currentNote!!.copy(
            title = _title.value.trim(),
            content = _content.value.trim(),
            color = _selectedColor.value,
            isPinned = _isPinned.value
          )
        )
      } else {
        // Yeni not
        addNoteUseCase(
          Note(
            title = _title.value.trim(),
            content = _content.value.trim(),
            color = _selectedColor.value,
            isPinned = _isPinned.value
          )
        )
      }

      _events.emit(NoteDetailEvent.NavigateBack)
    }
  }

  fun deleteNote() {
    viewModelScope.launch {
      if (noteId > 0L) {
        deleteNoteUseCase(noteId)
      }
      _events.emit(NoteDetailEvent.NavigateBack)
    }
  }
}

sealed class NoteDetailEvent {
  object NavigateBack : NoteDetailEvent()
  data class ShowError(val message: String) : NoteDetailEvent()
}
