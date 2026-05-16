package com.notdefterim.app.data.mapper

import com.notdefterim.app.data.local.entity.NoteEntity
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.model.NoteColor
import com.notdefterim.app.domain.model.RepeatInterval
import com.notdefterim.app.domain.model.Category
import com.notdefterim.app.data.local.entity.CategoryEntity
import com.notdefterim.app.data.local.entity.NoteWithCategory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Entity ↔ Domain model dönüşüm fonksiyonları.
 *
 * Neden ayrı bir mapper?
 * - Domain katmanı android bağımlılığı içermemeli.
 * - Dönüşüm mantığı tek bir yerde bulunursa test ve bakım kolaylaşır.
 */

/** NoteEntity → Domain Note */
fun NoteEntity.toDomain(): Note = Note(
  id = id,
  title = title,
  content = content,
  createdAt = Instant.ofEpochMilli(createdAt)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime(),
  updatedAt = Instant.ofEpochMilli(updatedAt)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime(),
  isPinned = isPinned,
  color = NoteColor.fromIndex(colorIndex),
  reminderAt = reminderAt?.let {
    Instant.ofEpochMilli(it)
      .atZone(ZoneId.systemDefault())
      .toLocalDateTime()
  },
  repeatInterval = try { RepeatInterval.valueOf(repeatInterval) } catch (e: Exception) { RepeatInterval.NONE },
  viewCount = viewCount,
  isLocked = isLocked,
  isChecklist = isChecklist
)

/** NoteWithCategory → Domain Note */
fun NoteWithCategory.toDomain(): Note {
  val note = this.note.toDomain()
  return note.copy(
    category = this.category?.toDomain()
  )
}

/** CategoryEntity → Domain Category */
fun CategoryEntity.toDomain(): Category = Category(
  id = id,
  name = name,
  colorHex = colorHex,
  type = try { com.notdefterim.app.domain.model.CategoryType.valueOf(type) } catch (e: Exception) { com.notdefterim.app.domain.model.CategoryType.NOTE }
)

/** Domain Category → CategoryEntity */
fun Category.toEntity(): CategoryEntity = CategoryEntity(
  id = id,
  name = name,
  colorHex = colorHex,
  type = type.name
)

/** Domain Note → NoteEntity */
fun Note.toEntity(): NoteEntity = NoteEntity(
  id = id,
  title = title,
  content = content,
  createdAt = createdAt
    .atZone(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli(),
  updatedAt = updatedAt
    .atZone(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli(),
  isPinned = isPinned,
  colorIndex = color.index,
  reminderAt = reminderAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
  repeatInterval = repeatInterval.name,
  categoryId = category?.id,
  viewCount = viewCount,
  isLocked = isLocked,
  isChecklist = isChecklist
)

/** Liste dönüşümü. */
fun List<NoteEntity>.toDomainList(): List<Note> = map { it.toDomain() }

@JvmName("toDomainListNoteWithCategory")
fun List<NoteWithCategory>.toDomainList(): List<Note> = map { it.toDomain() }
