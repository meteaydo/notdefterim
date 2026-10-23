package com.notdefterim.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.notdefterim.app.R
import com.notdefterim.app.domain.model.ReminderSchedule
import com.notdefterim.app.domain.model.RepeatInterval
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "Bugün 09:00", "Yarın 09:00", "Dün 09:00", "24 Eki 09:00"; başka bir yıldaysa "24 Eki 2027 09:00" */
@Composable
@ReadOnlyComposable
fun reminderTimeText(at: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
  val time = at.format(timeFormatter)
  val date = at.toLocalDate()
  val today = now.toLocalDate()
  return when (date) {
    today -> stringResource(R.string.reminder_today_format, time)
    today.plusDays(1) -> stringResource(R.string.reminder_tomorrow_format, time)
    today.minusDays(1) -> stringResource(R.string.reminder_yesterday_format, time)
    else -> {
      val pattern = if (date.year == today.year) "d MMM" else "d MMM yyyy"
      "${date.format(DateTimeFormatter.ofPattern(pattern, currentLocale()))} $time"
    }
  }
}

/** Tekrarın kısa adı ("Her Gün", "Hafta İçi", "Pzt, Çar"); tekrar yoksa null. */
@Composable
@ReadOnlyComposable
fun repeatText(interval: RepeatInterval, days: Set<DayOfWeek>): String? = when (interval) {
  RepeatInterval.NONE -> null
  RepeatInterval.DAILY -> stringResource(R.string.repeat_daily)
  RepeatInterval.WEEKLY -> stringResource(R.string.repeat_weekly)
  RepeatInterval.MONTHLY -> stringResource(R.string.repeat_monthly)
  RepeatInterval.YEARLY -> stringResource(R.string.repeat_yearly)
  RepeatInterval.DAYS_OF_WEEK -> when {
    days == ReminderSchedule.WEEKDAYS -> stringResource(R.string.repeat_weekdays)
    days.size == 7 -> stringResource(R.string.repeat_daily)
    else -> {
      val locale = currentLocale()
      weekOrder(locale).filter { it in days }.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }
  }
}

/** Haftanın günleri, bölgenin hafta başına göre sıralı (Türkiye'de Pazartesi, ABD'de Pazar başta) */
fun weekOrder(locale: Locale): List<DayOfWeek> {
  // Uygulama dili bölgesiz seçildiğinde ("tr") WeekFields haftayı Pazar'dan başlatıyor; bölge yoksa Pazartesi
  val first = if (locale.country.isEmpty()) DayOfWeek.MONDAY else WeekFields.of(locale).firstDayOfWeek
  return (0L until 7L).map { first.plus(it) }
}

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
