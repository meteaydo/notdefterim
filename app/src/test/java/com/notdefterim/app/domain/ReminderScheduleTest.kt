package com.notdefterim.app.domain

import com.notdefterim.app.domain.model.ReminderSchedule
import com.notdefterim.app.domain.model.RepeatInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class ReminderScheduleTest {

  // 23 Ekim 2026 Cuma
  private val now = LocalDateTime.of(2026, 10, 23, 12, 0)

  @Test
  fun tekSeferlik_gecmisZamanKurulmaz() {
    assertNull(ReminderSchedule.firstOccurrence(now.minusMinutes(1), RepeatInterval.NONE, emptySet(), now))
    val later = now.plusHours(1)
    assertEquals(later, ReminderSchedule.firstOccurrence(later, RepeatInterval.NONE, emptySet(), now))
  }

  @Test
  fun tekrarlayan_gecmisBaslangicSonrakiZamanaIlerler() {
    // Bugün 09:00 her gün → yarın 09:00
    val start = now.withHour(9)
    assertEquals(start.plusDays(1), ReminderSchedule.firstOccurrence(start, RepeatInterval.DAILY, emptySet(), now))
  }

  @Test
  fun haftaIci_cumaAksamindanSonraPazartesiyeGecer() {
    val start = now.withHour(9) // Cuma 09:00, geçti
    val first = ReminderSchedule.firstOccurrence(start, RepeatInterval.DAYS_OF_WEEK, ReminderSchedule.WEEKDAYS, now)
    assertEquals(LocalDateTime.of(2026, 10, 26, 9, 0), first)
    assertEquals(DayOfWeek.MONDAY, first!!.dayOfWeek)
  }

  @Test
  fun belirliGunler_secilmemisGunlukBaslangicIlkSeciliGuneKayar() {
    // Cumartesi 10:00 seçildi ama yalnız Salı ve Perşembe istendi
    val start = LocalDateTime.of(2026, 10, 24, 10, 0)
    val days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
    val first = ReminderSchedule.firstOccurrence(start, RepeatInterval.DAYS_OF_WEEK, days, now)
    assertEquals(LocalDateTime.of(2026, 10, 27, 10, 0), first)
    val second = ReminderSchedule.nextAfter(first!!, RepeatInterval.DAYS_OF_WEEK, days, 0, after = first)
    assertEquals(LocalDateTime.of(2026, 10, 29, 10, 0), second)
  }

  @Test
  fun belirliGunler_gunSecilmezseKurulmaz() {
    assertNull(ReminderSchedule.firstOccurrence(now.plusDays(1), RepeatInterval.DAYS_OF_WEEK, emptySet(), now))
  }

  @Test
  fun aylik_ayinSonGunuKaymaz() {
    val jan31 = LocalDateTime.of(2027, 1, 31, 9, 0)
    val feb = ReminderSchedule.nextAfter(jan31, RepeatInterval.MONTHLY, emptySet(), 31, after = jan31)
    assertEquals(LocalDateTime.of(2027, 2, 28, 9, 0), feb)
    val mar = ReminderSchedule.nextAfter(feb!!, RepeatInterval.MONTHLY, emptySet(), 31, after = feb)
    assertEquals(LocalDateTime.of(2027, 3, 31, 9, 0), mar)
    val apr = ReminderSchedule.nextAfter(mar!!, RepeatInterval.MONTHLY, emptySet(), 31, after = mar)
    assertEquals(LocalDateTime.of(2027, 4, 30, 9, 0), apr)
  }

  @Test
  fun aylik_eskiKayitlardaGunHatirlaticininGunu() {
    val mar15 = LocalDateTime.of(2027, 3, 15, 9, 0)
    assertEquals(
      LocalDateTime.of(2027, 4, 15, 9, 0),
      ReminderSchedule.nextAfter(mar15, RepeatInterval.MONTHLY, emptySet(), 0, after = mar15)
    )
  }

  @Test
  fun yillik_29SubatArtikYildaGeriDoner() {
    val feb29 = LocalDateTime.of(2028, 2, 29, 9, 0)
    val y2029 = ReminderSchedule.nextAfter(feb29, RepeatInterval.YEARLY, emptySet(), 29, after = feb29)
    assertEquals(LocalDateTime.of(2029, 2, 28, 9, 0), y2029)
    val y2032 = ReminderSchedule.nextAfter(y2029!!, RepeatInterval.YEARLY, emptySet(), 29, after = LocalDateTime.of(2031, 3, 1, 0, 0))
    assertEquals(LocalDateTime.of(2032, 2, 29, 9, 0), y2032)
  }

  @Test
  fun kacirilanTekrarlarAtlanir() {
    // Telefon üç hafta kapalı kaldı: haftalık hatırlatıcı şimdiden sonraki ilk tekrara gider
    val old = LocalDateTime.of(2026, 10, 1, 9, 0) // Perşembe
    assertEquals(
      LocalDateTime.of(2026, 10, 29, 9, 0),
      ReminderSchedule.nextAfter(old, RepeatInterval.WEEKLY, emptySet(), 0, after = now)
    )
  }

  @Test
  fun tekrarsizdaSonrakiYok() {
    assertNull(ReminderSchedule.nextAfter(now, RepeatInterval.NONE, emptySet(), 0, after = now))
  }
}
