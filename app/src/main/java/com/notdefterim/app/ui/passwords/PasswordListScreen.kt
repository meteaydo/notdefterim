package com.notdefterim.app.ui.passwords

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.notdefterim.app.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.domain.model.Password
import com.notdefterim.app.service.FloatingPasswordService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordListScreen(
  modifier: Modifier = Modifier,
  onNavigateBack: (() -> Unit)? = null,
  hideTopBar: Boolean = false,
  viewModel: PasswordListViewModel = hiltViewModel()
) {
  val passwords by viewModel.passwords.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val copiedState by viewModel.copiedState.collectAsStateWithLifecycle()

  var showAddDialog by remember { mutableStateOf(false) }
  var passwordToEdit by remember { mutableStateOf<Password?>(null) }
  var passwordToDelete by remember { mutableStateOf<Password?>(null) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      if (!hideTopBar) {
        TopAppBar(
          title = { Text(stringResource(R.string.passwords_title)) },
          navigationIcon = {
            if (onNavigateBack != null) {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_button_desc))
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
          )
        )
      }
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
      ) {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_password_desc))
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = paddingValues.calculateTopPadding())
        .imePadding()
        .navigationBarsPadding()
    ) {
      // Arama Çubuğu
      OutlinedTextField(
        value = searchQuery,
        onValueChange = viewModel::onSearchQueryChange,
        placeholder = { Text(stringResource(R.string.search_passwords)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search_desc))
            }
          }
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
          .fillMaxWidth()
          .alpha(0.7f)
          .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .padding(bottom = 12.dp)
          .alpha(0.6f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Rounded.SwipeRight,
          contentDescription = stringResource(R.string.swipe_to_edit_desc),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )

        Icon(
          imageVector = Icons.Rounded.TouchApp,
          contentDescription = stringResource(R.string.tap_to_copy_desc),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp)
        )

        Icon(
          imageVector = Icons.Rounded.SwipeLeft,
          contentDescription = stringResource(R.string.swipe_to_delete_desc),
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(16.dp)
        )
      }

      Box(modifier = Modifier.weight(1f)) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(passwords, key = { it.id }) { password ->
            PasswordCard(
              password = password,
              copiedState = copiedState,
              onCopy = { field -> viewModel.onPasswordCopied(password, field) },
              onEdit = { passwordToEdit = password },
              onDelete = { passwordToDelete = password }
            )
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddPasswordDialog(
      onDismiss = { showAddDialog = false },
      onSave = { platform, user, pass ->
        viewModel.savePassword(platform, user, pass)
        showAddDialog = false
      }
    )
  }

  if (passwordToEdit != null) {
    EditPasswordDialog(
      password = passwordToEdit!!,
      onDismiss = { passwordToEdit = null },
      onSave = { updatedPassword ->
        viewModel.updatePassword(updatedPassword)
        passwordToEdit = null
      }
    )
  }

  if (passwordToDelete != null) {
    AlertDialog(
      onDismissRequest = { passwordToDelete = null },
      title = { Text(stringResource(R.string.delete_password_title)) },
      text = { Text(stringResource(R.string.delete_password_title)) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deletePassword(passwordToDelete!!)
            passwordToDelete = null
          }
        ) {
          Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { passwordToDelete = null }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PasswordCard(
  password: Password,
  copiedState: Pair<Long, CopiedField>?,
  onCopy: (CopiedField) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  var passwordVisible by remember { mutableStateOf(false) }
  var showMenu by remember { mutableStateOf(false) }
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  val isThisCardCopied = copiedState?.first == password.id
  val copiedField = if (isThisCardCopied) copiedState?.second else CopiedField.NONE

  val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
    confirmValueChange = { dismissValue ->
      when (dismissValue) {
        androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> {
          onEdit()
          false
        }
        androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> {
          onDelete()
          false
        }
        else -> false
      }
    }
  )

  androidx.compose.material3.SwipeToDismissBox(
    state = dismissState,
    backgroundContent = {
      val direction = dismissState.dismissDirection
      val color = when (direction) {
        androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
        androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
        else -> Color.Transparent
      }
      val alignment = when (direction) {
        androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
      }
      val icon = when (direction) {
        androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> Icons.Rounded.Edit
        androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Delete
        else -> Icons.Rounded.Delete
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(MaterialTheme.shapes.medium)
          .background(color)
          .padding(horizontal = 20.dp),
        contentAlignment = alignment
      ) {
        if (direction != androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
          Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
      }
    },
    content = {
      val borderModifier = if (isThisCardCopied) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium) else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
      val cardColor = if (isThisCardCopied) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

      ElevatedCard(
        modifier = Modifier
          .fillMaxWidth()
          .then(borderModifier),
        colors = CardDefaults.elevatedCardColors(
          containerColor = cardColor
        )
      ) {
        Box(
          modifier = Modifier.combinedClickable(
            onClick = {}, // Required for combinedClickable to capture long clicks properly without disabling inner clickable
            onLongClick = { showMenu = true }
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = password.platformName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
              )
              
              IconButton(
                onClick = {
                  if (!Settings.canDrawOverlays(context)) {
                    Toast.makeText(context, context.getString(R.string.overlay_permission_request), Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    context.startActivity(intent)
                  } else {
                    com.notdefterim.app.service.FloatingWidgetManager.show(
                      context = context,
                      platform = password.platformName,
                      username = password.username,
                      passwordValue = password.passwordValue
                    )
                  }
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = stringResource(R.string.float_window_desc), tint = MaterialTheme.colorScheme.primary)
              }
            }
            
            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Kullanıcı Adı (Tıklanabilir)
              Column(
                modifier = Modifier
                  .weight(1f)
                  .combinedClickable(
                    onClick = {
                      clipboardManager.setText(AnnotatedString(password.username))
                      onCopy(CopiedField.USERNAME)
                      Toast.makeText(context, context.getString(R.string.username_copied), Toast.LENGTH_SHORT).show()
                    },
                    onLongClick = { showMenu = true }
                  )
                  .padding(vertical = 2.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = stringResource(R.string.username),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  if (copiedField == CopiedField.USERNAME) {
                    Icon(
                      Icons.Rounded.CheckCircle,
                      contentDescription = stringResource(R.string.copied_desc),
                      modifier = Modifier.size(14.dp).padding(start = 4.dp),
                      tint = MaterialTheme.colorScheme.primary
                    )
                  }
                }
                Text(
                  text = password.username,
                  style = if (password.username.length > 18) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                  color = if (copiedField == CopiedField.USERNAME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
              }

              // Parola (Tıklanabilir ve İkonlu)
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
              ) {
                Column(
                  modifier = Modifier
                    .combinedClickable(
                      onClick = {
                        clipboardManager.setText(AnnotatedString(password.passwordValue))
                        onCopy(CopiedField.PASSWORD)
                        Toast.makeText(context, context.getString(R.string.password_copied), Toast.LENGTH_SHORT).show()
                      },
                      onLongClick = { showMenu = true }
                    )
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                  horizontalAlignment = Alignment.End
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (copiedField == CopiedField.PASSWORD) {
                      Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = stringResource(R.string.copied_desc),
                        modifier = Modifier.size(14.dp).padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                      )
                    }
                    Text(
                      text = stringResource(R.string.password),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Text(
                    text = if (passwordVisible) password.passwordValue else "••••••••",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (copiedField == CopiedField.PASSWORD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                  )
                }
                
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                  Icon(
                    imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = if (passwordVisible) stringResource(R.string.hide_desc) else stringResource(R.string.show_desc)
                  )
                }
              }
            }
          }

          if (showMenu) {
            androidx.compose.material3.ModalBottomSheet(
              onDismissRequest = { showMenu = false }
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 32.dp, top = 8.dp)
              ) {
                Text(
                  text = stringResource(R.string.password_options),
                  style = MaterialTheme.typography.titleMedium,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                  color = MaterialTheme.colorScheme.primary
                )
                androidx.compose.material3.ListItem(
                  headlineContent = { Text(stringResource(R.string.edit)) },
                  leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                  modifier = Modifier.clickable {
                    showMenu = false
                    onEdit()
                  }
                )
                androidx.compose.material3.ListItem(
                  headlineContent = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                  leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                  modifier = Modifier.clickable {
                    showMenu = false
                    onDelete()
                  }
                )
              }
            }
          }
        }
      }
    }
  )
}

@Composable
fun AddPasswordDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, String) -> Unit
) {
  var platform by remember { mutableStateOf("") }
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.add_new_password_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = platform,
          onValueChange = { platform = it },
          label = { Text(stringResource(R.string.platform)) },
          singleLine = true
        )
        OutlinedTextField(
          value = username,
          onValueChange = { username = it },
          label = { Text(stringResource(R.string.username)) },
          singleLine = true
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text(stringResource(R.string.password)) },
          singleLine = true
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (platform.isNotBlank() || username.isNotBlank() || password.isNotBlank()) {
            val finalPlatform = platform.ifBlank { context.getString(R.string.unnamed_record) }
            onSave(finalPlatform, username, password)
          }
        }
      ) {
        Text(stringResource(R.string.save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
      }
    }
  )
}

@Composable
fun EditPasswordDialog(
  password: Password,
  onDismiss: () -> Unit,
  onSave: (Password) -> Unit
) {
  var platform by remember { mutableStateOf(password.platformName) }
  var username by remember { mutableStateOf(password.username) }
  var pass by remember { mutableStateOf(password.passwordValue) }
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.edit_password_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = platform,
          onValueChange = { platform = it },
          label = { Text(stringResource(R.string.platform)) },
          singleLine = true
        )
        OutlinedTextField(
          value = username,
          onValueChange = { username = it },
          label = { Text(stringResource(R.string.username)) },
          singleLine = true
        )
        OutlinedTextField(
          value = pass,
          onValueChange = { pass = it },
          label = { Text(stringResource(R.string.password)) },
          singleLine = true
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (platform.isNotBlank() || username.isNotBlank() || pass.isNotBlank()) {
            val finalPlatform = platform.ifBlank { context.getString(R.string.unnamed_record) }
            onSave(password.copy(platformName = finalPlatform, username = username, passwordValue = pass))
          }
        }
      ) {
        Text(stringResource(R.string.update))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
      }
    }
  )
}
