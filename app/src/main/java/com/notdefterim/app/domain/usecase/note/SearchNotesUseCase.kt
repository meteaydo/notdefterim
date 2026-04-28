package com.notdefterim.app.domain.usecase.note

import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Spotlight akıllı arama.
 * Kısa sorgular (< 2 karakter) tüm notları döndürür; performansı korur.
 */
class SearchNotesUseCase @Inject constructor(
  private val repository: NoteRepository
) {
  operator fun invoke(query: String): Flow<List<Note>> {
    return if (query.trim().length < 2) {
      repository.getAllNotes()
    } else {
      repository.searchNotes(query.trim())
    }
  }
}
