package com.notdefterim.app.domain.usecase.note

import com.notdefterim.app.domain.repository.NoteRepository
import javax.inject.Inject

/** Notu ID ile siler. */
class DeleteNoteUseCase @Inject constructor(
  private val repository: NoteRepository
) {
  suspend operator fun invoke(id: Long) = repository.deleteNoteById(id)
}
