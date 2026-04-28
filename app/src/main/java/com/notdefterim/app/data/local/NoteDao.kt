package com.notdefterim.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notdefterim.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Not veritabanı erişim arayüzü.
 *
 * Flow döndüren metodlar reaktif veri akışı sağlar:
 * not değiştiğinde UI otomatik güncellenir, manuel yenilemeye gerek kalmaz.
 */
@Dao
interface NoteDao {

  /** Tüm notları güncelleme zamanına göre azalan sırada döndürür. */
  @Query(
    """
    SELECT * FROM notes
    ORDER BY isPinned DESC, updatedAt DESC
    """
  )
  fun getAllNotes(): Flow<List<NoteEntity>>

  /** Belirli bir notu ID ile getirir; yoksa null döner. */
  @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
  suspend fun getNoteById(id: Long): NoteEntity?

  /**
   * Spotlight akıllı arama — başlık ve içerikte arar.
   * LIKE kullanımı SQLCipher ile uyumludur; FTS kullanmak için
   * ayrı bir FTS entity tanımlanabilir (performans artışı için önerilir).
   */
  @Query(
    """
    SELECT * FROM notes
    WHERE title LIKE '%' || :query || '%'
    OR content LIKE '%' || :query || '%'
    ORDER BY isPinned DESC, updatedAt DESC
    """
  )
  fun searchNotes(query: String): Flow<List<NoteEntity>>

  /** Yeni not ekler; ID çakışırsa günceller. */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNote(note: NoteEntity): Long

  /** Mevcut notu günceller. */
  @Update
  suspend fun updateNote(note: NoteEntity)

  /** Notu siler. */
  @Delete
  suspend fun deleteNote(note: NoteEntity)

  /** ID ile not siler. */
  @Query("DELETE FROM notes WHERE id = :id")
  suspend fun deleteNoteById(id: Long)

  /** Drive yedeği için tüm notları tek seferlik getirir (Flow değil). */
  @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
  suspend fun getAllNotesSnapshot(): List<NoteEntity>
}
