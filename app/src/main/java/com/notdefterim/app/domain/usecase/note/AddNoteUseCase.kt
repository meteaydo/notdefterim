package com.notdefterim.app.domain.usecase.note

import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.repository.NoteRepository
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Yeni not ekler.
 * İş kuralı: başlık ve içerik tamamen boşsa kaydetme.
 */
class AddNoteUseCase @Inject constructor(
  private val repository: NoteRepository
) {
  suspend operator fun invoke(note: Note): Result<Long> {
    if (note.title.isBlank() && note.content.isBlank()) {
      return Result.failure(IllegalArgumentException("Başlık veya içerik boş olamaz"))
    }
    val noteWithTimestamp = note.copy(
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now()
    )
    return Result.success(repository.addNote(noteWithTimestamp))
  }
}
