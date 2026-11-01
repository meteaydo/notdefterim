package com.notdefterim.app.util

import com.notdefterim.app.domain.model.ChecklistItem

/**
 * Paylaşılacak metni kurar: başlık, boş bir satır, sonra içerik.
 * Madde imli notta yalnızca seçilen maddeler ☑/☐ ile yazılır; kutusuz satırlar (açıklama) her zaman girer.
 */
fun buildShareText(
  title: String,
  content: String,
  isChecklist: Boolean,
  items: List<ChecklistItem>,
  includeChecked: Boolean = true,
  includeUnchecked: Boolean = true
): String {
  val body = if (!isChecklist) {
    content.trim()
  } else {
    items
      .filter { !it.hasCheckbox || (it.isChecked && includeChecked) || (!it.isChecked && includeUnchecked) }
      .filter { it.text.isNotBlank() }
      .joinToString("\n") { item ->
        when {
          !item.hasCheckbox -> item.text
          item.isChecked -> "☑ ${item.text}"
          else -> "☐ ${item.text}"
        }
      }
  }
  return listOf(title.trim(), body).filter { it.isNotBlank() }.joinToString("\n\n")
}
