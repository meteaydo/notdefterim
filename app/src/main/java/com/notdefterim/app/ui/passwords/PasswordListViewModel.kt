package com.notdefterim.app.ui.passwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdefterim.app.domain.model.Password
import com.notdefterim.app.domain.repository.PasswordRepository
import com.notdefterim.app.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.notdefterim.app.util.normalizeForSearch

import com.notdefterim.app.domain.usecase.category.GetCategoriesUseCase
import com.notdefterim.app.domain.model.Category
import com.notdefterim.app.domain.model.CategoryType

enum class CopiedField { NONE, USERNAME, PASSWORD }

@HiltViewModel
class PasswordListViewModel @Inject constructor(
  private val repository: PasswordRepository,
  private val appPreferences: AppPreferences,
  private val getCategoriesUseCase: GetCategoriesUseCase,
  private val categoryRepository: com.notdefterim.app.domain.repository.CategoryRepository
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  private val _copiedState = MutableStateFlow<Pair<Long, CopiedField>?>(null)
  val copiedState: StateFlow<Pair<Long, CopiedField>?> = _copiedState

  private val _selectedCategoryId = MutableStateFlow<Long?>(null)
  val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId

  val categories: StateFlow<List<Category>> = getCategoriesUseCase(CategoryType.PASSWORD)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val passwords = combine(
    repository.getAllPasswords(),
    _searchQuery,
    _selectedCategoryId
  ) { list, query, categoryId ->
    var filteredList = list
    
    // Filtreleme: Kategori
    if (categoryId != null) {
      filteredList = filteredList.filter { it.category?.id == categoryId }
    }
    
    // Filtreleme: Arama
    if (query.isNotBlank()) {
      val normalizedQuery = query.normalizeForSearch()
      filteredList = filteredList.filter {
        it.platformName.normalizeForSearch().contains(normalizedQuery) ||
        it.username.normalizeForSearch().contains(normalizedQuery)
      }
    }
    filteredList
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val frequentUsernames: StateFlow<List<String>> = repository.getAllPasswords()
    .map { list ->
      list.filter { it.username.isNotBlank() }
        .groupingBy { it.username }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { it.key }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val expiredPasswordAlert: StateFlow<String?> = combine(
    repository.getAllPasswords(),
    appPreferences.passwordReminderPeriod,
    appPreferences.dismissedReminders
  ) { list, periodMs, dismissedReminders ->
    if (periodMs == -1L) return@combine null

    val currentTime = System.currentTimeMillis()
    val expiredPlatforms = list.filter { password ->
      val timeToCompare = password.updatedAt ?: password.createdAt
      val key = "${password.id}_${timeToCompare}"
      
      if (dismissedReminders.contains(key)) {
        false
      } else {
        val timeDiff = currentTime - timeToCompare
        timeDiff > periodMs
      }
    }.map { it.platformName }.distinct()

    if (expiredPlatforms.isNotEmpty()) {
      expiredPlatforms.joinToString(", ")
    } else {
      null
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = null
  )

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query
  }

  fun onCategorySelected(categoryId: Long?) {
    _selectedCategoryId.value = categoryId
  }

  fun addCategory(name: String, colorHex: String) {
    viewModelScope.launch {
      categoryRepository.addCategory(Category(name = name, colorHex = colorHex, type = CategoryType.PASSWORD))
    }
  }

  fun dismissCurrentAlerts() {
    viewModelScope.launch {
      val periodMs = appPreferences.passwordReminderPeriod.value
      if (periodMs == -1L) return@launch
      val currentTime = System.currentTimeMillis()
      val dismissed = appPreferences.dismissedReminders.value

      repository.getAllPasswords().first().forEach { password ->
        val timeToCompare = password.updatedAt ?: password.createdAt
        val key = "${password.id}_${timeToCompare}"
        if (!dismissed.contains(key)) {
          val timeDiff = currentTime - timeToCompare
          if (timeDiff > periodMs) {
            appPreferences.dismissReminder(password.id, timeToCompare)
          }
        }
      }
    }
  }

  fun onPasswordCopied(password: Password, field: CopiedField) {
    _copiedState.value = Pair(password.id, field)
    val updatedPassword = password.copy(usageCount = password.usageCount + 1)
    updatePassword(updatedPassword)
  }

  fun savePassword(platformName: String, username: String, passwordValue: String, category: Category?) {
    viewModelScope.launch {
      repository.insertPassword(
        Password(
          platformName = platformName,
          username = username,
          passwordValue = passwordValue,
          category = category
        )
      )
    }
  }

  fun updatePassword(password: Password) {
    viewModelScope.launch {
      repository.updatePassword(password)
    }
  }

  fun deletePassword(password: Password) {
    viewModelScope.launch {
      repository.deletePassword(password)
    }
  }
}
