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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.data.remote.GoogleAuthManager
import com.notdefterim.app.data.remote.GoogleAuthState
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
  val googleAuthState by viewModel.googleAuthState.collectAsStateWithLifecycle()
  val isBackupLoading by viewModel.isBackupLoading.collectAsStateWithLifecycle()
  val isRestoreLoading by viewModel.isRestoreLoading.collectAsStateWithLifecycle()
  val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  // Google Sign-In Activity Launcher
  val signInLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      viewModel.onGoogleSignInResult(result.data)
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is SettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
        is SettingsEvent.LaunchSignIn -> signInLauncher.launch(event.intent)
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Ayarlar") },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
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

      // ── Google Hesabı Bölümü ─────────────────────────────────────
      SectionTitle("Google Hesabı")

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
                Text(state.account.displayName ?: "Kullanıcı")
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
                  Text("Çıkış")
                }
              }
            )
          }
          is GoogleAuthState.SignedOut -> {
            ListItem(
              headlineContent = { Text("Google ile Giriş Yap") },
              supportingContent = { Text("Drive yedekleme için giriş yapın") },
              leadingContent = {
                Icon(Icons.Rounded.CloudOff, contentDescription = null)
              },
              trailingContent = {
                Button(
                  onClick = {
                    signInLauncher.launch(googleAuthManager.getSignInIntent())
                  }
                ) {
                  Text("Giriş")
                }
              }
            )
          }
          is GoogleAuthState.Error -> {
            ListItem(
              headlineContent = { Text("Giriş Hatası") },
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
      SectionTitle("Google Drive Yedekleme")

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
              Text("Şimdi Yedekle")
            }

            OutlinedButton(
              onClick = viewModel::restoreFromCloud,
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
              Text("Geri Yükle")
            }
          }

          if (googleAuthState !is GoogleAuthState.SignedIn) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Yedekleme için Google hesabıyla giriş yapın",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // ── Bilgi Bölümü ─────────────────────────────────────────────
      SectionTitle("Güvenlik Bilgisi")

      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          InfoRow(label = "Şifreleme", value = "AES-256 (SQLCipher)")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = "Anahtar Depolama", value = "Android Keystore")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = "Drive Yedekleme", value = "App Data Folder (gizli)")
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = "Versiyon", value = "1.0.0")
        }
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
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
