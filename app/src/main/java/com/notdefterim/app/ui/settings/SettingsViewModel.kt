package com.notdefterim.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdefterim.app.data.remote.GoogleAuthManager
import com.notdefterim.app.data.remote.GoogleAuthState
import com.notdefterim.app.domain.usecase.backup.BackupToCloudUseCase
import com.notdefterim.app.domain.usecase.backup.RestoreFromCloudUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
  private val googleAuthManager: GoogleAuthManager,
  private val backupToCloudUseCase: BackupToCloudUseCase,
  private val restoreFromCloudUseCase: RestoreFromCloudUseCase
) : ViewModel() {

  val googleAuthState: StateFlow<GoogleAuthState> = googleAuthManager.authState
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = GoogleAuthState.SignedOut
    )

  private val _isBackupLoading = MutableStateFlow(false)
  val isBackupLoading: StateFlow<Boolean> = _isBackupLoading.asStateFlow()

  private val _isRestoreLoading = MutableStateFlow(false)
  val isRestoreLoading: StateFlow<Boolean> = _isRestoreLoading.asStateFlow()

  private val _lastBackupTime = MutableStateFlow<Long?>(null)
  val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

  private val _events = MutableSharedFlow<SettingsEvent>()
  val events = _events.asSharedFlow()

  init {
    googleAuthManager.checkSignInStatus()
  }

  fun onGoogleSignInResult(data: android.content.Intent?) {
    viewModelScope.launch {
      googleAuthManager.handleSignInResult(data)
    }
  }

  fun signOut() {
    viewModelScope.launch {
      googleAuthManager.signOut()
    }
  }

  fun backupNow() {
    viewModelScope.launch {
      _isBackupLoading.value = true
      backupToCloudUseCase().fold(
        onSuccess = {
          _lastBackupTime.value = System.currentTimeMillis()
          _events.emit(SettingsEvent.ShowMessage("Yedekleme başarılı"))
        },
        onFailure = { error ->
          _events.emit(SettingsEvent.ShowMessage("Yedekleme başarısız: ${error.localizedMessage}"))
        }
      )
      _isBackupLoading.value = false
    }
  }

  fun restoreFromCloud() {
    viewModelScope.launch {
      _isRestoreLoading.value = true
      restoreFromCloudUseCase().fold(
        onSuccess = { count ->
          _events.emit(SettingsEvent.ShowMessage("$count not geri yüklendi"))
        },
        onFailure = { error ->
          _events.emit(SettingsEvent.ShowMessage("Geri yükleme başarısız: ${error.localizedMessage}"))
        }
      )
      _isRestoreLoading.value = false
    }
  }
}

sealed class SettingsEvent {
  data class ShowMessage(val message: String) : SettingsEvent()
  data class LaunchSignIn(val intent: android.content.Intent) : SettingsEvent()
}
