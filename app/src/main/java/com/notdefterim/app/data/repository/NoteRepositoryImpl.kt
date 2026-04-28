package com.notdefterim.app.data.repository

import com.notdefterim.app.data.local.NoteDao
import com.notdefterim.app.data.mapper.toDomain
import com.notdefterim.app.data.mapper.toDomainList
import com.notdefterim.app.data.mapper.toEntity
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NoteRepository arayüzünün veri katmanı implementasyonu.
 * DAO çağrılarını sarmalar ve Entity ↔ Domain dönüşümlerini koordine eder.
 */
@Singleton
class NoteRepositoryImpl @Inject constructor(
  private val noteDao: NoteDao
) : NoteRepository {

  override fun getAllNotes(): Flow<List<Note>> =
    noteDao.getAllNotes().map { it.toDomainList() }

  override suspend fun getNoteById(id: Long): Note? =
    noteDao.getNoteById(id)?.toDomain()

  override fun searchNotes(query: String): Flow<List<Note>> =
    noteDao.searchNotes(query).map { it.toDomainList() }

  override suspend fun addNote(note: Note): Long =
    noteDao.insertNote(note.toEntity())

  override suspend fun updateNote(note: Note) =
    noteDao.updateNote(note.toEntity())

  override suspend fun deleteNote(note: Note) =
    noteDao.deleteNote(note.toEntity())

  override suspend fun deleteNoteById(id: Long) =
    noteDao.deleteNoteById(id)

  override suspend fun getAllNotesSnapshot(): List<Note> =
    noteDao.getAllNotesSnapshot().toDomainList()
}
