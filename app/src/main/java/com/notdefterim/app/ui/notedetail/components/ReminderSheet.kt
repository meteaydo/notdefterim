package com.notdefterim.app.ui.notedetail.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notdefterim.app.R
import com.notdefterim.app.domain.model.ReminderSchedule
import com.notdefterim.app.domain.model.RepeatInterval
import com.notdefterim.app.ui.components.currentLocale
import com.notdefterim.app.ui.components.reminderTimeText
import com.notdefterim.app.ui.components.weekOrder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

/**
 * Pencerede seçilen tekrar. "Hafta içi" ve "Belirli günler" aynı [RepeatInterval.DAYS_OF_WEEK]
 * kaydına dönüşür; ayrı tutulmaları belirli günlerde Pzt–Cum seçilince seçimin "Hafta içi"ne atlamaması için.
 */
private enum class RepeatOption(val labelRes: Int) {
  ONCE(R.string.repeat_once),
  DAILY(R.string.repeat_daily),
  WEEKDAYS(R.string.repeat_weekdays),
  WEEKLY(R.string.repeat_weekly),
  MONTHLY(R.string.repeat_monthly),
  YEARLY(R.string.repeat_yearly),
  CUSTOM_DAYS(R.string.repeat_custom_days)
}

/** Kaydedilen hatırlatıcı: [at] ilk hatırlatma zamanı, [dayOfMonth] aylık/yıllık tekrarın asıl günü (yoksa 0) */
data class ReminderSelection(
  val at: LocalDateTime,
  val interval: RepeatInterval,
  val days: Set<DayOfWeek>,
  val dayOfMonth: Int
)

/**
 * Hatırlatıcı kurma penceresi: hızlı seçenekler, tarih, saat ve tekrar bir arada.
 * [onRemove] null değilse not zaten bir hatırlatıcıya sahiptir ve "Kaldır" düğmesi görünür.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
  initialAt: LocalDateTime?,
  initialInterval: RepeatInterval,
  initialDays: Set<DayOfWeek>,
  onSave: (ReminderSelection) -> Unit,
  onRemove: (() -> Unit)?,
  onDismiss: () -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ) {
    ReminderSheetContent(
      initialAt = initialAt,
      initialInterval = initialInterval,
      initialDays = initialDays,
      onSave = onSave,
      onRemove = onRemove
    )
  }
}

/** Pencerenin içeriği; [clock] ekran görüntüsü testlerinde sabit bir zaman verir. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheetContent(
  initialAt: LocalDateTime?,
  initialInterval: RepeatInterval,
  initialDays: Set<DayOfWeek>,
  onSave: (ReminderSelection) -> Unit,
  onRemove: (() -> Unit)?,
  clock: () -> LocalDateTime = LocalDateTime::now
) {
  val now = clock()
  // Yeni hatırlatıcı bir sonraki tam saatle açılır
  val initial = remember { initialAt ?: now.truncatedTo(ChronoUnit.HOURS).plusHours(1) }
  var date by remember { mutableStateOf(initial.toLocalDate()) }
  var time by remember { mutableStateOf(initial.toLocalTime().truncatedTo(ChronoUnit.MINUTES)) }
  var option by remember {
    mutableStateOf(
      when (initialInterval) {
        RepeatInterval.NONE -> RepeatOption.ONCE
        RepeatInterval.DAILY -> RepeatOption.DAILY
        RepeatInterval.WEEKLY -> RepeatOption.WEEKLY
        RepeatInterval.MONTHLY -> RepeatOption.MONTHLY
        RepeatInterval.YEARLY -> RepeatOption.YEARLY
        RepeatInterval.DAYS_OF_WEEK ->
          if (initialDays == ReminderSchedule.WEEKDAYS) RepeatOption.WEEKDAYS else RepeatOption.CUSTOM_DAYS
      }
    )
  }
  var customDays by remember {
    mutableStateOf(
      if (initialInterval == RepeatInterval.DAYS_OF_WEEK && initialDays.isNotEmpty()) initialDays
      else setOf(initial.dayOfWeek)
    )
  }
  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }

  val start = LocalDateTime.of(date, time)
  val interval = when (option) {
    RepeatOption.ONCE -> RepeatInterval.NONE
    RepeatOption.DAILY -> RepeatInterval.DAILY
    RepeatOption.WEEKLY -> RepeatInterval.WEEKLY
    RepeatOption.MONTHLY -> RepeatInterval.MONTHLY
    RepeatOption.YEARLY -> RepeatInterval.YEARLY
    RepeatOption.WEEKDAYS, RepeatOption.CUSTOM_DAYS -> RepeatInterval.DAYS_OF_WEEK
  }
  val days = when (option) {
    RepeatOption.WEEKDAYS -> ReminderSchedule.WEEKDAYS
    RepeatOption.CUSTOM_DAYS -> customDays
    else -> emptySet()
  }
  val first = ReminderSchedule.firstOccurrence(start, interval, days, now)

  val locale = currentLocale()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
  ) {
    Text(
      text = stringResource(R.string.reminder_sheet_title),
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Hızlı seçenekler
    val inOneHour = now.plusHours(1).truncatedTo(ChronoUnit.MINUTES)
    val thisEvening = now.toLocalDate().atTime(20, 0)
    val tomorrowMorning = now.toLocalDate().plusDays(1).atTime(9, 0)
    val quickPicks = listOfNotNull(
      stringResource(R.string.reminder_in_one_hour) to inOneHour,
      // Akşama bir saatten az kaldıysa gösterilmez
      (reminderTimeText(thisEvening, now) to thisEvening).takeIf { thisEvening > inOneHour },
      reminderTimeText(tomorrowMorning, now) to tomorrowMorning
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      quickPicks.forEach { (label, at) ->
        FilterChip(
          selected = start == at,
          onClick = {
            date = at.toLocalDate()
            time = at.toLocalTime()
          },
          label = { Text(label) }
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Tarih ve saat
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      val datePattern = if (date.year == now.year) "EEE, d MMM" else "EEE, d MMM yyyy"
      OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(date.format(DateTimeFormatter.ofPattern(datePattern, locale)))
      }
      OutlinedButton(onClick = { showTimePicker = true }) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(time.format(DateTimeFormatter.ofPattern("HH:mm")))
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Tekrar
    Text(
      text = stringResource(R.string.reminder_repeat_label),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      RepeatOption.entries.forEach { item ->
        FilterChip(
          selected = option == item,
          onClick = { option = item },
          label = { Text(stringResource(item.labelRes)) }
        )
      }
    }

    if (option == RepeatOption.CUSTOM_DAYS) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        weekOrder(locale).forEach { day ->
          FilterChip(
            selected = day in customDays,
            onClick = { customDays = if (day in customDays) customDays - day else customDays + day },
            label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) }
          )
        }
      }
    }

    // İlk hatırlatma seçilenden farklıysa (geçmiş başlangıç ya da seçilmemiş gün) ya da kurulamıyorsa açıkla
    val message = when {
      first == null && interval == RepeatInterval.DAYS_OF_WEEK -> stringResource(R.string.reminder_pick_day)
      first == null -> stringResource(R.string.reminder_time_passed)
      first != start -> stringResource(R.string.reminder_first_at, reminderTimeText(first, now))
      else -> null
    }
    if (message != null) {
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (first == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      if (onRemove != null) {
        TextButton(
          onClick = onRemove,
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
          Text(stringResource(R.string.remove_reminder))
        }
      }
      Spacer(modifier = Modifier.weight(1f))
      Button(
        enabled = first != null,
        onClick = {
          // Pencere uzun süre açık kaldıysa seçilen zaman bu arada geçmiş olabilir
          val at = ReminderSchedule.firstOccurrence(start, interval, days, clock()) ?: return@Button
          val dayOfMonth = if (interval == RepeatInterval.MONTHLY || interval == RepeatInterval.YEARLY) date.dayOfMonth else 0
          onSave(ReminderSelection(at, interval, days, dayOfMonth))
        }
      ) {
        Text(stringResource(R.string.save))
      }
    }
  }

  if (showDatePicker) {
    val today = now.toLocalDate()
    val pickerState = rememberDatePickerState(
      initialSelectedDateMillis = date.toUtcMillis(),
      selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis.toUtcDate() >= today
        override fun isSelectableYear(year: Int) = year >= today.year
      }
    )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let { date = it.toUtcDate() }
          showDatePicker = false
        }) { Text(stringResource(R.string.ok)) }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
      }
    ) {
      DatePicker(state = pickerState)
    }
  }

  if (showTimePicker) {
    val context = LocalContext.current
    val timeState = rememberTimePickerState(
      initialHour = time.hour,
      initialMinute = time.minute,
      is24Hour = DateFormat.is24HourFormat(context)
    )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      confirmButton = {
        TextButton(onClick = {
          time = LocalTime.of(timeState.hour, timeState.minute)
          showTimePicker = false
        }) { Text(stringResource(R.string.ok)) }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
      },
      text = { TimePicker(state = timeState) }
    )
  }
}

// DatePicker tarihleri UTC gece yarısı olarak tutar
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
