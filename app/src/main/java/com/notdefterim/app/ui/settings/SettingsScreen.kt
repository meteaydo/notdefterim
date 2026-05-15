package com.notdefterim.app.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.data.remote.GoogleAuthManager
import com.notdefterim.app.data.remote.GoogleAuthState
import com.notdefterim.app.ui.components.SetupAppPinDialog
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Share
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.notdefterim.app.R
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onNavigateBack: () -> Unit,
  googleAuthManager: GoogleAuthManager,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val googleAuthState by viewModel.googleAuthState.collectAsStateWithLifecycle()
  val isBackupLoading by viewModel.isBackupLoading.collectAsStateWithLifecycle()
  val isRestoreLoading by viewModel.isRestoreLoading.collectAsStateWithLifecycle()
  val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
  val cloudBackups by viewModel.cloudBackups.collectAsStateWithLifecycle()
  val autoLockTimeout by viewModel.autoLockTimeout.collectAsStateWithLifecycle()
  val passwordReminderPeriod by viewModel.passwordReminderPeriod.collectAsStateWithLifecycle()
  val appPin by viewModel.appPin.collectAsStateWithLifecycle()
  val appPinHint by viewModel.appPinHint.collectAsStateWithLifecycle()
  val appPinScope by viewModel.appPinScope.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  // Google Sign-In Activity Launcher
  val signInLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      viewModel.onGoogleSignInResult(result.data)
    }
  }

  var showExportDialog by remember { mutableStateOf(false) }
  var showImportDialog by remember { mutableStateOf<Uri?>(null) }
  var showLanguageDialog by remember { mutableStateOf(false) }
  var showAutoLockDialog by remember { mutableStateOf(false) }
  var showReminderDialog by remember { mutableStateOf(false) }
  var showAppPinDialog by remember { mutableStateOf(false) }
  var showBackupListDialog by remember { mutableStateOf(false) }
  var selectedBackupToRestore by remember { mutableStateOf<com.notdefterim.app.data.remote.BackupInfo?>(null) }
  var dialogPassword by remember { mutableStateOf("") }
  var dialogHint by remember { mutableStateOf("") }

  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
  ) { uri ->
    if (uri != null) {
      viewModel.exportToDevice(uri, dialogPassword, dialogHint)
    }
    // Seçim yapılsa da yapılmasa da diyaloğu kapat
    showExportDialog = false
  }

  val openDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      dialogPassword = ""
      viewModel.getBackupHintAndRequestImport(uri)
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is SettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
        is SettingsEvent.LaunchSignIn -> signInLauncher.launch(event.intent)
        is SettingsEvent.ShowBackupListDialog -> showBackupListDialog = true
        is SettingsEvent.ShowImportPasswordDialog -> {
          dialogHint = event.hint
          showImportDialog = event.uri
        }
        is SettingsEvent.ShowShareSheet -> {
          val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            event.file
          )
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_or_save_backup)))
        }
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings_title)) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_button_desc))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = androidx.compose.ui.graphics.Color.Transparent
        )
      )
    }
  ) { paddingValues ->

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
    ) {

      Spacer(modifier = Modifier.height(8.dp))

      // ── Dil Seçimi Bölümü ────────────────────────────────────────
      SectionTitle(stringResource(R.string.language_section))
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().clickable { showLanguageDialog = true }
      ) {
        ListItem(
          colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
          headlineContent = { Text(stringResource(R.string.language_section)) },
          supportingContent = { Text(stringResource(R.string.language_description)) },
          trailingContent = { Icon(Icons.Rounded.ArrowBack, modifier = androidx.compose.ui.Modifier.rotate(180f), contentDescription = null) }
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // ── Otomatik Kilitleme Bölümü ────────────────────────────────
      SectionTitle(stringResource(R.string.auto_lock_section))
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().clickable { showAutoLockDialog = true }
      ) {
        val currentTimeout = com.notdefterim.app.domain.model.AutoLockTimeout.fromMs(autoLockTimeout)
        ListItem(
          colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
          headlineContent = { Text(stringResource(R.string.auto_lock_section)) },
          supportingContent = { Text(stringResource(currentTimeout.titleResId)) },
          trailingContent = { Icon(Icons.Rounded.ArrowBack, modifier = androidx.compose.ui.Modifier.rotate(180f), contentDescription = null) }
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // ── Parola Değiştirme Tavsiyesi ────────────────────────────────
      SectionTitle(stringResource(R.string.reminder_section))
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().clickable { showReminderDialog = true }
      ) {
        val currentReminder = com.notdefterim.app.domain.model.PasswordUpdateReminderPeriod.fromMs(passwordReminderPeriod)
        ListItem(
          colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
          headlineContent = { Text(stringResource(R.string.reminder_section)) },
          supportingContent = { Text(stringResource(currentReminder.titleResId)) },
          trailingContent = { Icon(Icons.Rounded.ArrowBack, modifier = androidx.compose.ui.Modifier.rotate(180f), contentDescription = null) }
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // ── Uygulama İçi PIN ────────────────────────────────
      SectionTitle(stringResource(R.string.app_pin_title))
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().clickable { showAppPinDialog = true }
      ) {
        ListItem(
          colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
          headlineContent = { Text(stringResource(R.string.app_pin_title)) },
          supportingContent = { 
            Text(stringResource(if (appPin == null) R.string.app_pin_not_set else R.string.app_pin_active)) 
          },
          trailingContent = { Icon(Icons.Rounded.ArrowBack, modifier = androidx.compose.ui.Modifier.rotate(180f), contentDescription = null) }
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // ── Google Hesabı Bölümü ─────────────────────────────────────
      SectionTitle(stringResource(R.string.google_account_section))

      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        when (val state = googleAuthState) {
          is GoogleAuthState.SignedIn -> {
            ListItem(
              headlineContent = {
                Text(state.account.displayName ?: stringResource(R.string.guest_user))
              },
              supportingContent = {
                Text(state.account.email ?: "")
              },
              leadingContent = {
                Icon(Icons.Rounded.Person, contentDescription = null)
              },
              trailingContent = {
                OutlinedButton(
                  onClick = viewModel::signOut,
                  colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                  )
                ) {
                  Icon(
                    Icons.Rounded.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(stringResource(R.string.logout))
                }
              }
            )
          }
          is GoogleAuthState.SignedOut -> {
            ListItem(
              headlineContent = { Text(stringResource(R.string.login_with_google)) },
              supportingContent = { Text(stringResource(R.string.login_for_drive_backup)) },
              leadingContent = {
                Icon(Icons.Rounded.CloudOff, contentDescription = null)
              },
              trailingContent = {
                Button(
                  onClick = {
                    signInLauncher.launch(googleAuthManager.getSignInIntent())
                  }
                ) {
                  Text(stringResource(R.string.login))
                }
              }
            )
          }
          is GoogleAuthState.Error -> {
            ListItem(
              headlineContent = { Text(stringResource(R.string.login_error)) },
              supportingContent = { Text(state.message) },
              leadingContent = {
                Icon(
                  Icons.Rounded.CloudOff,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error
                )
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // ── Google Drive Yedekleme Bölümü ───────────────────────────
      SectionTitle(stringResource(R.string.drive_backup_section))

      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {

          // Son yedekleme zamanı
          if (lastBackupTime != null) {
            val dateStr = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
              .format(Date(lastBackupTime!!))
            Text(
              text = "Son yedekleme: $dateStr",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
          }

          // Şimdi Yedekle butonu
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Button(
              onClick = viewModel::backupNow,
              enabled = googleAuthState is GoogleAuthState.SignedIn && !isBackupLoading,
              modifier = Modifier.weight(1f)
            ) {
              if (isBackupLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
              } else {
                Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
              }
              Spacer(modifier = Modifier.size(8.dp))
              Text(stringResource(R.string.backup_now))
            }

            OutlinedButton(
              onClick = viewModel::loadCloudBackups,
              enabled = googleAuthState is GoogleAuthState.SignedIn && !isRestoreLoading,
              modifier = Modifier.weight(1f)
            ) {
              if (isRestoreLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp
                )
              } else {
                Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              }
              Spacer(modifier = Modifier.size(8.dp))
              Text(stringResource(R.string.restore_from_backup))
            }
          }

          if (googleAuthState !is GoogleAuthState.SignedIn) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.login_required_for_backup),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // ── Cihaza Yedekle Bölümü ───────────────────────────────────
      SectionTitle(stringResource(R.string.local_backup_section))

      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = stringResource(R.string.local_backup_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Button(
              onClick = { 
                dialogPassword = ""
                dialogHint = ""
                showExportDialog = true 
              },
              enabled = !isBackupLoading,
              modifier = Modifier.weight(1f)
            ) {
              if (isBackupLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.size(8.dp))
              } else {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(8.dp))
              }
              Text(stringResource(R.string.export_backup))
            }

            OutlinedButton(
              onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.size(8.dp))
              Text(stringResource(R.string.import_backup))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // ── Bilgi Bölümü ─────────────────────────────────────────────
      SectionTitle(stringResource(R.string.security_info_section))

      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          InfoRow(label = stringResource(R.string.encryption), value = "AES-256 (SQLCipher)")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = stringResource(R.string.key_storage), value = "Android Keystore")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = stringResource(R.string.drive_backup), value = "App Data Folder (gizli)")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = stringResource(R.string.version), value = "1.0.0")
        }
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }

  // ── Dialogs ──────────────────────────────────────────────────
  if (showExportDialog) {
    AlertDialog(
      onDismissRequest = { showExportDialog = false },
      title = { Text(stringResource(R.string.encrypt_backup_title)) },
      text = {
        Column {
          Text(stringResource(R.string.encrypt_backup_desc))
          Spacer(Modifier.height(16.dp))
          OutlinedTextField(
            value = dialogPassword,
            onValueChange = { dialogPassword = it },
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(Modifier.height(8.dp))
          OutlinedTextField(
            value = dialogHint,
            onValueChange = { dialogHint = it },
            label = { Text(stringResource(R.string.hint_optional)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              if (dialogPassword.isNotBlank()) {
                val dateFormat = android.text.format.DateFormat.format("dd.MM.yyyy", java.util.Date())
                createDocumentLauncher.launch("NotDefterim Yedek ($dateFormat).notdefterim")
              }
            },
            enabled = dialogPassword.isNotBlank(),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.save))
          }
          
          Button(
            onClick = {
              if (dialogPassword.isNotBlank()) {
                viewModel.createBackupFile(dialogPassword, dialogHint)
                showExportDialog = false
              }
            },
            enabled = dialogPassword.isNotBlank(),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.share))
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showExportDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }

  if (showImportDialog != null) {
    AlertDialog(
      onDismissRequest = { showImportDialog = null },
      title = { Text(stringResource(R.string.decrypt_backup_title)) },
      text = {
        Column {
          Text(stringResource(R.string.decrypt_backup_desc))
          if (dialogHint.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.hint_format, dialogHint), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
          }
          Spacer(Modifier.height(16.dp))
          OutlinedTextField(
            value = dialogPassword,
            onValueChange = { dialogPassword = it },
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (dialogPassword.isNotBlank()) {
              viewModel.importFromDevice(showImportDialog!!, dialogPassword)
              showImportDialog = null
            }
          },
          enabled = dialogPassword.isNotBlank()
        ) {
          Text(stringResource(R.string.import_button))
        }
      },
      dismissButton = {
        TextButton(onClick = { showImportDialog = null }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }

  if (showLanguageDialog) {
    val languages = listOf("tr" to "Türkçe", "en" to "English", "ru" to "Русский", "es" to "Español", "de" to "Deutsch", "fr" to "Français")
    val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.language ?: java.util.Locale.getDefault().language

    AlertDialog(
      onDismissRequest = { showLanguageDialog = false },
      title = { Text(stringResource(R.string.language_section)) },
      text = {
        Column {
          languages.forEach { (tag, name) ->
            TextButton(
              onClick = {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                showLanguageDialog = false
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(name)
                if (currentLocale.startsWith(tag)) {
                  Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showLanguageDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }

  if (showAutoLockDialog) {
    val timeouts = com.notdefterim.app.domain.model.AutoLockTimeout.entries

    AlertDialog(
      onDismissRequest = { showAutoLockDialog = false },
      title = { Text(stringResource(R.string.auto_lock_section)) },
      text = {
        Column {
          Text(stringResource(R.string.auto_lock_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.height(8.dp))
          timeouts.forEach { timeout ->
            TextButton(
              onClick = {
                viewModel.setAutoLockTimeout(timeout.timeoutMs)
                showAutoLockDialog = false
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(stringResource(timeout.titleResId))
                if (autoLockTimeout == timeout.timeoutMs) {
                  Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showAutoLockDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }

  if (showReminderDialog) {
    val reminders = com.notdefterim.app.domain.model.PasswordUpdateReminderPeriod.entries

    AlertDialog(
      onDismissRequest = { showReminderDialog = false },
      title = { Text(stringResource(R.string.reminder_section)) },
      text = {
        Column {
          Text(stringResource(R.string.reminder_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.height(8.dp))
          reminders.forEach { reminder ->
            TextButton(
              onClick = {
                viewModel.setPasswordReminderPeriod(reminder.periodMs)
                showReminderDialog = false
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(stringResource(reminder.titleResId))
                if (passwordReminderPeriod == reminder.periodMs) {
                  Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showReminderDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }

  if (showBackupListDialog) {
    AlertDialog(
      onDismissRequest = { showBackupListDialog = false },
      title = { Text(stringResource(R.string.select_backup_title)) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (cloudBackups.isEmpty()) {
            Text(stringResource(R.string.no_notes_yet))
          } else {
            cloudBackups.forEach { backup ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clickable {
                    selectedBackupToRestore = backup
                    showBackupListDialog = false
                  },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  val dateStr = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
                    .format(Date(backup.createdTimeMillis))
                  val sizeKb = backup.sizeBytes / 1024
                  Text(text = dateStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                  Text(text = "Boyut: $sizeKb KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showBackupListDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }

  if (selectedBackupToRestore != null) {
    AlertDialog(
      onDismissRequest = { selectedBackupToRestore = null },
      title = { Text(stringResource(R.string.confirm_restore_title)) },
      text = { Text(stringResource(R.string.confirm_restore_desc)) },
      confirmButton = {
        Button(onClick = {
          viewModel.restoreFromCloud(selectedBackupToRestore!!.id)
          selectedBackupToRestore = null
        }) {
          Text(stringResource(R.string.restore_from_backup))
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedBackupToRestore = null }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }

  // Uygulama İçi PIN Ayarlama Dialog'u
  if (showAppPinDialog) {
    SetupAppPinDialog(
      appPin = appPin,
      appPinHint = appPinHint,
      appPinScope = appPinScope,
      onDismiss = { showAppPinDialog = false },
      onSave = { newPin, newHint, newScope -> 
        viewModel.setAppPin(newPin, newHint) 
        viewModel.setAppPinScope(newScope)
      }
    )
  }
}

@Composable
private fun SectionTitle(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(bottom = 8.dp)
  )
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
