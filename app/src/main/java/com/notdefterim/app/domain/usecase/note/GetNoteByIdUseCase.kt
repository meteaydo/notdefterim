package com.notdefterim.app.domain.usecase.note

import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.repository.NoteRepository
import javax.inject.Inject

/** ID ile tek bir notu getirir. Bulunamazsa null döner. */
class GetNoteByIdUseCase @Inject constructor(
  private val repository: NoteRepository
) {
  suspend operator fun invoke(id: Long): Note? = repository.getNoteById(id)
}
