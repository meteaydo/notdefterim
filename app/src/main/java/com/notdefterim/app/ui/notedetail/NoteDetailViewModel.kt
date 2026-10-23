package com.notdefterim.app.ui.notedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.model.NoteColor
import com.notdefterim.app.domain.model.RepeatInterval
import com.notdefterim.app.domain.model.Category
import com.notdefterim.app.domain.model.ChecklistItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.notdefterim.app.domain.usecase.category.AddCategoryUseCase
import com.notdefterim.app.domain.usecase.category.GetCategoriesUseCase
import com.notdefterim.app.domain.usecase.note.AddNoteUseCase
import com.notdefterim.app.domain.usecase.note.DeleteNoteUseCase
import com.notdefterim.app.domain.usecase.note.GetNoteByIdUseCase
import com.notdefterim.app.domain.usecase.note.UpdateNoteUseCase
import com.notdefterim.app.service.ReminderManager
import com.notdefterim.app.ui.notedetail.components.ReminderSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDateTime
import javax.inject.Inject


@HiltViewModel
class NoteDetailViewModel @Inject constructor(
  private val getNoteByIdUseCase: GetNoteByIdUseCase,
  private val addNoteUseCase: AddNoteUseCase,
  private val updateNoteUseCase: UpdateNoteUseCase,
  private val deleteNoteUseCase: DeleteNoteUseCase,
  private val getCategoriesUseCase: GetCategoriesUseCase,
  private val addCategoryUseCase: AddCategoryUseCase,
  private val reminderManager: ReminderManager,
  savedStateHandle: SavedStateHandle
) : ViewModel() {

  // Navigation argümanı: 0 = yeni not, >0 = düzenleme
  private val noteId: Long = savedStateHandle["noteId"] ?: 0L
  private val categoryIdStr: String? = savedStateHandle["categoryId"]
  private val initialCategoryId: Long? = categoryIdStr?.toLongOrNull()

  private val _title = MutableStateFlow("")
  val title: StateFlow<String> = _title.asStateFlow()

  private val _content = MutableStateFlow("")
  val content: StateFlow<String> = _content.asStateFlow()

  val categories: StateFlow<List<Category>> = getCategoriesUseCase(com.notdefterim.app.domain.model.CategoryType.NOTE)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private val _selectedCategory = MutableStateFlow<Category?>(null)
  val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

  private val _selectedColor = MutableStateFlow(NoteColor.DEFAULT)
  val selectedColor: StateFlow<NoteColor> = _selectedColor.asStateFlow()

  private val _isPinned = MutableStateFlow(false)
  val isPinned: StateFlow<Boolean> = _isPinned.asStateFlow()

  private val _isLocked = MutableStateFlow(false)
  val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

  private val _reminderAt = MutableStateFlow<LocalDateTime?>(null)
  val reminderAt: StateFlow<LocalDateTime?> = _reminderAt.asStateFlow()

  private val _isChecklist = MutableStateFlow(false)
  val isChecklist: StateFlow<Boolean> = _isChecklist.asStateFlow()

  private val _checklistItems = MutableStateFlow<List<ChecklistItem>>(emptyList())
  val checklistItems: StateFlow<List<ChecklistItem>> = _checklistItems.asStateFlow()

  private val _repeatInterval = MutableStateFlow(RepeatInterval.NONE)
  val repeatInterval: StateFlow<RepeatInterval> = _repeatInterval.asStateFlow()

  private val _repeatDays = MutableStateFlow<Set<DayOfWeek>>(emptySet())
  val repeatDays: StateFlow<Set<DayOfWeek>> = _repeatDays.asStateFlow()

  // Aylık/yıllık tekrarın asıl günü; ekranda görünmez (bkz. ReminderSchedule)
  private var repeatDayOfMonth = 0

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _isSearchActive = MutableStateFlow(false)
  val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

  // Tek seferlik olaylar (navigasyon geri dönüşü, hata mesajı)
  private val _events = MutableSharedFlow<NoteDetailEvent>()
  val events = _events.asSharedFlow()

  private var currentNote: Note? = null

  // Arka plan kaydı ile geri tuşu kaydı aynı anda çalışıp yeni notu iki kez eklemesin
  private val saveMutex = Mutex()
  private var savedInBackground = false

  init {
    if (noteId > 0L) {
      loadNote()
    } else if (initialCategoryId != null) {
      viewModelScope.launch {
        getCategoriesUseCase(com.notdefterim.app.domain.model.CategoryType.NOTE).collect { catList ->
           val cat = catList.find { it.id == initialCategoryId }
           if (cat != null && _selectedCategory.value == null) {
               _selectedCategory.value = cat
           }
        }
      }
    }
  }

  private fun loadNote() {
    viewModelScope.launch {
      currentNote = getNoteByIdUseCase(noteId)
      currentNote?.let { note ->
        _title.value = note.title
        _content.value = note.content
        _selectedColor.value = note.color
        _selectedCategory.value = note.category
        _isPinned.value = note.isPinned
        _isLocked.value = note.isLocked
        _reminderAt.value = note.reminderAt
        _repeatInterval.value = note.repeatInterval
        _repeatDays.value = note.repeatDays
        repeatDayOfMonth = note.repeatDayOfMonth
        _isChecklist.value = note.isChecklist

        if (note.isChecklist) {
            try {
                _checklistItems.value = Json.decodeFromString<List<ChecklistItem>>(note.content)
                _content.value = ""
            } catch (e: Exception) {
                _content.value = note.content
                _isChecklist.value = false
            }
        } else {
            _content.value = note.content
        }
        
        // Görüntülenme sayısını artır ve arka planda kaydet
        val updatedNote = note.copy(viewCount = note.viewCount + 1)
        currentNote = updatedNote
        updateNoteUseCase(updatedNote)
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

  fun onCategorySelect(category: Category?) {
    _selectedCategory.value = category
  }

  fun addNewCategory(name: String, colorHex: String) {
    viewModelScope.launch {
      val newCat = Category(name = name, colorHex = colorHex)
      val id = addCategoryUseCase(newCat)
      _selectedCategory.value = newCat.copy(id = id)
    }
  }

  fun onPinToggle() {
    _isPinned.value = !_isPinned.value
  }

  fun onLockToggle() {
    _isLocked.value = !_isLocked.value
  }

  fun onToggleChecklistMode() {
    val currentMode = _isChecklist.value
    _isChecklist.value = !currentMode
    
    if (!currentMode) {
        // Text to Checklist
        val lines = _content.value.lines().filter { it.isNotBlank() }
        _checklistItems.value = lines.map { ChecklistItem(text = it.trim()) }
        if (_checklistItems.value.isEmpty()) {
             _checklistItems.value = listOf(ChecklistItem(text = ""))
        }
        _content.value = ""
    } else {
        // Checklist to Text
        val text = _checklistItems.value.map { it.text }.filter { it.isNotBlank() }.joinToString("\n")
        _content.value = text
        _checklistItems.value = emptyList()
    }
  }

  fun updateChecklistItem(item: ChecklistItem) {
      _checklistItems.value = _checklistItems.value.map {
          if (it.id == item.id) item else it
      }
  }

  fun addChecklistItem(text: String = "") {
      _checklistItems.value = _checklistItems.value + ChecklistItem(text = text)
  }

  fun removeChecklistItem(id: String) {
      _checklistItems.value = _checklistItems.value.filter { it.id != id }
  }

  fun setReminder(selection: ReminderSelection) {
    _reminderAt.value = selection.at
    _repeatInterval.value = selection.interval
    _repeatDays.value = selection.days
    repeatDayOfMonth = selection.dayOfMonth
  }

  fun clearReminder() {
    _reminderAt.value = null
    _repeatInterval.value = RepeatInterval.NONE
    _repeatDays.value = emptySet()
    repeatDayOfMonth = 0
  }

  fun onSearchQueryChange(newQuery: String) {
    _searchQuery.value = newQuery
  }

  fun onSearchActiveChange(isActive: Boolean) {
    _isSearchActive.value = isActive
    if (!isActive) {
      _searchQuery.value = ""
    }
  }

  fun saveNote() {
    viewModelScope.launch {
      val result = persistNote(deleteIfEmpty = true)
      // Değişiklikler arka planda zaten kaydedildiyse de "kaydedildi" bildirimi gösterilir
      val showSavedMessage = result == SaveResult.SAVED ||
          (result == SaveResult.UNCHANGED && savedInBackground)
      _events.emit(NoteDetailEvent.NavigateBack(showSavedMessage = showSavedMessage))
    }
  }

  /**
   * Ekran arka plana geçerken (başka uygulama, ekran kapanması, otomatik kilit) çağrılır.
   * Geri dönmeden kaydeder; boşaltılmış notu silmez, çünkü kullanıcı düzenlemenin ortasında olabilir.
   */
  fun saveInBackground() {
    viewModelScope.launch {
      if (persistNote(deleteIfEmpty = false) == SaveResult.SAVED) {
        savedInBackground = true
      }
    }
  }

  private suspend fun persistNote(deleteIfEmpty: Boolean): SaveResult =
    saveMutex.withLock { persistNoteLocked(deleteIfEmpty) }

  private suspend fun persistNoteLocked(deleteIfEmpty: Boolean): SaveResult {
    // Düzenlenen not henüz yüklenmediyse alanlar boştur; kaydetmek notu siler ya da kopyalardı
    if (noteId > 0L && currentNote == null) return SaveResult.UNCHANGED

    val finalContent = if (_isChecklist.value) {
        Json.encodeToString(_checklistItems.value)
    } else {
        _content.value.trim()
    }

    val isNoteEmpty = _title.value.isBlank() && finalContent.isBlank() && (!_isChecklist.value || _checklistItems.value.isEmpty())

    if (isNoteEmpty) {
      val savedId = currentNote?.id ?: 0L
      if (deleteIfEmpty && savedId > 0L) {
          // Var olan bir not tamamen boşaltılmışsa onu sil
          reminderManager.cancelReminder(savedId)
          deleteNoteUseCase(savedId)
      }
      return SaveResult.EMPTY
    }

    val existingNote = currentNote
    if (existingNote != null) {
      // Düzenleme (ya da arka planda daha önce kaydedilmiş yeni not)
      val hasChanges = existingNote.title != _title.value.trim() ||
          existingNote.content != finalContent ||
          existingNote.color != _selectedColor.value ||
          existingNote.category != _selectedCategory.value ||
          existingNote.isPinned != _isPinned.value ||
          existingNote.isLocked != _isLocked.value ||
          existingNote.reminderAt != _reminderAt.value ||
          existingNote.repeatInterval != _repeatInterval.value ||
          existingNote.repeatDays != _repeatDays.value ||
          existingNote.repeatDayOfMonth != repeatDayOfMonth ||
          existingNote.isChecklist != _isChecklist.value

      if (!hasChanges) return SaveResult.UNCHANGED

      val updatedNote = existingNote.copy(
        title = _title.value.trim(),
        content = finalContent,
        color = _selectedColor.value,
        category = _selectedCategory.value,
        isPinned = _isPinned.value,
        isLocked = _isLocked.value,
        reminderAt = _reminderAt.value,
        repeatInterval = _repeatInterval.value,
        repeatDays = _repeatDays.value,
        repeatDayOfMonth = repeatDayOfMonth,
        isChecklist = _isChecklist.value,
        viewCount = existingNote.viewCount // viewCount'u koru
      )
      updateNoteUseCase(updatedNote)
      currentNote = updatedNote
      if (updatedNote.reminderAt != null) {
        reminderManager.scheduleReminder(updatedNote)
      } else {
        reminderManager.cancelReminder(updatedNote.id)
      }
    } else {
      // Yeni not
      val newNote = Note(
        title = _title.value.trim(),
        content = finalContent,
        color = _selectedColor.value,
        category = _selectedCategory.value,
        isPinned = _isPinned.value,
        isLocked = _isLocked.value,
        reminderAt = _reminderAt.value,
        repeatInterval = _repeatInterval.value,
        repeatDays = _repeatDays.value,
        repeatDayOfMonth = repeatDayOfMonth,
        isChecklist = _isChecklist.value
      )
      val result = addNoteUseCase(newNote)
      result.onSuccess { generatedId ->
        // Sonraki kayıtlar yeni kopya eklemek yerine bu notu güncellesin
        currentNote = newNote.copy(id = generatedId)
        if (newNote.reminderAt != null) {
          reminderManager.scheduleReminder(newNote.copy(id = generatedId))
        }
      }
    }

    return SaveResult.SAVED
  }

  fun deleteNote() {
    viewModelScope.launch {
      // Yeni not arka planda kaydedilmiş olabilir; onun id'si currentNote'ta
      val savedId = currentNote?.id ?: noteId
      if (savedId > 0L) {
        reminderManager.cancelReminder(savedId)
        deleteNoteUseCase(savedId)
      }
      _events.emit(NoteDetailEvent.NavigateBack(showSavedMessage = false))
    }
  }

  private enum class SaveResult { SAVED, UNCHANGED, EMPTY }
}

sealed class NoteDetailEvent {
  data class NavigateBack(val showSavedMessage: Boolean = false) : NoteDetailEvent()
  data class ShowError(val message: String) : NoteDetailEvent()
}
