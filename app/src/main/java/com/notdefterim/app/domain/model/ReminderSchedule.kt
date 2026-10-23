package com.notdefterim.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Hatırlatıcının ilk ve sonraki zamanlarını hesaplar. Android'e bağlı değildir.
 */
object ReminderSchedule {

  /** Pazartesi–Cuma */
  val WEEKDAYS: Set<DayOfWeek> = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
  )

  /**
   * Kullanıcının seçtiği [start] zamanına göre ilk hatırlatma zamanı.
   * Tek seferlik hatırlatıcıda [start] geçmişteyse null döner. Tekrarlayanlarda geçmiş bir başlangıç
   * ya da seçilmemiş bir gün, sonraki uygun zamana ilerletilir.
   */
  fun firstOccurrence(
    start: LocalDateTime,
    interval: RepeatInterval,
    days: Set<DayOfWeek>,
    now: LocalDateTime
  ): LocalDateTime? {
    if (interval == RepeatInterval.NONE) return start.takeIf { it > now }
    val onSelectedDay = interval != RepeatInterval.DAYS_OF_WEEK || start.dayOfWeek in days
    if (start > now && onSelectedDay) return start
    return nextAfter(start, interval, days, start.dayOfMonth, maxOf(start, now))
  }

  /**
   * [current]'tan sonraki, [after]'dan geç olan ilk tekrar; tekrar yoksa null.
   * [dayOfMonth] aylık/yıllık tekrarın asıl günüdür (0: [current]'ın günü). Böylece 31'inde kurulan
   * aylık hatırlatıcı Şubat'ta 28'ine düşer ama Mart'ta yeniden 31'ine döner.
   */
  fun nextAfter(
    current: LocalDateTime,
    interval: RepeatInterval,
    days: Set<DayOfWeek>,
    dayOfMonth: Int,
    after: LocalDateTime
  ): LocalDateTime? {
    val anchorDay = if (dayOfMonth in 1..31) dayOfMonth else current.dayOfMonth
    val candidates: Sequence<LocalDateTime> = when (interval) {
      RepeatInterval.NONE -> return null
      RepeatInterval.DAILY -> steps().map { current.plusDays(it) }
      RepeatInterval.WEEKLY -> steps().map { current.plusWeeks(it) }
      RepeatInterval.MONTHLY -> steps().map { current.plusMonths(it).withDayClamped(anchorDay) }
      RepeatInterval.YEARLY -> steps().map { current.plusYears(it).withDayClamped(anchorDay) }
      RepeatInterval.DAYS_OF_WEEK -> {
        if (days.isEmpty()) return null
        steps().map { current.plusDays(it) }.filter { it.dayOfWeek in days }
      }
    }
    return candidates.first { it > after }
  }

  private fun steps() = generateSequence(1L) { it + 1 }

  private fun LocalDateTime.withDayClamped(day: Int): LocalDateTime =
    withDayOfMonth(minOf(day, YearMonth.from(this).lengthOfMonth()))
}
