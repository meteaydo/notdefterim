package com.notdefterim.app.domain.usecase.note

import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.repository.NoteRepository
import java.time.LocalDateTime
import javax.inject.Inject

/** Mevcut notu günceller; [touch] true ise updatedAt yenilenir (içerik değişmediyse false verin). */
class UpdateNoteUseCase @Inject constructor(
  private val repository: NoteRepository
) {
  suspend operator fun invoke(note: Note, touch: Boolean = true) {
    repository.updateNote(if (touch) note.copy(updatedAt = LocalDateTime.now()) else note)
  }
}
