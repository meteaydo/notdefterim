package com.notdefterim.app.data.mapper

import com.notdefterim.app.data.local.entity.NoteEntity
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.model.NoteColor
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
  color = NoteColor.fromIndex(colorIndex)
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
  colorIndex = color.index
)

/** Liste dönüşümü. */
fun List<NoteEntity>.toDomainList(): List<Note> = map { it.toDomain() }
