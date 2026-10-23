package com.notdefterim.app.ui.notedetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.Button
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import android.app.AlarmManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.width
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.notdefterim.app.ui.components.ActionPinDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.notdefterim.app.R
import com.notdefterim.app.domain.model.DefaultNoteCategory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdefterim.app.domain.model.NoteColor
import com.notdefterim.app.domain.model.RepeatInterval
import com.notdefterim.app.ui.theme.LocalNoteCardColors
import com.notdefterim.app.ui.notelist.components.AddCategoryDialog
import com.notdefterim.app.ui.notedetail.components.ChecklistEditor
import com.notdefterim.app.ui.notedetail.components.ReminderSheet
import com.notdefterim.app.ui.components.reminderTimeText
import com.notdefterim.app.ui.components.repeatText
import com.notdefterim.app.ui.components.SetupAppPinDialog
import androidx.compose.ui.text.style.TextAlign
import com.notdefterim.app.util.searchMatchRanges

/**
 * Not ekleme ve düzenleme ekranı.
 * Renk seçici, sabitleme ve silme işlemleri bu ekranda yapılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
  onNavigateBack: () -> Unit,
  onSaved: () -> Unit = {},
  modifier: Modifier = Modifier,
  viewModel: NoteDetailViewModel = hiltViewModel(),
  authViewModel: com.notdefterim.app.ui.auth.AuthViewModel = hiltViewModel()
) {
  val title by viewModel.title.collectAsStateWithLifecycle()
  val content by viewModel.content.collectAsStateWithLifecycle()
  val selectedColor by viewModel.selectedColor.collectAsStateWithLifecycle()
  val isPinned by viewModel.isPinned.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val isChecklist by viewModel.isChecklist.collectAsStateWithLifecycle()
  val checklistItems by viewModel.checklistItems.collectAsStateWithLifecycle()
  val pinPromptState by authViewModel.pinPromptState.collectAsStateWithLifecycle()
  val appPin by authViewModel.appPin.collectAsStateWithLifecycle()
  val appPinHint by authViewModel.appPinHint.collectAsStateWithLifecycle()
  val appPinScope by authViewModel.appPinScope.collectAsStateWithLifecycle()
  val isPinApplicable = appPin != null && (appPinScope == 0 || appPinScope == 1)
  
  var showSetupPinDialog by remember { mutableStateOf(false) }
  var isCategoriesExpanded by remember { mutableStateOf(false) }
  val reminderAt by viewModel.reminderAt.collectAsStateWithLifecycle()
  val repeatInterval by viewModel.repeatInterval.collectAsStateWithLifecycle()
  val repeatDays by viewModel.repeatDays.collectAsStateWithLifecycle()
  val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
  var isContentVisible by remember(isLocked) { mutableStateOf(!isLocked) }
  
  val categories by viewModel.categories.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  var showAddCategoryDialog by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  var showDeleteDialog by remember { mutableStateOf(false) }
  var showOverlayPermissionDialog by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()
  var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
  var viewportHeight by remember { mutableStateOf(0) }
  var contentOffsetY by remember { mutableStateOf(0f) }

  var contentTextFieldValue by remember { mutableStateOf(TextFieldValue(content)) }
  var isContentFocused by remember { mutableStateOf(false) }
  val cursorMarginPx = with(LocalDensity.current) { 32.dp.toPx() }

  LaunchedEffect(content) {
      if (contentTextFieldValue.text != content) {
          contentTextFieldValue = contentTextFieldValue.copy(text = content)
      }
  }

  // İmleci görünür alanda tut. BasicTextField bunu yalnızca odaklandığı anda yapıyor: o anda klavye
  // henüz açılmamış ve imleç henüz dokunulan yere taşınmamış oluyor. Klavye açılıp görünür alan
  // daraldığında (viewportHeight) ya da imleç yer değiştirdiğinde burada yeniden kaydırılır.
  LaunchedEffect(isContentFocused, contentTextFieldValue.selection, viewportHeight, textLayoutResult) {
    if (!isContentFocused || viewportHeight == 0) return@LaunchedEffect
    val layout = textLayoutResult ?: return@LaunchedEffect
    val cursorOffset = contentTextFieldValue.selection.end.coerceIn(0, layout.layoutInput.text.length)
    val cursorRect = layout.getCursorRect(cursorOffset)
    val cursorTop = contentOffsetY + cursorRect.top
    val cursorBottom = contentOffsetY + cursorRect.bottom
    val visibleTop = scrollState.value
    val visibleBottom = scrollState.value + viewportHeight
    val target = when {
      cursorBottom + cursorMarginPx > visibleBottom -> cursorBottom + cursorMarginPx - viewportHeight
      cursorTop - cursorMarginPx < visibleTop -> cursorTop - cursorMarginPx
      else -> return@LaunchedEffect
    }
    scrollState.scrollTo(target.toInt().coerceAtLeast(0))
  }

  // Arama sorgusu değiştiğinde ilk eşleşmeye scroll yap
  LaunchedEffect(searchQuery) {
    if (searchQuery.isNotBlank()) {
      val index = content.searchMatchRanges(searchQuery).firstOrNull()?.first ?: -1
      if (index >= 0) {
        textLayoutResult?.let { layout ->
          val line = layout.getLineForOffset(index)
          val y = layout.getLineTop(line)
          
          val targetY = if (viewportHeight > 0) {
              val absoluteY = contentOffsetY + y
              // Kelimeyi görünür alanın ortasına hizalamak için:
              absoluteY - (viewportHeight / 2)
          } else {
              contentOffsetY + y
          }
          
          scrollState.animateScrollTo(targetY.toInt().coerceAtLeast(0))
        }
      }
    }
  }

  val primaryContainer = MaterialTheme.colorScheme.primaryContainer
  val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

  // Arama metnini vurgula
  val visualTransformation = remember(searchQuery, primaryContainer, onPrimaryContainer) {
    if (searchQuery.isBlank()) VisualTransformation.None
    else VisualTransformation { text ->
      val annotated = buildAnnotatedString {
        append(text)
        text.toString().searchMatchRanges(searchQuery).forEach { range ->
          addStyle(
            style = SpanStyle(
              background = primaryContainer,
              color = onPrimaryContainer,
              fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            start = range.first,
            end = range.last + 1
          )
        }
      }
      TransformedText(annotated, OffsetMapping.Identity)
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }

  val context = LocalContext.current

  // Tek seferlik olayları dinle ve ilk açılışta klavyeyi gizle
  LaunchedEffect(Unit) {
    focusManager.clearFocus()
    viewModel.events.collect { event ->
      when (event) {
        is NoteDetailEvent.NavigateBack -> {
            if (event.showSavedMessage) {
                onSaved()
            }
            onNavigateBack()
        }
        is NoteDetailEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
      }
    }
  }

  val animatedBackground = MaterialTheme.colorScheme.background

  // context moved above

  // Tarih, saat ve tekrar tek pencerede seçilir
  var showReminderSheet by remember { mutableStateOf(false) }

  // Hatırlatıcının dakikasında gelmesi için "Alarmlar ve hatırlatıcılar" izni gerekir (Android 12+).
  // İzin yoksa önce sorulur; kullanıcı vermezse hatırlatıcı yaklaşık zamanlı kurulur.
  var showExactAlarmDialog by remember { mutableStateOf(false) }
  val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) {
    showReminderSheet = true
  }
  val startReminderFlow = {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
      showExactAlarmDialog = true
    } else {
      showReminderSheet = true
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      startReminderFlow()
    } else {
      coroutineScope.launch {
        snackbarHostState.showSnackbar(context.getString(R.string.reminder_notification_permission_denied))
      }
    }
  }

  // Önce bildirim izni (Android 13+), sonra kesin alarm izni, sonra hatırlatıcı penceresi
  val openReminder = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      startReminderFlow()
    }
  }

  BackHandler {
    viewModel.saveNote()
  }

  // Geri tuşuna basmadan başka uygulamaya geçilirse ya da ekran kapanırsa yazılanlar kaybolmasın
  LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
    viewModel.saveInBackground()
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {},
        navigationIcon = {
          androidx.compose.material3.FilledIconButton(
            onClick = { viewModel.saveNote() },
            modifier = Modifier.padding(start = 8.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.back_button_desc),
              modifier = Modifier.size(28.dp)
            )
          }
        },
        actions = {
          // Alarm
          IconToggleButton(
            checked = reminderAt != null,
            onCheckedChange = { openReminder() },
            colors = IconButtonDefaults.iconToggleButtonColors(
              checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          ) {
            Icon(
              imageVector = if (reminderAt != null) Icons.Rounded.NotificationsActive else Icons.Outlined.Notifications,
              contentDescription = stringResource(R.string.add_reminder)
            )
          }

          // Kilit
          IconToggleButton(
            checked = isLocked,
            onCheckedChange = { isChecked -> 
              viewModel.onLockToggle() 
              if (isChecked) {
                authViewModel.lockSession()
              }
            },
            colors = IconButtonDefaults.iconToggleButtonColors(
              checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          ) {
            Icon(
              imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
              contentDescription = stringResource(if (isLocked) R.string.unlock else R.string.lock)
            )
          }

          // Checklist Toggle
          IconToggleButton(
            checked = isChecklist,
            onCheckedChange = { viewModel.onToggleChecklistMode() },
            colors = IconButtonDefaults.iconToggleButtonColors(
              checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
              contentDescription = stringResource(R.string.checklist_mode)
            )
          }

          // Yüzen Pencerede Aç
          IconButton(onClick = {
            if (!Settings.canDrawOverlays(context)) {
              showOverlayPermissionDialog = true
            } else {
              val displayContent = if (isChecklist) {
                  viewModel.checklistItems.value.joinToString("\n") { (if (it.isChecked) "☑ " else "☐ ") + it.text }
              } else {
                  content
              }
              com.notdefterim.app.service.FloatingNoteManager.show(context, title, displayContent)
            }
          }) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
              contentDescription = stringResource(R.string.open_in_floating_window),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Paylaş
          IconButton(onClick = {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TITLE, title)
              val textToShare = if (title.isNotBlank()) "$title\n\n$content" else content
              putExtra(Intent.EXTRA_TEXT, textToShare)
            }
            context.startActivity(Intent.createChooser(sendIntent, null))
          }) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = stringResource(R.string.share),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Sil
          IconButton(onClick = { showDeleteDialog = true }) {
            Icon(
              imageVector = Icons.Rounded.Delete,
              contentDescription = stringResource(R.string.delete),
              tint = MaterialTheme.colorScheme.error
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent
        )
      )
    }
  ) { paddingValues ->

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = paddingValues.calculateTopPadding())
        .imePadding()
        .navigationBarsPadding()
    ) {

      if (!isContentVisible) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isPinApplicable) {
              Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
              Spacer(modifier = Modifier.height(16.dp))
              Text(stringResource(R.string.note_is_locked), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.height(16.dp))
            } else {
              Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                stringResource(R.string.security_tip_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                stringResource(R.string.locked_note_security_tip),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
              )
              Spacer(modifier = Modifier.height(16.dp))
              Button(onClick = { showSetupPinDialog = true }) {
                Text(stringResource(R.string.set_pin))
              }
              Spacer(modifier = Modifier.height(16.dp))
            }
            
            Button(onClick = { 
              val activity = context as? androidx.fragment.app.FragmentActivity
              if (activity != null) {
                authViewModel.authenticateAction(
                  activity = activity,
                  title = context.getString(R.string.app_name),
                  subtitle = context.getString(R.string.pin_required_view_note),
                  targetScope = 1,
                  onSuccess = { isContentVisible = true },
                  onError = { err -> 
                    coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                  }
                )
              } else {
                isContentVisible = true
              }
            }) {
              Text(stringResource(R.string.show_content))
            }
          }
        }
      } else {
        // ── Sabit Arama Kutusu (Üstte) ──────────────────────────────────────────
      OutlinedTextField(
        value = searchQuery,
        onValueChange = viewModel::onSearchQueryChange,
        placeholder = { Text(stringResource(R.string.search_anything)) },
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

      // ── Kategori Seçimi (SABİT) ────────────────────────────────────────────
      val maxChipsWhenCollapsed = 7
      val visibleCategories = if (isCategoriesExpanded) categories else categories.take(maxChipsWhenCollapsed)
      val showExpandChip = !isCategoriesExpanded && categories.size > maxChipsWhenCollapsed

      @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
      androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        visibleCategories.forEach { cat ->
          val isSelected = selectedCategory?.id == cat.id
          
          androidx.compose.material3.FilterChip(
            selected = isSelected,
            onClick = {
              if (isSelected) viewModel.onCategorySelect(null)
              else viewModel.onCategorySelect(cat)
            },
            label = { 
               Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  when (DefaultNoteCategory.of(cat.name)) {
                      DefaultNoteCategory.SHOPPING -> Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                      DefaultNoteCategory.TODO -> Icon(Icons.AutoMirrored.Rounded.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp))
                      DefaultNoteCategory.IDEAS -> Icon(Icons.Outlined.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                      null -> {}
                  }
                  Text(cat.name, style = MaterialTheme.typography.labelMedium)
               }
            },
            leadingIcon = if (isSelected) { { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
            modifier = Modifier
              .height(28.dp)
              .alpha(if (selectedCategory != null && !isSelected) 0.4f else 1f),
            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              labelColor = MaterialTheme.colorScheme.onSurface,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
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
          androidx.compose.material3.FilterChip(
            selected = false,
            onClick = { isCategoriesExpanded = true },
            label = { Text("...", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.height(28.dp),
            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
              labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            ),
            border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = false,
              borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
              borderWidth = 1.dp,
            )
          )
        } else if (isCategoriesExpanded && categories.size > maxChipsWhenCollapsed) {
          androidx.compose.material3.FilterChip(
            selected = false,
            onClick = { isCategoriesExpanded = false },
            label = { Text(stringResource(R.string.hide_desc), style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.height(28.dp),
            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
              labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            ),
            border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = false,
              borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
              borderWidth = 1.dp,
            )
          )
        }
      }


        reminderAt?.let { at ->
          // Dokununca hatırlatıcı penceresi açılır; vakti geçmişse kırmızı
          val reminderColor = if (at > java.time.LocalDateTime.now()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
          val reminderLabel = listOfNotNull(reminderTimeText(at), repeatText(repeatInterval, repeatDays)).joinToString(" · ")

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f)
                .clickable { openReminder() }
                .padding(vertical = 4.dp)
            ) {
              Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = reminderColor, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = reminderLabel,
                style = MaterialTheme.typography.labelMedium,
                color = reminderColor
              )
            }

            IconButton(onClick = { viewModel.clearReminder() }) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel_reminder), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
          }
        }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
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

        Column(
          modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
              viewportHeight = coordinates.size.height
            }
            .verticalScroll(scrollState)
        ) {

    // Reminder logic moved outside the column


        // ── Başlık ─────────────────────────────────────────────────────
        TextField(
          value = title,
          onValueChange = { newValue ->
              if (newValue.contains("\n")) {
                  focusManager.moveFocus(FocusDirection.Down)
              } else {
                  viewModel.onTitleChange(newValue)
              }
          },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
          keyboardActions = KeyboardActions(
              onNext = { focusManager.moveFocus(FocusDirection.Down) }
          ),
          placeholder = {
            Text(
              stringResource(R.string.title_placeholder),
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
          },
          textStyle = MaterialTheme.typography.headlineSmall,
          colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── İçerik ─────────────────────────────────────────────────────
        if (isChecklist) {
            ChecklistEditor(
                items = checklistItems,
                searchQuery = searchQuery,
                onItemChange = { item -> viewModel.updateChecklistItem(item) },
                onAddItem = { text -> viewModel.addChecklistItem(text) },
                onRemoveItem = { id -> viewModel.removeChecklistItem(id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        contentOffsetY = coordinates.positionInParent().y
                    }
            )
        } else {
            // onTextLayout desteği için BasicTextField kullanıyoruz (scroll kontrolü için)
            BasicTextField(
              value = contentTextFieldValue,
              onValueChange = { newValue ->
                  contentTextFieldValue = newValue
                  viewModel.onContentChange(newValue.text)
              },
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onGloballyPositioned { coordinates ->
                  contentOffsetY = coordinates.positionInParent().y
                }
                .onFocusChanged { isContentFocused = it.isFocused },
              textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground
              ),
              cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              visualTransformation = visualTransformation,
              onTextLayout = { textLayoutResult = it },
              decorationBox = { innerTextField ->
                if (content.isEmpty()) {
                  Text(
                    stringResource(R.string.write_your_note),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                  )
                }
                innerTextField()
              }
            )
        }

        // Kullanıcının sayfanın en altına rahatça kaydırabilmesi ve 
        // son satırı klavyenin üstünde rahatça görebilmesi için boşluk
        Spacer(modifier = Modifier.height(300.dp))
      } // Column bitişi

      // Üst İkon
      val topAlpha by animateFloatAsState(if (scrollState.canScrollBackward) 1f else 0f, label = "topAlpha")
      if (topAlpha > 0f) {
        Box(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp)
            .alpha(topAlpha)
            .background(
              brush = Brush.radialGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                  androidx.compose.ui.graphics.Color.Transparent
                )
              )
            )
            .padding(8.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = stringResource(R.string.more_notes_above),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Alt İkon
      val bottomAlpha by animateFloatAsState(if (scrollState.canScrollForward) 1f else 0f, label = "bottomAlpha")
      if (bottomAlpha > 0f) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 8.dp)
            .alpha(bottomAlpha)
            .background(
              brush = Brush.radialGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                  androidx.compose.ui.graphics.Color.Transparent
                )
              )
            )
            .padding(8.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = stringResource(R.string.more_notes_below),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      } // Box bitişi
      } // else bitişi

    }
  }

  if (showAddCategoryDialog) {
    AddCategoryDialog(
      onDismiss = { showAddCategoryDialog = false },
      onAddCategory = { name, hex ->
        viewModel.addNewCategory(name, hex)
      }
    )
  }

  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text(stringResource(R.string.delete_note_title)) },
      text = { Text(stringResource(R.string.delete_note_desc)) },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            viewModel.deleteNote()
          }
        ) {
          Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }

  if (showReminderSheet) {
    ReminderSheet(
      initialAt = reminderAt,
      initialInterval = repeatInterval,
      initialDays = repeatDays,
      onSave = { selection ->
        viewModel.setReminder(selection)
        showReminderSheet = false
      },
      onRemove = if (reminderAt == null) null else {
        {
          viewModel.clearReminder()
          showReminderSheet = false
        }
      },
      onDismiss = { showReminderSheet = false }
    )
  }

  if (showExactAlarmDialog) {
    AlertDialog(
      onDismissRequest = {
        showExactAlarmDialog = false
        showReminderSheet = true
      },
      title = { Text(stringResource(R.string.exact_alarm_title)) },
      text = { Text(stringResource(R.string.exact_alarm_desc)) },
      confirmButton = {
        TextButton(
          onClick = {
            showExactAlarmDialog = false
            exactAlarmSettingsLauncher.launch(
              Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            )
          }
        ) {
          Text(stringResource(R.string.exact_alarm_allow))
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showExactAlarmDialog = false
            showReminderSheet = true
          }
        ) {
          Text(stringResource(R.string.exact_alarm_later))
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

  if (showSetupPinDialog) {
    SetupAppPinDialog(
      appPin = appPin,
      appPinHint = appPinHint,
      appPinScope = appPinScope,
      targetScope = 1,
      checkCurrentPin = authViewModel::checkPin,
      onDismiss = { showSetupPinDialog = false },
      onSave = { newPin, newHint, newScope -> 
        authViewModel.setAppPin(newPin, newHint) 
        authViewModel.setAppPinScope(newScope)
      }
    )
  }

  if (showOverlayPermissionDialog) {
    AlertDialog(
      onDismissRequest = { showOverlayPermissionDialog = false },
      title = { Text(stringResource(R.string.overlay_permission_title)) },
      text = { Text(stringResource(R.string.overlay_permission_note_desc)) },
      confirmButton = {
        TextButton(
          onClick = {
            showOverlayPermissionDialog = false
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            context.startActivity(intent)
          }
        ) {
          Text(stringResource(R.string.go_to_settings))
        }
      },
      dismissButton = {
        TextButton(onClick = { showOverlayPermissionDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }
}
