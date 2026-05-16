package com.notdefterim.app.domain.repository

import com.notdefterim.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Not repository sözleşmesi — domain katmanında tanımlanır.
 * Implementasyon data katmanında yapılır (NoteRepositoryImpl).
 *
 * Neden interface?
 * - Bağımlılığı tersine çevirir (Dependency Inversion Principle).
 * - Test sırasında kolayca mock edilebilir.
 */
interface NoteRepository {
  fun getAllNotes(): Flow<List<Note>>
  suspend fun getNoteById(id: Long): Note?
  suspend fun addNote(note: Note): Long
  suspend fun updateNote(note: Note)
  suspend fun deleteNote(note: Note)
  suspend fun deleteNoteById(id: Long)
  suspend fun getAllNotesSnapshot(): List<Note>
}
