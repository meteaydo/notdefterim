package com.notdefterim.app.domain

import com.notdefterim.app.domain.model.ChecklistItem
import com.notdefterim.app.domain.model.linesToChecklist
import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistConversionTest {

  private val previous = listOf(
    ChecklistItem(id = "1", text = "Süt", isChecked = true),
    ChecklistItem(id = "2", text = "Ekmek"),
    ChecklistItem(id = "3", text = "Not: indirimli olanları al", hasCheckbox = false)
  )

  @Test
  fun listeKapatilipAcilinca_isaretVeMaddeImiGeriGelir() {
    val result = linesToChecklist(listOf("Süt", "Ekmek", "Not: indirimli olanları al"), previous)
    assertEquals(previous, result)
  }

  @Test
  fun metindeYeniYazilanSatir_isaretsizMaddeOlur_bosSatirAtlanir() {
    val result = linesToChecklist(listOf("Ekmek", "", "Yumurta", "  Süt "), previous)
    assertEquals(listOf("Ekmek", "Yumurta", "Süt"), result.map { it.text })
    assertEquals(listOf(false, false, true), result.map { it.isChecked })
    assertEquals(listOf("2", "1"), listOf(result[0].id, result[2].id))
  }

  @Test
  fun ayniMetinliMaddeler_sirayla_eslesir() {
    val twice = listOf(ChecklistItem(id = "a", text = "Su", isChecked = true), ChecklistItem(id = "b", text = "Su"))
    val result = linesToChecklist(listOf("Su", "Su", "Su"), twice)
    assertEquals(listOf(true, false, false), result.map { it.isChecked })
    assertEquals(listOf("a", "b"), result.take(2).map { it.id })
  }

  @Test
  fun oncekiListeYoksa_hepsiIsaretsizMadde() {
    val result = linesToChecklist(listOf("Bir", "İki"))
    assertEquals(listOf("Bir", "İki"), result.map { it.text })
    assertEquals(listOf(false, false), result.map { it.isChecked })
    assertEquals(listOf(true, true), result.map { it.hasCheckbox })
  }
}
