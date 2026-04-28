package com.notdefterim.app.domain.usecase.note

import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Tüm notları reaktif akışla döndürür. Sıralama: önce sabitliler, sonra güncelleme zamanı. */
class GetNotesUseCase @Inject constructor(
  private val repository: NoteRepository
) {
  operator fun invoke(): Flow<List<Note>> = repository.getAllNotes()
}
