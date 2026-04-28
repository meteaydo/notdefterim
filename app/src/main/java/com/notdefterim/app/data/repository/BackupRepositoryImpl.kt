package com.notdefterim.app.data.repository

import com.notdefterim.app.data.local.NoteDao
import com.notdefterim.app.data.mapper.toDomainList
import com.notdefterim.app.data.mapper.toEntity
import com.notdefterim.app.data.remote.DriveServiceHelper
import com.notdefterim.app.data.remote.GoogleAuthManager
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.repository.BackupRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BackupRepository arayüzünün implementasyonu.
 *
 * Yedekleme stratejisi: "son yazma kazanır"
 * - Yerel notlar JSON olarak serileştirilerek Drive'a yüklenir.
 * - Geri yüklemede Drive verisi yerel veritabanının üzerine yazılır.
 * - Çakışma çözümü basit tutulmuştur; gelişmiş senkronizasyon için
 *   delta sync ve conflict resolution eklenebilir.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
  private val noteDao: NoteDao,
  private val driveServiceHelper: DriveServiceHelper,
  private val googleAuthManager: GoogleAuthManager
) : BackupRepository {

  private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
  }

  override suspend fun backupToCloud(): Result<Unit> {
    return try {
      val notes = noteDao.getAllNotesSnapshot().toDomainList()
      val backupPayload = BackupPayload(
        version = 1,
        backupTimestamp = System.currentTimeMillis(),
        noteCount = notes.size,
        notes = notes.map { it.toSerializable() }
      )
      val jsonString = json.encodeToString(backupPayload)

      driveServiceHelper.uploadBackup(jsonString, googleAuthManager)
        .map { /* drive file id'sini görmezden gel */ }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun restoreFromCloud(): Result<Int> {
    return try {
      val jsonResult = driveServiceHelper.downloadLatestBackup(googleAuthManager)
      jsonResult.fold(
        onSuccess = { jsonString ->
          val payload = json.decodeFromString<BackupPayload>(jsonString)
          val entities = payload.notes.map { it.toEntity() }

          // Mevcut notları sil, Drive'daki yedekle değiştir
          // NOT: Üretimde merge stratejisi daha uygundur
          noteDao.getAllNotesSnapshot().forEach { noteDao.deleteNoteById(it.id) }
          entities.forEach { noteDao.insertNote(it) }

          Result.success(entities.size)
        },
        onFailure = { Result.failure(it) }
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

// ─── Yedek Veri Modelleri (yalnızca bu dosyada kullanılır) ─────────────────

@kotlinx.serialization.Serializable
private data class BackupPayload(
  val version: Int,
  val backupTimestamp: Long,
  val noteCount: Int,
  val notes: List<SerializableNote>
)

@kotlinx.serialization.Serializable
private data class SerializableNote(
  val id: Long,
  val title: String,
  val content: String,
  val createdAt: Long,
  val updatedAt: Long,
  val isPinned: Boolean,
  val colorIndex: Int
)

private fun Note.toSerializable() = SerializableNote(
  id = id,
  title = title,
  content = content,
  createdAt = createdAt
    .atZone(java.time.ZoneId.systemDefault())
    .toInstant().toEpochMilli(),
  updatedAt = updatedAt
    .atZone(java.time.ZoneId.systemDefault())
    .toInstant().toEpochMilli(),
  isPinned = isPinned,
  colorIndex = color.index
)

private fun SerializableNote.toEntity() =
  com.notdefterim.app.data.local.entity.NoteEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    colorIndex = colorIndex
  )
