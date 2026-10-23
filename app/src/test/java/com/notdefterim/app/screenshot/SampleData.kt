package com.notdefterim.app.screenshot

import com.notdefterim.app.domain.model.Category
import com.notdefterim.app.domain.model.CategoryType
import com.notdefterim.app.domain.model.ChecklistItem
import com.notdefterim.app.domain.model.Note
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

/**
 * Ekran görüntüsü testlerinde kullanılan sabit örnek veri.
 * Tarihler ve id'ler sabittir; her çalıştırmada aynı görüntü üretilir.
 */
object SampleData {

  /** Testlerin "şimdi"si; kartlardaki hatırlatıcı zamanı buna göre "Bugün/Yarın" diye yazılır */
  val date: LocalDateTime = LocalDateTime.of(2026, 10, 12, 9, 30)

  val shopping = Category(id = 1, name = "Alışveriş", colorHex = "#FFF0D5")
  val todo = Category(id = 2, name = "Yapılacaklar", colorHex = "#D5E5FF")
  val ideas = Category(id = 3, name = "Fikirler", colorHex = "#E5FFD5")

  val passwordCategories = listOf(
    Category(id = 10, name = "Banka", colorHex = "#D5E5FF", type = CategoryType.PASSWORD),
    Category(id = 11, name = "Google", colorHex = "#FFD5D5", type = CategoryType.PASSWORD),
    Category(id = 12, name = "S. Medya", colorHex = "#E5D5FF", type = CategoryType.PASSWORD)
  )

  val shoppingItems = listOf(
    ChecklistItem(id = "c1", text = "Süt", isChecked = true),
    ChecklistItem(id = "c2", text = "Ekmek"),
    ChecklistItem(id = "c3", text = "Yumurta"),
    ChecklistItem(id = "c4", text = "Zeytinyağı", isChecked = true)
  )

  private val weekendItems = listOf(
    ChecklistItem(id = "w1", text = "Evi temizle"),
    ChecklistItem(id = "w2", text = "Arabayı yıka"),
    ChecklistItem(id = "w3", text = "Kitap oku")
  )

  val shoppingNote = Note(
    id = 1,
    title = "Market İhtiyaçları",
    content = Json.encodeToString(shoppingItems),
    createdAt = date,
    updatedAt = date,
    isPinned = true,
    category = shopping,
    isChecklist = true
  )

  val weekendNote = Note(
    id = 2,
    title = "Hafta Sonu Planı",
    content = Json.encodeToString(weekendItems),
    createdAt = date,
    updatedAt = date.minusDays(1),
    category = todo,
    isChecklist = true
  )

  val ideaNote = Note(
    id = 3,
    title = "Yeni Proje Fikirleri",
    content = "Yapay zeka entegreli not alma uygulaması yapabilirim. Çevrimdışı ve güvenli olmalı.",
    createdAt = date,
    updatedAt = date.minusDays(3),
    category = ideas
  )

  val reminderNote = Note(
    id = 4,
    title = "Banka randevusu",
    content = "Saat 14:00'te şubede olmayı unutma.",
    createdAt = date,
    updatedAt = date.minusDays(5),
    reminderAt = date.plusDays(2)
  )

  val lockedNote = Note(
    id = 5,
    title = "Özel notlar",
    content = "Bu içerik kilitli olduğu için bulanık görünür.",
    createdAt = date,
    updatedAt = date.minusDays(8),
    isLocked = true
  )

  val untitledNote = Note(
    id = 6,
    title = "",
    content = "Başlıksız kısa bir not.",
    createdAt = date,
    updatedAt = date.minusDays(20)
  )

  val notes = listOf(shoppingNote, weekendNote, ideaNote, reminderNote, lockedNote, untitledNote)
}
