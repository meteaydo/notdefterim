package com.notdefterim.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var isChecked: Boolean = false,
    var hasCheckbox: Boolean = true
)

/**
 * Düz metin satırlarını maddelere çevirir; boş satırlar atlanır.
 * Not az önce listeden metne çevrildiyse [previous] o listedir: aynı metinli maddelerin
 * işareti ve madde imi geri gelir. Eşleşmeyen (metinde yeni yazılan) satırlar işaretsiz madde olur.
 */
fun linesToChecklist(lines: List<String>, previous: List<ChecklistItem> = emptyList()): List<ChecklistItem> {
    val unused = previous.toMutableList()
    return lines.map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
        val match = unused.firstOrNull { it.text.trim() == line }
        if (match != null) {
            unused.remove(match)
            match.copy(text = line)
        } else {
            ChecklistItem(text = line)
        }
    }
}
