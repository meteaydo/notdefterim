package com.notdefterim.app.ui.passwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdefterim.app.domain.model.Password
import com.notdefterim.app.domain.repository.PasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CopiedField { NONE, USERNAME, PASSWORD }

@HiltViewModel
class PasswordListViewModel @Inject constructor(
  private val repository: PasswordRepository
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  private val _copiedState = MutableStateFlow<Pair<Long, CopiedField>?>(null)
  val copiedState: StateFlow<Pair<Long, CopiedField>?> = _copiedState

  val passwords = combine(
    repository.getAllPasswords(),
    _searchQuery
  ) { list, query ->
    if (query.isBlank()) {
      list
    } else {
      list.filter {
        it.platformName.contains(query, ignoreCase = true) ||
        it.username.contains(query, ignoreCase = true)
      }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query
  }

  fun onPasswordCopied(password: Password, field: CopiedField) {
    _copiedState.value = Pair(password.id, field)
    val updatedPassword = password.copy(usageCount = password.usageCount + 1)
    updatePassword(updatedPassword)
  }

  fun savePassword(platformName: String, username: String, passwordValue: String) {
    viewModelScope.launch {
      repository.insertPassword(
        Password(
          platformName = platformName,
          username = username,
          passwordValue = passwordValue
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
