package com.notdefterim.app.data.repository

import android.content.Context
import android.util.Base64
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.notdefterim.app.core.security.EncryptionHelper
import com.notdefterim.app.data.remote.GoogleAuthManager
import com.notdefterim.app.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import com.notdefterim.app.BuildConfig
import com.notdefterim.app.data.local.NoteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class GoogleDriveBackupRepositoryImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val authManager: GoogleAuthManager,
  private val encryptionHelper: EncryptionHelper,
  private val noteDatabase: NoteDatabase
) : BackupRepository {

  companion object {
    private const val BACKUP_FILE_PREFIX = "notdefterim_backup_"
    private const val MIME_TYPE_ZIP = "application/zip"
    private const val DB_NAME = "notdefterim_encrypted.db"
    private const val MAX_BACKUP_FILES = 5
    /** Passphrase sarmalama için sabitler */
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256
    private const val KEY_WRAP_SALT_SIZE = 16
    // DRIVE_BACKUP_WRAP_PASSWORD artık BuildConfig üzerinden okunuyor (local.properties'tan gelir)
  }

  override suspend fun backupToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val driveService = getDriveService() ?: return@withContext Result.failure(Exception("Google hesabı ile giriş yapılmamış."))

      // Açık tüm SQLite işlemlerini (WAL) ana dosyaya yazmaya zorla (Checkpoint)
      noteDatabase.query(androidx.sqlite.db.SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))

      // 1. Kasa ve Anahtarı hazırlayıp ZIP'le
      val backupZipFile = createBackupZip()

      val timestamp = System.currentTimeMillis()
      val backupFileName = "${BACKUP_FILE_PREFIX}${timestamp}.zip"
      val fileContent = FileContent(MIME_TYPE_ZIP, backupZipFile)

      val fileMetadata = File().apply {
        name = backupFileName
        parents = listOf("appDataFolder")
      }
      driveService.files().create(fileMetadata, fileContent).execute()

      // Eski yedekleri temizle (en fazla MAX_BACKUP_FILES tut)
      val fileList = driveService.files().list()
        .setSpaces("appDataFolder")
        .setQ("name contains '$BACKUP_FILE_PREFIX'")
        .setFields("files(id, name, createdTime)")
        .setOrderBy("createdTime desc")
        .execute()

      val filesToDelete = fileList.files.drop(MAX_BACKUP_FILES)
      filesToDelete.forEach { file ->
        driveService.files().delete(file.id).execute()
      }

      // Geçici ZIP dosyasını sil
      backupZipFile.delete()

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun restoreFromCloud(backupId: String?): Result<Int> = withContext(Dispatchers.IO) {
    try {
      val driveService = getDriveService() ?: return@withContext Result.failure(Exception("Google hesabı ile giriş yapılmamış."))

      // 1. Drive'daki yedeği bul
      val existingFile = if (backupId != null) {
        driveService.files().get(backupId)
          .setFields("id, name")
          .execute()
      } else {
        val fileList = driveService.files().list()
          .setSpaces("appDataFolder")
          .setQ("name contains '$BACKUP_FILE_PREFIX'")
          .setOrderBy("createdTime desc")
          .setPageSize(1)
          .execute()
        fileList.files.firstOrNull()
      }
        ?: return@withContext Result.failure(Exception("Drive'da herhangi bir yedek bulunamadı."))

      // 2. Yedeği indir
      val downloadedZip = java.io.File(context.cacheDir, "downloaded_backup.zip")
      FileOutputStream(downloadedZip).use { outputStream ->
        driveService.files().get(existingFile.id).executeMediaAndDownloadTo(outputStream)
      }

      // ÖNCE Veritabanını tamamen kapat (Yoksa Room kapanırken bellekteki eski verileri dosyanın üzerine geri yazar)
      noteDatabase.close()

      // 3. ZIP'i aç ve Kasa ile Anahtarı yerine koy
      extractBackupZip(downloadedZip)
      downloadedZip.delete()

      // Uygulamayı yeniden başlat ki yeni veritabanını okusun
      val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
      if (intent != null) {
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
      }

      // Not sayısı vb. hesaplanabilir, şimdilik başarılı anlamında 1 dönüyoruz
      Result.success(1)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun listBackups(): Result<List<com.notdefterim.app.data.remote.BackupInfo>> = withContext(Dispatchers.IO) {
    try {
      val driveService = getDriveService() ?: return@withContext Result.failure(Exception("Google hesabı ile giriş yapılmamış."))

      val fileList = driveService.files().list()
        .setSpaces("appDataFolder")
        .setQ("name contains '$BACKUP_FILE_PREFIX'")
        .setFields("files(id, name, createdTime, size)")
        .setOrderBy("createdTime desc")
        .execute()

      val backups = fileList.files.map { file ->
        com.notdefterim.app.data.remote.BackupInfo(
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

  private fun getDriveService(): Drive? {
    val account = authManager.getCurrentAccount() ?: return null
    val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
    credential.selectedAccount = account.account

    return Drive.Builder(
      com.google.api.client.http.javanet.NetHttpTransport(),
      GsonFactory.getDefaultInstance(),
      credential
    )
      .setApplicationName("NotDefterim")
      .build()
  }

  /**
   * Veritabanı dosyalarını ve şifreleme anahtarını tek bir ZIP'te birleştirir.
   *
   * Güvenlik: Passphrase plain-text yazılmaz;
   * sabit bir sarmalama şifresinden PBKDF2 ile türetilen AES-GCM anahtarıyla şifrelenir.
   */
  private fun createBackupZip(): java.io.File {
    val zipFile = java.io.File(context.cacheDir, "temp_backup.zip")
    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
      // Veritabanı dosyaları (db, wal, shm)
      val dbFile = context.getDatabasePath(DB_NAME)
      val filesToZip = listOf(
        dbFile,
        java.io.File(dbFile.path + "-wal"),
        java.io.File(dbFile.path + "-shm")
      ).filter { it.exists() }

      filesToZip.forEach { file ->
        zos.putNextEntry(ZipEntry(file.name))
        FileInputStream(file).use { fis -> fis.copyTo(zos) }
        zos.closeEntry()
      }

      // Passphrase'ı BuildConfig'ten gelen sarmalama şifresinden türetilen AES-GCM anahtarıyla şifreliyoruz
      val passphrase = encryptionHelper.getDatabasePassphrase()
      val wrapSalt = ByteArray(KEY_WRAP_SALT_SIZE).apply { SecureRandom().nextBytes(this) }
      val wrapSpec: KeySpec = PBEKeySpec(BuildConfig.DRIVE_BACKUP_WRAP_PASSWORD.toCharArray(), wrapSalt, ITERATION_COUNT, KEY_LENGTH)
      val wrapFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      val wrapKey = SecretKeySpec(wrapFactory.generateSecret(wrapSpec).encoded, "AES")
      val wrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
      val wrapIv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
      wrapCipher.init(Cipher.ENCRYPT_MODE, wrapKey, GCMParameterSpec(128, wrapIv))
      val encryptedPassphrase = wrapCipher.doFinal(passphrase)

      // Format: salt(16) | iv(12) | encryptedPassphrase
      val keyConfigPayload = wrapSalt + wrapIv + encryptedPassphrase
      zos.putNextEntry(ZipEntry("key.config"))
      zos.write(keyConfigPayload)
      zos.closeEntry()
    }
    return zipFile
  }

  /**
   * İndirilen ZIP dosyasını açar, passphrase'ı çözer ve veritabanı + anahtarı doğru yerlerine yerleştirir.
   * Güvenlik: ZIP Slip saldırılarına karşı entry adları dolayılaması denetlenir.
   */
  private fun extractBackupZip(zipFile: java.io.File) {
    ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        // Güvenlik: ZIP Slip saldırısını önlemek için entry adını temizle
        val safeEntryName = entry.name.replace("..", "").trimStart('/')

        if (safeEntryName == "key.config") {
          val keyPayload = zis.readBytes()
          val passphrase = if (keyPayload.size > 28) {
            // Yeni format: şifrelemiş payload'ı çöz
            val wrapSalt = keyPayload.copyOfRange(0, KEY_WRAP_SALT_SIZE)
            val wrapIv = keyPayload.copyOfRange(KEY_WRAP_SALT_SIZE, KEY_WRAP_SALT_SIZE + 12)
            val encryptedPassphrase = keyPayload.copyOfRange(KEY_WRAP_SALT_SIZE + 12, keyPayload.size)

            val wrapSpec: KeySpec = PBEKeySpec(BuildConfig.DRIVE_BACKUP_WRAP_PASSWORD.toCharArray(), wrapSalt, ITERATION_COUNT, KEY_LENGTH)
            val wrapFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val wrapKey = SecretKeySpec(wrapFactory.generateSecret(wrapSpec).encoded, "AES")
            val wrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
            wrapCipher.init(Cipher.DECRYPT_MODE, wrapKey, GCMParameterSpec(128, wrapIv))
            wrapCipher.doFinal(encryptedPassphrase)
          } else {
            // Eski format uyumluluk: Base64 plain-text
            android.util.Base64.decode(keyPayload.toString(Charsets.UTF_8), android.util.Base64.NO_WRAP)
          }
          encryptionHelper.restoreDatabasePassphrase(passphrase)
        } else if (safeEntryName.startsWith(DB_NAME)) {
          // Güvenli yol doğrulaması
          val targetFile = context.getDatabasePath(safeEntryName)
          val canonicalDb = context.getDatabasePath(DB_NAME).canonicalPath
          if (targetFile.canonicalPath.startsWith(canonicalDb.removeSuffix(DB_NAME))) {
            FileOutputStream(targetFile).use { fos ->
              zis.copyTo(fos)
            }
          } else {
            android.util.Log.w("DriveBackup", "Güvensiz ZIP entry'si atlandı: ${entry.name}")
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }
  }
}
