package com.notdefterim.app.ui.passwords

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.notdefterim.app.ui.notelist.components.SmartFlowRow
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.notdefterim.app.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.domain.model.Password
import com.notdefterim.app.service.FloatingPasswordService
import com.notdefterim.app.ui.auth.AuthViewModel
import com.notdefterim.app.ui.components.ActionPinDialog
import com.notdefterim.app.ui.components.PasswordGeneratorCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordListScreen(
  modifier: Modifier = Modifier,
  onNavigateBack: (() -> Unit)? = null,
  hideTopBar: Boolean = false,
  initialShowAddDialog: Boolean = false,
  viewModel: PasswordListViewModel = hiltViewModel(),
  authViewModel: AuthViewModel = hiltViewModel()
) {
  val passwords by viewModel.passwords.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val copiedState by viewModel.copiedState.collectAsStateWithLifecycle()
  val frequentUsernames by viewModel.frequentUsernames.collectAsStateWithLifecycle()
  val expiredPasswordAlert by viewModel.expiredPasswordAlert.collectAsStateWithLifecycle()
  val pinPromptState by authViewModel.pinPromptState.collectAsStateWithLifecycle()
  val unlockedPasswords = remember { androidx.compose.runtime.mutableStateListOf<Long>() }
  val categories by viewModel.categories.collectAsStateWithLifecycle()
  val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
  var isCategoriesExpanded by remember { mutableStateOf(false) }

  var showAddDialog by remember { mutableStateOf(initialShowAddDialog) }
  var passwordToEdit by remember { mutableStateOf<Password?>(null) }
  var passwordToDelete by remember { mutableStateOf<Password?>(null) }
  
  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var newCategoryName by remember { mutableStateOf("") }
  val defaultCategoryColors = listOf("#4CAF50", "#2196F3", "#FF9800", "#E91E63", "#9C27B0", "#00BCD4", "#795548")
  var newCategoryColor by remember { mutableStateOf(defaultCategoryColors.first()) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    topBar = {
      if (!hideTopBar) {
        TopAppBar(
          title = { Text(stringResource(R.string.passwords_title)) },
          navigationIcon = {
            if (onNavigateBack != null) {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_button_desc))
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
      AnimatedVisibility(visible = expiredPasswordAlert != null) {
        expiredPasswordAlert?.let { platforms ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
              contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
          ) {
            Row(
              modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = stringResource(R.string.password_expired_alert, platforms),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = { viewModel.dismissCurrentAlerts() }) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = stringResource(R.string.clear_search_desc),
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }

      // Arama Çubuğu
      OutlinedTextField(
        value = searchQuery,
        onValueChange = viewModel::onSearchQueryChange,
        placeholder = { Text("Herhangi Birşey Ara") },
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

      // ── Kategori Filtre Çubuğu ──────────────────────────────────────
      val maxChipsWhenCollapsed = 7
      val visibleCategories = if (isCategoriesExpanded) categories else categories.take(maxChipsWhenCollapsed)
      val showExpandChip = !isCategoriesExpanded && categories.size > maxChipsWhenCollapsed

      SmartFlowRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalSpacing = 8.dp,
        verticalSpacing = 8.dp
      ) {
        // Tümü
        FilterChip(
          selected = selectedCategoryId == null,
          onClick = { viewModel.onCategorySelected(null) },
          label = { Text("Tümü", style = MaterialTheme.typography.labelMedium) },
          modifier = Modifier
            .height(28.dp)
            .alpha(if (selectedCategoryId == null) 1f else 0.4f),
          colors = FilterChipDefaults.filterChipColors(
            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selectedCategoryId == null,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
          )
        )

        // Kategoriler
        visibleCategories.forEach { cat ->
          val isSelected = selectedCategoryId == cat.id
          
          FilterChip(
            selected = isSelected,
            onClick = { 
              if (isSelected) viewModel.onCategorySelected(null)
              else viewModel.onCategorySelected(cat.id)
            },
            label = { 
               Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  CategoryIcon(
                    categoryName = cat.name,
                    tint = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.size(15.dp)
                  )
                  Text(cat.name, style = MaterialTheme.typography.labelMedium)
               }
            },
            modifier = Modifier
              .height(28.dp)
              .alpha(if (isSelected) 1f else 0.4f),
            colors = FilterChipDefaults.filterChipColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              labelColor = MaterialTheme.colorScheme.onSurface,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
              selectedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
              borderWidth = 1.dp,
              selectedBorderWidth = 1.dp
            )
          )
        }

        if (showExpandChip) {
          FilterChip(
            selected = false,
            onClick = { isCategoriesExpanded = true },
            label = { Text("...", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.height(28.dp),
            colors = FilterChipDefaults.filterChipColors(
              labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = false,
              borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
              borderWidth = 1.dp,
            )
          )
        } else if (isCategoriesExpanded && categories.size > maxChipsWhenCollapsed) {
          FilterChip(
            selected = false,
            onClick = { isCategoriesExpanded = false },
            label = { Text("Gizle", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.height(28.dp),
            colors = FilterChipDefaults.filterChipColors(
              labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = false,
              borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
              borderWidth = 1.dp,
            )
          )
        }

        // Yeni kategori ekleme butonu
        androidx.compose.material3.InputChip(
          selected = false,
          onClick = {
            newCategoryName = ""
            newCategoryColor = defaultCategoryColors.first()
            showAddCategoryDialog = true
          },
          label = { Text("+ Kategori Ekle", style = MaterialTheme.typography.labelMedium) },
          modifier = Modifier.height(28.dp).alpha(0.4f)
        )
      }

      Box(modifier = Modifier.weight(1f)) {
        // Kaydırma geçiş efekti (sabit alanın alt yüzeyinin erimesi)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(
              brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                  androidx.compose.ui.graphics.Color.Transparent
                )
              )
            )
            .align(Alignment.TopCenter)
            .zIndex(1f)
        )

        androidx.compose.animation.AnimatedContent(
          targetState = passwords.isEmpty(),
          transitionSpec = { androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut() },
          label = "empty_state_transition"
        ) { isEmptyState ->
          if (isEmptyState) {
            EmptyPasswordsContent(
              selectedCategoryId = selectedCategoryId,
              hasSearchQuery = searchQuery.isNotEmpty()
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              items(passwords, key = { it.id }) { password ->
                PasswordCard(
                  password = password,
                  copiedState = copiedState,
                  isAnyPasswordUnlocked = unlockedPasswords.isNotEmpty(),
                  onUnlockedStateChange = { isUnlocked ->
                    if (isUnlocked) {
                      if (!unlockedPasswords.contains(password.id)) unlockedPasswords.add(password.id)
                    } else {
                      unlockedPasswords.remove(password.id)
                    }
                  },
                  onCopy = { field -> viewModel.onPasswordCopied(password, field) },
                  onEdit = { passwordToEdit = password },
                  onDelete = { passwordToDelete = password },
                  authViewModel = authViewModel
                )
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddPasswordDialog(
      onDismiss = { showAddDialog = false },
      onSave = { platform, user, pass, category ->
        viewModel.savePassword(platform, user, pass, category)
        showAddDialog = false
      },
      frequentUsernames = frequentUsernames,
      categories = categories
    )
  }

  if (passwordToEdit != null) {
    EditPasswordDialog(
      password = passwordToEdit!!,
      onDismiss = { passwordToEdit = null },
      onSave = { updatedPassword ->
        viewModel.updatePassword(updatedPassword)
        passwordToEdit = null
      },
      frequentUsernames = frequentUsernames,
      categories = categories
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

  if (pinPromptState != null) {
    ActionPinDialog(
      pinPromptState = pinPromptState!!,
      onDismiss = { authViewModel.dismissPinPrompt() },
      onVerify = { pin, keep -> authViewModel.verifyActionPin(pin, keep) }
    )
  }

  // ── Yeni Kategori Ekleme Diyalogu ──────────────────────────────────────────
  if (showAddCategoryDialog) {
    AlertDialog(
      onDismissRequest = { showAddCategoryDialog = false },
      title = { Text("Yeni Kategori") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = newCategoryName,
            onValueChange = { newCategoryName = it },
            label = { Text("Kategori Adı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Text("Renk Seçin", style = MaterialTheme.typography.labelMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            defaultCategoryColors.forEach { colorHex ->
              val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .background(color, CircleShape)
                  .then(
                    if (newCategoryColor == colorHex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    else Modifier
                  )
                  .combinedClickable(onClick = { newCategoryColor = colorHex })
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (newCategoryName.isNotBlank()) {
              viewModel.addCategory(newCategoryName.trim(), newCategoryColor)
            }
            showAddCategoryDialog = false
          }
        ) {
          Text("Ekle")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddCategoryDialog = false }) {
          Text("İptal")
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
  isAnyPasswordUnlocked: Boolean,
  onUnlockedStateChange: (Boolean) -> Unit,
  onCopy: (CopiedField) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  authViewModel: AuthViewModel
) {
  var passwordVisible by remember { mutableStateOf(false) }
  var showMenu by remember { mutableStateOf(false) }
  val clipboardManager = LocalClipboard.current
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
    },
    positionalThreshold = { totalDistance -> totalDistance * 0.4f }
  )

  androidx.compose.material3.SwipeToDismissBox(
    state = dismissState,
    modifier = Modifier.fillMaxWidth(),
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

      Box(modifier = Modifier.fillMaxWidth()) {
        val topPadding = if (password.category != null) 12.dp else 0.dp
        
        ElevatedCard(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
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
              .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 0.dp)
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
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = stringResource(R.string.float_window_desc), tint = MaterialTheme.colorScheme.primary)
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
                       clipboardManager.nativeClipboard.setPrimaryClip(
                          android.content.ClipData.newPlainText("", password.username))
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                  )
                  if (copiedField == CopiedField.USERNAME) {
                    Icon(
                      Icons.Rounded.CheckCircle,
                      contentDescription = stringResource(R.string.copied_desc),
                      modifier = Modifier.size(15.dp).padding(start = 4.dp),
                      tint = MaterialTheme.colorScheme.primary
                    )
                  } else {
                    Icon(
                      Icons.Rounded.ContentCopy,
                      contentDescription = "Kullanıcı Adını Kopyala",
                      modifier = Modifier.size(15.dp).padding(start = 4.dp),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
                        val performCopy = {
                          clipboardManager.nativeClipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("", password.passwordValue))
                          onCopy(CopiedField.PASSWORD)
                          Toast.makeText(context, context.getString(R.string.password_copied), Toast.LENGTH_SHORT).show()
                        }

                        if (passwordVisible || isAnyPasswordUnlocked) {
                          performCopy()
                        } else {
                          val activity = context as? androidx.fragment.app.FragmentActivity
                          if (activity != null) {
                              authViewModel.authenticateAction(
                                  activity = activity,
                                  title = context.getString(R.string.app_name),
                                  subtitle = "Uygulama içi pin korumasını açtınız. Parolayı kopyalamak için pini girmeniz gerekiyor.",
                                  targetScope = 2,
                                  onSuccess = { 
                                      performCopy()
                                      passwordVisible = true
                                      onUnlockedStateChange(true)
                                  },
                                  onError = {}
                              )
                          }
                        }
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
                        modifier = Modifier.size(15.dp).padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                      )
                    } else {
                      Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = "Parolayı Kopyala",
                        modifier = Modifier.size(15.dp).padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                      )
                    }
                    Text(
                      text = stringResource(R.string.password),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                  }
                  Text(
                    text = if (passwordVisible) password.passwordValue else "••••••••",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (copiedField == CopiedField.PASSWORD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                  )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                  onClick = {
                    if (!passwordVisible) {
                        if (isAnyPasswordUnlocked) {
                            passwordVisible = true
                            onUnlockedStateChange(true)
                        } else {
                            val activity = context as? androidx.fragment.app.FragmentActivity
                            if (activity != null) {
                                authViewModel.authenticateAction(
                                    activity = activity,
                                    title = context.getString(R.string.app_name),
                                    subtitle = "Uygulama içi pin korumasını açtınız. Parolayı görüntülemek için pini girmeniz gerekiyor.",
                                    targetScope = 2,
                                    onSuccess = { 
                                        passwordVisible = true
                                        onUnlockedStateChange(true)
                                    },
                                    onError = {}
                                )
                            }
                        }
                    } else {
                        passwordVisible = false
                        onUnlockedStateChange(false)
                        authViewModel.lockSession()
                    }
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = if (passwordVisible) stringResource(R.string.hide_desc) else stringResource(R.string.show_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                  )
                }
              }
            }
            
            val isUpdated = password.updatedAt != null
            val displayDate = password.updatedAt ?: password.createdAt
            val dateString = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(displayDate))
            val prefix = if (isUpdated) "Değiştirildi:" else "Oluşturuldu:"
            val timeAgo = getTimeAgoText(displayDate)
            
            Text(
              text = "$prefix $dateString ($timeAgo)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              fontWeight = FontWeight.Normal,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp, top = 6.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
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
      
      if (password.category != null) {
        val catName = password.category.name
        val catColorHex = password.category.colorHex
        val catColor = try { Color(android.graphics.Color.parseColor(catColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

        Box(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .background(MaterialTheme.colorScheme.background, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            .padding(6.dp),
          contentAlignment = Alignment.Center
        ) {
           CategoryIcon(
             categoryName = catName,
             modifier = Modifier.size(15.dp),
             tint = Color.Black.copy(alpha = 0.8f)
           )
        }
      }
      
    }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, String, com.notdefterim.app.domain.model.Category?) -> Unit,
  frequentUsernames: List<String>,
  categories: List<com.notdefterim.app.domain.model.Category>
) {
  var platform by remember { mutableStateOf("") }
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf<com.notdefterim.app.domain.model.Category?>(null) }
  var usernameExpanded by remember { mutableStateOf(false) }
  var categoryExpanded by remember { mutableStateOf(false) }
  var showGenerator by remember { mutableStateOf(false) }
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
        ExposedDropdownMenuBox(
          expanded = usernameExpanded,
          onExpandedChange = { usernameExpanded = !usernameExpanded }
        ) {
          OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username)) },
            singleLine = true,
            modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = usernameExpanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
          )
          if (frequentUsernames.isNotEmpty()) {
            ExposedDropdownMenu(
              expanded = usernameExpanded,
              onDismissRequest = { usernameExpanded = false }
            ) {
              frequentUsernames.forEach { suggestion ->
                DropdownMenuItem(
                  text = { Text(suggestion) },
                  onClick = {
                    username = suggestion
                    usernameExpanded = false
                  }
                )
              }
            }
          }
        }
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text(stringResource(R.string.password)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        
        TextButton(
          onClick = { showGenerator = true },
          modifier = Modifier.align(Alignment.End)
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Şifre Üret")
        }

        if (showGenerator) {
          val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
          ModalBottomSheet(
            onDismissRequest = { showGenerator = false },
            sheetState = sheetState
          ) {
            PasswordGeneratorCard(
              onPasswordGenerated = { generated ->
                password = generated
                showGenerator = false
              },
              modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            )
          }
        }
        
        ExposedDropdownMenuBox(
          expanded = categoryExpanded,
          onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
          OutlinedTextField(
            value = selectedCategory?.name ?: "Kategori Yok",
            onValueChange = { },
            readOnly = true,
            label = { Text("Kategori") },
            modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
          )
          ExposedDropdownMenu(
            expanded = categoryExpanded,
            onDismissRequest = { categoryExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text("Kategori Yok") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.BookmarkBorder,
                  contentDescription = null,
                  modifier = Modifier.size(24.dp)
                )
              },
              onClick = {
                selectedCategory = null
                categoryExpanded = false
              }
            )
            categories.forEach { category ->
              DropdownMenuItem(
                text = { Text(category.name) },
                leadingIcon = {
                  CategoryIcon(
                    categoryName = category.name,
                    tint = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                  )
                },
                onClick = {
                  selectedCategory = category
                  categoryExpanded = false
                }
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (platform.isNotBlank() || username.isNotBlank() || password.isNotBlank()) {
            val finalPlatform = platform.ifBlank { context.getString(R.string.unnamed_record) }
            onSave(finalPlatform, username, password, selectedCategory)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPasswordDialog(
  password: Password,
  onDismiss: () -> Unit,
  onSave: (Password) -> Unit,
  frequentUsernames: List<String>,
  categories: List<com.notdefterim.app.domain.model.Category>
) {
  var platform by remember { mutableStateOf(password.platformName) }
  var username by remember { mutableStateOf(password.username) }
  var pass by remember { mutableStateOf(password.passwordValue) }
  var selectedCategory by remember { mutableStateOf<com.notdefterim.app.domain.model.Category?>(password.category) }
  var usernameExpanded by remember { mutableStateOf(false) }
  var categoryExpanded by remember { mutableStateOf(false) }
  var showGenerator by remember { mutableStateOf(false) }
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
        ExposedDropdownMenuBox(
          expanded = usernameExpanded,
          onExpandedChange = { usernameExpanded = !usernameExpanded }
        ) {
          OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username)) },
            singleLine = true,
            modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = usernameExpanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
          )
          if (frequentUsernames.isNotEmpty()) {
            ExposedDropdownMenu(
              expanded = usernameExpanded,
              onDismissRequest = { usernameExpanded = false }
            ) {
              frequentUsernames.forEach { suggestion ->
                DropdownMenuItem(
                  text = { Text(suggestion) },
                  onClick = {
                    username = suggestion
                    usernameExpanded = false
                  }
                )
              }
            }
          }
        }
        OutlinedTextField(
          value = pass,
          onValueChange = { pass = it },
          label = { Text(stringResource(R.string.password)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        
        TextButton(
          onClick = { showGenerator = true },
          modifier = Modifier.align(Alignment.End)
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Şifre Üret")
        }

        if (showGenerator) {
          val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
          ModalBottomSheet(
            onDismissRequest = { showGenerator = false },
            sheetState = sheetState
          ) {
            PasswordGeneratorCard(
              onPasswordGenerated = { generated ->
                pass = generated
                showGenerator = false
              },
              modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            )
          }
        }
        
        ExposedDropdownMenuBox(
          expanded = categoryExpanded,
          onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
          OutlinedTextField(
            value = selectedCategory?.name ?: "Kategori Yok",
            onValueChange = { },
            readOnly = true,
            label = { Text("Kategori") },
            modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
          )
          ExposedDropdownMenu(
            expanded = categoryExpanded,
            onDismissRequest = { categoryExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text("Kategori Yok") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.BookmarkBorder,
                  contentDescription = null,
                  modifier = Modifier.size(24.dp)
                )
              },
              onClick = {
                selectedCategory = null
                categoryExpanded = false
              }
            )
            categories.forEach { category ->
              DropdownMenuItem(
                text = { Text(category.name) },
                leadingIcon = {
                  CategoryIcon(
                    categoryName = category.name,
                    tint = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                  )
                },
                onClick = {
                  selectedCategory = category
                  categoryExpanded = false
                }
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (platform.isNotBlank() || username.isNotBlank() || pass.isNotBlank()) {
            val finalPlatform = platform.ifBlank { context.getString(R.string.unnamed_record) }
            onSave(password.copy(platformName = finalPlatform, username = username, passwordValue = pass, category = selectedCategory, updatedAt = System.currentTimeMillis()))
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

fun getTimeAgoText(timestamp: Long): String {
  val date = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
  val now = java.time.LocalDate.now()
  val period = java.time.Period.between(date, now)
  
  val years = period.years
  val months = period.months
  val days = period.days

  if (years == 0 && months == 0 && days == 0) {
    return "Bugün"
  }

  val parts = mutableListOf<String>()
  if (years > 0) parts.add("$years yıl")
  if (months > 0) parts.add("$months ay")
  if (days > 0) parts.add("$days gün")
  
  return parts.joinToString(" ") + " önce"
}

@Composable
fun CategoryIcon(categoryName: String, tint: Color, modifier: Modifier = Modifier) {
  val catIcon = when {
    categoryName.equals("Banka", ignoreCase = true) -> Icons.Rounded.AccountBalance
    categoryName.equals("Google", ignoreCase = true) -> androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.ic_google)
    categoryName.equals("S. Medya", ignoreCase = true) -> Icons.Rounded.Share
    categoryName.equals("Resmi", ignoreCase = true) -> Icons.Rounded.Domain
    categoryName.equals("Aile", ignoreCase = true) -> Icons.Rounded.Groups
    else -> Icons.Rounded.Bookmark
  }
  Icon(
    imageVector = catIcon,
    contentDescription = categoryName,
    modifier = modifier,
    tint = tint
  )
}

@Composable
private fun EmptyPasswordsContent(
  selectedCategoryId: Long?,
  hasSearchQuery: Boolean,
  modifier: Modifier = Modifier
) {
  val emptyText = when {
    hasSearchQuery -> "Eşleşen parola bulunamadı"
    selectedCategoryId != null -> "Bu kategoride parola yok"
    else -> "Henüz parola eklenmemiş"
  }
  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = Icons.Rounded.Lock,
      contentDescription = null,
      modifier = Modifier.size(80.dp),
      tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = emptyText,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
  }
}
