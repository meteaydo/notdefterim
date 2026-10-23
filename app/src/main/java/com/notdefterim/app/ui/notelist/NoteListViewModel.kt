package com.notdefterim.app.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdefterim.app.data.local.AppPreferences
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.model.NoteSortOrder
import com.notdefterim.app.domain.model.Category
import com.notdefterim.app.domain.model.DefaultNoteCategory
import com.notdefterim.app.domain.repository.CategoryRepository
import com.notdefterim.app.domain.usecase.category.GetCategoriesUseCase
import com.notdefterim.app.domain.usecase.note.DeleteNoteUseCase
import com.notdefterim.app.domain.usecase.note.GetNotesUseCase
import com.notdefterim.app.domain.usecase.note.UpdateNoteUseCase
import com.notdefterim.app.service.ReminderManager
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
import java.time.LocalDateTime
import javax.inject.Inject
import com.notdefterim.app.util.matchesSearch

@HiltViewModel
class NoteListViewModel @Inject constructor(
  private val getNotesUseCase: GetNotesUseCase,
  private val deleteNoteUseCase: DeleteNoteUseCase,
  private val updateNoteUseCase: UpdateNoteUseCase,
  private val getCategoriesUseCase: GetCategoriesUseCase,
  private val categoryRepository: CategoryRepository,
  private val reminderManager: ReminderManager,
  private val appPreferences: AppPreferences
) : ViewModel() {

  enum class FilterType { ALL, LOCKED, FREQUENT, CATEGORY, REMINDER }
  
  private val _selectedFilterType = MutableStateFlow(FilterType.ALL)
  val selectedFilterType = _selectedFilterType.asStateFlow()

  private val _selectedCategoryId = MutableStateFlow<Long?>(null)
  val selectedCategoryId = _selectedCategoryId.asStateFlow()

  val categories: StateFlow<List<Category>> = getCategoriesUseCase(com.notdefterim.app.domain.model.CategoryType.NOTE)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _isSearchActive = MutableStateFlow(false)
  val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

  /**
   * Spotlight arama: debounce ile gereksiz DB sorgularını önler.
   * 300ms bekledikten sonra query değerlendirmeye alınır.
   */
  @OptIn(FlowPreview::class)
  val notes: StateFlow<List<Note>> = kotlinx.coroutines.flow.combine(
    getNotesUseCase(),
    _searchQuery.debounce(300L),
    _selectedFilterType,
    _selectedCategoryId,
    appPreferences.noteSortOrder
  ) { noteList, query, filterType, categoryId, sortOrder ->
      val filteredList = if (query.isBlank()) {
          noteList
      } else {
          noteList.filter {
              it.title.matchesSearch(query) ||
              it.content.matchesSearch(query)
          }
      }

    // Kullanıcının seçtiği sıra; filtrelerin öne aldığı notların ve sabitlenenlerin içinde uygulanır
    val order = sortOrder.comparator()

    when (filterType) {
      FilterType.ALL -> filteredList.sortedWith(compareByDescending<Note> { it.isPinned }.then(order))
      FilterType.LOCKED -> filteredList.sortedWith(
        compareByDescending<Note> { it.isLocked }
          .thenByDescending { it.isPinned }
          .then(order)
      )
      FilterType.FREQUENT -> filteredList.sortedByDescending { it.viewCount }
      FilterType.REMINDER -> {
        // Önce yaklaşan hatırlatıcılar (en yakını üstte), sonra vakti geçenler (en yenisi üstte), en altta hatırlatıcısızlar
        val now = LocalDateTime.now()
        val (withReminder, without) = filteredList.partition { it.reminderAt != null }
        val (upcoming, past) = withReminder.partition { it.reminderAt!! > now }
        upcoming.sortedBy { it.reminderAt } +
          past.sortedByDescending { it.reminderAt } +
          without.sortedWith(compareByDescending<Note> { it.isPinned }.then(order))
      }
      FilterType.CATEGORY -> {
        if (categoryId != null) {
          // Seçili kategori notlarını EN ÜSTE koy, diğerlerini alta bırak.
          filteredList.sortedWith(
            compareByDescending<Note> { it.category?.id == categoryId }
              .thenByDescending { it.isPinned }
              .then(order)
          )
        } else {
          filteredList.sortedWith(compareByDescending<Note> { it.isPinned }.then(order))
        }
      }
    }
  }.stateIn(
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

  fun setFilter(type: FilterType, categoryId: Long? = null) {
    _selectedFilterType.value = type
    _selectedCategoryId.value = categoryId
  }

  val sortOrder: StateFlow<NoteSortOrder> = appPreferences.noteSortOrder

  fun setSortOrder(order: NoteSortOrder) {
    appPreferences.setNoteSortOrder(order)
  }

  /** Seçili filtreye yeniden dokunulunca "Tümü"ye döner. */
  fun toggleFilter(type: FilterType) {
    setFilter(if (_selectedFilterType.value == type) FilterType.ALL else type)
  }

  fun deleteNote(noteId: Long) {
    viewModelScope.launch {
      reminderManager.cancelReminder(noteId)
      deleteNoteUseCase(noteId)
    }
  }

  fun togglePin(note: Note) {
    viewModelScope.launch {
      updateNoteUseCase(note.copy(isPinned = !note.isPinned))
    }
  }

  /** Kategorinin korumalı olup olmadığını kontrol eder */
  fun isCategoryProtected(category: Category): Boolean =
    DefaultNoteCategory.of(category.name) != null

  /** Kategori adını ve rengini günceller */
  fun updateCategory(category: Category) {
    viewModelScope.launch {
      categoryRepository.updateCategory(category)
    }
  }

  /** Kategoriyi siler. Korumalı kategorileri silmez. */
  fun deleteCategory(category: Category) {
    if (isCategoryProtected(category)) return
    viewModelScope.launch {
      categoryRepository.deleteCategory(category.id)
      // Aktif filtre silinen kategoriyse, filtre sıfırla
      if (_selectedFilterType.value == FilterType.CATEGORY && _selectedCategoryId.value == category.id) {
        _selectedFilterType.value = FilterType.ALL
        _selectedCategoryId.value = null
      }
    }
  }

  /** Yeni kategori ekler */
  fun addCategory(name: String, colorHex: String) {
    viewModelScope.launch {
      categoryRepository.addCategory(Category(name = name, colorHex = colorHex))
    }
  }
}
