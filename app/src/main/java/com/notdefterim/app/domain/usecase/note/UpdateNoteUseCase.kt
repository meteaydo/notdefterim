package com.notdefterim.app.domain.usecase.note

import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.repository.NoteRepository
import java.time.LocalDateTime
import javax.inject.Inject

/** Mevcut notu günceller; updatedAt otomatik yenilenir. */
class UpdateNoteUseCase @Inject constructor(
  private val repository: NoteRepository
) {
  suspend operator fun invoke(note: Note) {
    repository.updateNote(note.copy(updatedAt = LocalDateTime.now()))
  }
}
