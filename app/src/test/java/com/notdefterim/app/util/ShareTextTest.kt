package com.notdefterim.app.util

import com.notdefterim.app.domain.model.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareTextTest {

  private val items = listOf(
    ChecklistItem(id = "1", text = "Süt", isChecked = true),
    ChecklistItem(id = "2", text = "Ekmek"),
    ChecklistItem(id = "3", text = "Not: indirimli olanları al", hasCheckbox = false),
    ChecklistItem(id = "4", text = "  ")
  )

  @Test
  fun duzNot_basligiVeIcerigiGonderir() {
    assertEquals("Başlık\n\nİçerik", buildShareText("Başlık", " İçerik ", isChecklist = false, items = emptyList()))
    assertEquals("İçerik", buildShareText("", "İçerik", isChecklist = false, items = emptyList()))
  }

  @Test
  fun maddeImliNot_varsayilandaTumMaddelerGider() {
    assertEquals(
      "Market\n\n☑ Süt\n☐ Ekmek\nNot: indirimli olanları al",
      buildShareText("Market", "", isChecklist = true, items = items)
    )
  }

  @Test
  fun maddeImliNot_yalnizIsaretsizler() {
    assertEquals(
      "Market\n\n☐ Ekmek\nNot: indirimli olanları al",
      buildShareText("Market", "", isChecklist = true, items = items, includeChecked = false)
    )
  }

  @Test
  fun maddeImliNot_yalnizIsaretliler() {
    assertEquals(
      "Market\n\n☑ Süt\nNot: indirimli olanları al",
      buildShareText("Market", "", isChecklist = true, items = items, includeUnchecked = false)
    )
  }
}
