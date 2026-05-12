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
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.width
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.focus.FocusDirection
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

  
  var isCategoriesExpanded by remember { mutableStateOf(false) }
  val reminderAt by viewModel.reminderAt.collectAsStateWithLifecycle()
  val repeatInterval by viewModel.repeatInterval.collectAsStateWithLifecycle()
  val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
  var isContentVisible by remember(isLocked) { mutableStateOf(!isLocked) }
  
  val categories by viewModel.categories.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  var showAddCategoryDialog by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  var showDeleteDialog by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()
  var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
  var viewportHeight by remember { mutableStateOf(0) }
  var contentOffsetY by remember { mutableStateOf(0f) }

  var contentTextFieldValue by remember { mutableStateOf(TextFieldValue(content)) }

  LaunchedEffect(content) {
      if (contentTextFieldValue.text != content) {
          contentTextFieldValue = contentTextFieldValue.copy(text = content)
      }
  }

  // Arama sorgusu değiştiğinde ilk eşleşmeye scroll yap
  LaunchedEffect(searchQuery) {
    if (searchQuery.isNotBlank()) {
      val index = content.lowercase().indexOf(searchQuery.lowercase())
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
        val lowerText = text.toString().lowercase()
        val lowerQuery = searchQuery.lowercase()
        var startIndex = 0
        while (startIndex < lowerText.length) {
          val index = lowerText.indexOf(lowerQuery, startIndex)
          if (index < 0) break
          addStyle(
            style = SpanStyle(
              background = primaryContainer,
              color = onPrimaryContainer,
              fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            start = index,
            end = index + lowerQuery.length
          )
          startIndex = index + lowerQuery.length
        }
      }
      TransformedText(annotated, OffsetMapping.Identity)
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }

  val context = LocalContext.current

  // Tek seferlik olayları dinle
  LaunchedEffect(Unit) {
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

  val noteCardColors = LocalNoteCardColors.current
  val parsedCategoryColor = selectedCategory?.colorHex?.let { hex ->
    try {
      val hexColor = android.graphics.Color.parseColor(hex)
      val hsv = FloatArray(3)
      android.graphics.Color.colorToHSV(hexColor, hsv)
      hsv[1] = hsv[1] * 0.2f
      Color(android.graphics.Color.HSVToColor(hsv))
    } catch (e: Exception) { null }
  }
  val backgroundColor = parsedCategoryColor ?: noteCardColors.colors.getOrElse(selectedColor.index) {
    MaterialTheme.colorScheme.background
  }

  val animatedBackground by animateColorAsState(
    targetValue = backgroundColor,
    animationSpec = tween(durationMillis = 400),
    label = "bg_anim"
  )

  // context moved above

  val showAlarmDialog = {
    val calendar = java.util.Calendar.getInstance()
    DatePickerDialog(
      context,
      { _, year, month, dayOfMonth ->
        calendar.set(year, month, dayOfMonth)
        TimePickerDialog(
          context,
          { _, hour, minute ->
            val reminder = java.time.LocalDateTime.of(year, month + 1, dayOfMonth, hour, minute)
            viewModel.onReminderSelect(reminder)
          },
          calendar.get(java.util.Calendar.HOUR_OF_DAY),
          calendar.get(java.util.Calendar.MINUTE),
          true
        ).show()
      },
      calendar.get(java.util.Calendar.YEAR),
      calendar.get(java.util.Calendar.MONTH),
      calendar.get(java.util.Calendar.DAY_OF_MONTH)
    ).show()
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      showAlarmDialog()
    } else {
      coroutineScope.launch {
        snackbarHostState.showSnackbar("Bildirim izni verilmediği için hatırlatıcı kurulamadı.")
      }
    }
  }

  BackHandler {
    viewModel.saveNote()
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = animatedBackground,
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
              imageVector = Icons.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.back_button_desc),
              modifier = Modifier.size(28.dp)
            )
          }
        },
        actions = {
          // Alarm
          IconToggleButton(
            checked = reminderAt != null,
            onCheckedChange = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                  showAlarmDialog()
                } else {
                  permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
              } else {
                showAlarmDialog()
              }
            },
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
            onCheckedChange = { viewModel.onLockToggle() },
            colors = IconButtonDefaults.iconToggleButtonColors(
              checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          ) {
            Icon(
              imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
              contentDescription = if (isLocked) "Kilidi Aç" else "Kilitle"
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
              imageVector = Icons.Rounded.FormatListBulleted,
              contentDescription = "Liste Modu"
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
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Bu not kilitli", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { 
              val activity = context as? androidx.fragment.app.FragmentActivity
              if (activity != null) {
                authViewModel.authenticateAction(
                  activity = activity,
                  title = context.getString(R.string.app_name),
                  subtitle = "Kilitli notu görmek için kimliğinizi doğrulayın",
                  onSuccess = { isContentVisible = true },
                  onError = { err -> 
                    coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                  }
                )
              } else {
                isContentVisible = true
              }
            }) {
              Text("İçeriği Göster")
            }
          }
        }
      } else {
        // ── Sabit Arama Kutusu (Üstte) ──────────────────────────────────────────
      OutlinedTextField(
        value = searchQuery,
        onValueChange = viewModel::onSearchQueryChange,
        placeholder = { Text(stringResource(R.string.search)) },
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

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
              viewportHeight = coordinates.size.height
            }
            .verticalScroll(scrollState)
        ) {

        if (reminderAt != null) {
          var showRepeatMenu by remember { mutableStateOf(false) }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.clickable { showRepeatMenu = true }
            ) {
              Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              
              val repeatText = when(repeatInterval) {
                RepeatInterval.DAILY -> stringResource(R.string.repeat_daily)
                RepeatInterval.WEEKLY -> stringResource(R.string.repeat_weekly)
                RepeatInterval.MONTHLY -> stringResource(R.string.repeat_monthly)
                RepeatInterval.YEARLY -> stringResource(R.string.repeat_yearly)
                else -> stringResource(R.string.repeat_none)
              }
              
              Text(
                text = "Hatırlatıcı Kuruldu: ${reminderAt!!.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))} ($repeatText)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
              )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(
              onClick = { 
                viewModel.onReminderSelect(null)
                viewModel.onRepeatIntervalSelect(RepeatInterval.NONE)
              },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel_reminder), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
          }

          if (showRepeatMenu) {
            AlertDialog(
              onDismissRequest = { showRepeatMenu = false },
              title = { Text("Tekrarlama Seçimi") },
              text = {
                Column {
                   RepeatInterval.values().forEach { interval ->
                      val text = when(interval) {
                         RepeatInterval.DAILY -> stringResource(R.string.repeat_daily)
                         RepeatInterval.WEEKLY -> stringResource(R.string.repeat_weekly)
                         RepeatInterval.MONTHLY -> stringResource(R.string.repeat_monthly)
                         RepeatInterval.YEARLY -> stringResource(R.string.repeat_yearly)
                         else -> stringResource(R.string.repeat_none)
                      }
                      Row(
                         modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                               viewModel.onRepeatIntervalSelect(interval)
                               showRepeatMenu = false 
                            }
                            .padding(16.dp),
                         verticalAlignment = Alignment.CenterVertically
                      ) {
                         Text(text)
                         if (repeatInterval == interval) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                         }
                      }
                   }
                }
              },
              confirmButton = {
                TextButton(onClick = { showRepeatMenu = false }) {
                  Text("Tamam")
                }
              }
            )
          }
        }

        // ── Kategori Seçimi ────────────────────────────────────────────
        val maxChipsWhenCollapsed = 7
        val visibleCategories = if (isCategoriesExpanded) categories else categories.take(maxChipsWhenCollapsed)
        val showExpandChip = !isCategoriesExpanded && categories.size > maxChipsWhenCollapsed

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          visibleCategories.forEach { cat ->
            val isSelected = selectedCategory?.id == cat.id
            val catColor = try { 
              val hexColor = android.graphics.Color.parseColor(cat.colorHex)
              val hsv = FloatArray(3)
              android.graphics.Color.colorToHSV(hexColor, hsv)
              hsv[1] = hsv[1] * 0.4f
              Color(android.graphics.Color.HSVToColor(hsv))
            } catch (e: Exception) { Color.Transparent }
            
            FilterChip(
              selected = isSelected,
              onClick = {
                if (isSelected) viewModel.onCategorySelect(null)
                else viewModel.onCategorySelect(cat)
              },
              label = { 
                 Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (cat.name.equals("Alışveriş", ignoreCase = true)) {
                        Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                    } else if (cat.name.equals("Yapılacaklar", ignoreCase = true)) {
                        Icon(Icons.Rounded.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp))
                    } else if (cat.name.equals("Fikirler", ignoreCase = true)) {
                        Icon(Icons.Outlined.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                    Text(cat.name, style = MaterialTheme.typography.labelMedium)
                 }
              },
              leadingIcon = if (isSelected) { { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
              modifier = Modifier
                .height(28.dp)
                .alpha(if (selectedCategory != null && !isSelected) 0.4f else 1f),
              colors = FilterChipDefaults.filterChipColors(
                containerColor = catColor.copy(alpha = 0.6f),
                selectedContainerColor = catColor,
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedLabelColor = MaterialTheme.colorScheme.onSurface
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
        }

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
                },
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
                  Color(0xFFE1BEE7).copy(alpha = 0.9f),
                  Color(0xFFE1BEE7).copy(alpha = 0.0f)
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
                  Color(0xFFE1BEE7).copy(alpha = 0.9f),
                  Color(0xFFE1BEE7).copy(alpha = 0.0f)
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
}
