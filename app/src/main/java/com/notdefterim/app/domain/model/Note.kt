package com.notdefterim.app.domain.model

import java.time.LocalDateTime

/**
 * Domain model — android bağımlılığı içermez.
 * UI ve iş mantığı bu modeli kullanır; Entity sadece veri katmanında kalır.
 */
data class Note(
  val id: Long = 0,
  val title: String,
  val content: String,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  val updatedAt: LocalDateTime = LocalDateTime.now(),
  val isPinned: Boolean = false,
  val color: NoteColor = NoteColor.DEFAULT
)

/**
 * Not kartı arka plan renkleri.
 * index değeri NoteEntity.colorIndex ile eşleştirilir.
 */
enum class NoteColor(val index: Int) {
  DEFAULT(0),
  ROSE(1),
  PEACH(2),
  SAND(3),
  MINT(4),
  SKY(5),
  LAVENDER(6),
  GRAPHITE(7);

  companion object {
    fun fromIndex(index: Int): NoteColor =
      entries.firstOrNull { it.index == index } ?: DEFAULT
  }
}
