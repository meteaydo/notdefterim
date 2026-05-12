package com.notdefterim.app.data.remote

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Google Drive REST API v3 — appDataFolder işlemleri.
 *
 * appDataFolder neden?
 * - Kullanıcı kendi Drive'ında bu klasörü göremez, yanlışlıkla silemez.
 * - Uygulama silindiğinde Drive verisi de temizlenir.
 * - Kullanıcının Drive kotasından düşmez.
 */
class DriveServiceHelper(private val context: Context) {

  companion object {
    private const val APP_NAME = "NotDefterim"
    private const val BACKUP_FILE_NAME = "notdefterim_backup.json"
    private const val MIME_TYPE_JSON = "application/json"
    /** Eski yedekleri temizlerken bu kadar dosyayı koruruz. */
    private const val MAX_BACKUP_FILES = 5
  }

  /**
   * Drive servis istemcisi — her çağrıda yeniden oluşturulur.
   * Token yenileme GoogleAccountCredential tarafından yönetilir.
   */
  private fun buildDriveService(googleAuthManager: GoogleAuthManager): Drive? {
    val account = googleAuthManager.getCurrentAccount() ?: return null

    val credential = GoogleAccountCredential.usingOAuth2(
      context,
      listOf(DriveScopes.DRIVE_APPDATA)
    ).apply { selectedAccount = account.account }

    return Drive.Builder(
      com.google.api.client.http.javanet.NetHttpTransport(),
      GsonFactory.getDefaultInstance(),
      credential
    )
      .setApplicationName(APP_NAME)
      .build()
  }

  /**
   * JSON içeriğini appDataFolder'a yükler.
   * Aynı isimde varsa günceller (overwrite), yoksa oluşturur.
   * Her yedekte timestamp eklenmiş dosya adı kullanılır, eski yedekler temizlenir.
   */
  suspend fun uploadBackup(
    jsonContent: String,
    googleAuthManager: GoogleAuthManager
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val driveService = buildDriveService(googleAuthManager)
        ?: return@withContext Result.failure(IllegalStateException("Drive servisi başlatılamadı"))

      val timestamp = System.currentTimeMillis()
      val fileName = "notdefterim_backup_$timestamp.json"

      val fileMetadata = File().apply {
        name = fileName
        parents = listOf("appDataFolder")
      }

      val contentStream = ByteArrayContent.fromString(MIME_TYPE_JSON, jsonContent)

      val uploadedFile = driveService.files()
        .create(fileMetadata, contentStream)
        .setFields("id, name, createdTime")
        .execute()

      // Eski yedekleri temizle (en fazla MAX_BACKUP_FILES tut)
      cleanupOldBackups(driveService)

      Result.success(uploadedFile.id)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * appDataFolder'daki en son yedeği indirir.
   * Zaman damgasına göre en yeni dosyayı seçer.
   */
  suspend fun downloadLatestBackup(
    googleAuthManager: GoogleAuthManager
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val driveService = buildDriveService(googleAuthManager)
        ?: return@withContext Result.failure(IllegalStateException("Drive servisi başlatılamadı"))

      val fileList = driveService.files().list()
        .setSpaces("appDataFolder")
        .setFields("files(id, name, createdTime)")
        .setOrderBy("createdTime desc")
        .setPageSize(1)
        .execute()

      val latestFile = fileList.files.firstOrNull()
        ?: return@withContext Result.failure(NoSuchElementException("Drive'da yedek bulunamadı"))

      val outputStream = ByteArrayOutputStream()
      driveService.files().get(latestFile.id)
        .executeMediaAndDownloadTo(outputStream)

      Result.success(outputStream.toString(Charsets.UTF_8.name()))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Belirli bir yedeği ID'sine göre indirir.
   */
  suspend fun downloadBackup(
    backupId: String,
    googleAuthManager: GoogleAuthManager
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val driveService = buildDriveService(googleAuthManager)
        ?: return@withContext Result.failure(IllegalStateException("Drive servisi başlatılamadı"))

      val outputStream = ByteArrayOutputStream()
      driveService.files().get(backupId)
        .executeMediaAndDownloadTo(outputStream)

      Result.success(outputStream.toString(Charsets.UTF_8.name()))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /** Drive'daki tüm yedekleri listeler. */
  suspend fun listBackups(
    googleAuthManager: GoogleAuthManager
  ): Result<List<BackupInfo>> = withContext(Dispatchers.IO) {
    try {
      val driveService = buildDriveService(googleAuthManager)
        ?: return@withContext Result.failure(IllegalStateException("Drive servisi başlatılamadı"))

      val fileList = driveService.files().list()
        .setSpaces("appDataFolder")
        .setFields("files(id, name, createdTime, size)")
        .setOrderBy("createdTime desc")
        .execute()

      val backups = fileList.files.map { file ->
        BackupInfo(
          id = file.id,
          name = file.name,
          createdTimeMillis = file.createdTime?.value ?: 0L,
          sizeBytes = file.getSize() ?: 0L
        )
      }

      Result.success(backups)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /** MAX_BACKUP_FILES sınırını aşan eski dosyaları siler. */
  private fun cleanupOldBackups(driveService: Drive) {
    try {
      val fileList = driveService.files().list()
        .setSpaces("appDataFolder")
        .setFields("files(id, name, createdTime)")
        .setOrderBy("createdTime desc")
        .execute()

      val filesToDelete = fileList.files.drop(MAX_BACKUP_FILES)
      filesToDelete.forEach { file ->
        driveService.files().delete(file.id).execute()
      }
    } catch (e: Exception) {
      // Temizleme başarısız olsa da yükleme işlemi etkilenmesin
      android.util.Log.w("DriveServiceHelper", "Eski yedek temizleme hatası: ${e.message}")
    }
  }
}

/** Drive'daki bir yedek dosyasının özet bilgisi. */
data class BackupInfo(
  val id: String,
  val name: String,
  val createdTimeMillis: Long,
  val sizeBytes: Long
)
