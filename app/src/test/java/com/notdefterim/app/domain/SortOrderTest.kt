package com.notdefterim.app.domain

import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.model.NoteSortOrder
import com.notdefterim.app.domain.model.Password
import com.notdefterim.app.domain.model.PasswordSortOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale

class SortOrderTest {

  private val turkish = Collator.getInstance(Locale.forLanguageTag("tr-TR"))
  private val day = LocalDateTime.of(2026, 10, 23, 12, 0)

  private fun note(title: String, createdDaysAgo: Long, editedDaysAgo: Long) = Note(
    title = title,
    content = "",
    createdAt = day.minusDays(createdDaysAgo),
    updatedAt = day.minusDays(editedDaysAgo)
  )

  private val a = note("Çarşı", createdDaysAgo = 1, editedDaysAgo = 5)
  private val b = note("ödev", createdDaysAgo = 9, editedDaysAgo = 1)
  private val c = note("", createdDaysAgo = 3, editedDaysAgo = 2)
  private val d = note("Banka", createdDaysAgo = 5, editedDaysAgo = 9)

  private fun List<Note>.titles() = map { it.title.ifBlank { "-" } }

  @Test
  fun notlar_sonDuzenlenenOnce() {
    assertEquals(listOf("ödev", "-", "Çarşı", "Banka"), listOf(a, b, c, d).sortedWith(NoteSortOrder.LAST_EDITED.comparator()).titles())
  }

  @Test
  fun notlar_sonOlusturulanOnce() {
    assertEquals(listOf("Çarşı", "-", "Banka", "ödev"), listOf(a, b, c, d).sortedWith(NoteSortOrder.CREATED.comparator()).titles())
  }

  @Test
  fun notlar_basligaGoreTurkceSiraBasliksizlarSonda() {
    assertEquals(listOf("Banka", "Çarşı", "ödev", "-"), listOf(a, b, c, d).sortedWith(NoteSortOrder.TITLE.comparator(turkish)).titles())
  }

  @Test
  fun parolalar_enEskiDegistirilenOnce_hicDegismeyenOlusturmaZamaniyla() {
    val old = Password(platformName = "Eski", username = "", passwordValue = "", createdAt = 100, updatedAt = 500)
    val neverChanged = Password(platformName = "Hiç", username = "", passwordValue = "", createdAt = 300)
    val recent = Password(platformName = "Yeni", username = "", passwordValue = "", createdAt = 50, updatedAt = 900)
    assertEquals(
      listOf("Hiç", "Eski", "Yeni"),
      listOf(recent, old, neverChanged).sortedWith(PasswordSortOrder.LAST_CHANGED.comparator()).map { it.platformName }
    )
  }

  @Test
  fun parolalar_enCokKullanilanVeAdaGore() {
    val bank = Password(platformName = "Banka", username = "", passwordValue = "", usageCount = 2)
    val mail = Password(platformName = "e-Devlet", username = "", passwordValue = "", usageCount = 7)
    val shop = Password(platformName = "Çiçekçi", username = "", passwordValue = "", usageCount = 0)
    val list = listOf(shop, bank, mail)
    assertEquals(listOf("e-Devlet", "Banka", "Çiçekçi"), list.sortedWith(PasswordSortOrder.MOST_USED.comparator()).map { it.platformName })
    assertEquals(listOf("Banka", "Çiçekçi", "e-Devlet"), list.sortedWith(PasswordSortOrder.NAME.comparator(turkish)).map { it.platformName })
  }

  @Test
  fun bilinmeyenKayitVarsayilanaDoner() {
    assertEquals(NoteSortOrder.LAST_EDITED, NoteSortOrder.fromName(null))
    assertEquals(PasswordSortOrder.MOST_USED, PasswordSortOrder.fromName("YOK"))
  }
}
