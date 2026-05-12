package com.notdefterim.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdefterim.app.data.remote.GoogleAuthManager
import com.notdefterim.app.data.remote.GoogleAuthState
import com.notdefterim.app.data.remote.BackupInfo
import com.notdefterim.app.domain.usecase.backup.BackupToCloudUseCase
import com.notdefterim.app.domain.usecase.backup.GetCloudBackupsUseCase
import com.notdefterim.app.domain.usecase.backup.RestoreFromCloudUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.notdefterim.app.data.repository.LocalBackupManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import android.net.Uri
import java.io.File
import com.notdefterim.app.R

@HiltViewModel
class SettingsViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val googleAuthManager: GoogleAuthManager,
  private val backupToCloudUseCase: BackupToCloudUseCase,
  private val restoreFromCloudUseCase: RestoreFromCloudUseCase,
  private val getCloudBackupsUseCase: GetCloudBackupsUseCase,
  private val localBackupManager: LocalBackupManager
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

  private val _cloudBackups = MutableStateFlow<List<BackupInfo>>(emptyList())
  val cloudBackups: StateFlow<List<BackupInfo>> = _cloudBackups.asStateFlow()

  private val _lastBackupTime = MutableStateFlow<Long?>(null)
  val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

  private val _events = MutableSharedFlow<SettingsEvent>()
  val events = _events.asSharedFlow()

  init {
    viewModelScope.launch {
      googleAuthManager.checkSignInStatus()
    }
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
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.backup_successful)))
        },
        onFailure = { error ->
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.backup_failed, error.localizedMessage)))
        }
      )
      _isBackupLoading.value = false
    }
  }

  fun loadCloudBackups() {
    viewModelScope.launch {
      _isRestoreLoading.value = true
      getCloudBackupsUseCase().fold(
        onSuccess = { backups ->
          _cloudBackups.value = backups
          _events.emit(SettingsEvent.ShowBackupListDialog)
        },
        onFailure = { error ->
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.backup_list_failed, error.localizedMessage)))
        }
      )
      _isRestoreLoading.value = false
    }
  }

  fun restoreFromCloud(backupId: String) {
    viewModelScope.launch {
      _isRestoreLoading.value = true
      restoreFromCloudUseCase(backupId).fold(
        onSuccess = { count ->
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.restore_successful, count)))
        },
        onFailure = { error ->
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.restore_failed, error.localizedMessage)))
        }
      )
      _isRestoreLoading.value = false
    }
  }
  fun createBackupFile(password: String, hint: String) {
    viewModelScope.launch {
      _isBackupLoading.value = true
      localBackupManager.exportToFile(password, hint).fold(
        onSuccess = { file ->
          _events.emit(SettingsEvent.ShowShareSheet(file))
        },
        onFailure = { error ->
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.file_creation_failed, error.localizedMessage)))
        }
      )
      _isBackupLoading.value = false
    }
  }

  fun exportToDevice(uri: Uri, password: String, hint: String) {
    viewModelScope.launch {
      _isBackupLoading.value = true
      localBackupManager.exportToUri(uri, password, hint).fold(
        onSuccess = {
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.saved_to_device_successfully)))
        },
        onFailure = { error ->
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.save_failed, error.localizedMessage)))
        }
      )
      _isBackupLoading.value = false
    }
  }

  fun getBackupHintAndRequestImport(uri: Uri) {
    viewModelScope.launch {
      localBackupManager.getHintFromUri(uri).fold(
        onSuccess = { hint ->
          _events.emit(SettingsEvent.ShowImportPasswordDialog(uri, hint))
        },
        onFailure = { error ->
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.file_read_failed, error.localizedMessage)))
        }
      )
    }
  }

  fun importFromDevice(uri: Uri, password: String) {
    viewModelScope.launch {
      _isRestoreLoading.value = true
      localBackupManager.importFromUri(uri, password).fold(
        onSuccess = {
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.restore_restart_message)))
        },
        onFailure = { error ->
          _events.emit(SettingsEvent.ShowMessage(context.getString(R.string.restore_failed, error.localizedMessage)))
        }
      )
      _isRestoreLoading.value = false
    }
  }
}

sealed class SettingsEvent {
  data class ShowMessage(val message: String) : SettingsEvent()
  data class LaunchSignIn(val intent: android.content.Intent) : SettingsEvent()
  data class ShowImportPasswordDialog(val uri: Uri, val hint: String) : SettingsEvent()
  data class ShowShareSheet(val file: File) : SettingsEvent()
  object ShowBackupListDialog : SettingsEvent()
}
