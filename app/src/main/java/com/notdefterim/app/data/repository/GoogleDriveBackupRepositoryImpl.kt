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
import com.notdefterim.app.data.local.NoteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
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

  /** Veritabanı dosyalarını ve şifreleme anahtarını (passphrase) tek bir ZIP'te birleştirir. */
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

      // Anahtar (Passphrase) dosyası
      val passphrase = encryptionHelper.getDatabasePassphrase()
      val passphraseB64 = Base64.encodeToString(passphrase, Base64.NO_WRAP)
      zos.putNextEntry(ZipEntry("key.config"))
      zos.write(passphraseB64.toByteArray())
      zos.closeEntry()
    }
    return zipFile
  }

  /** İndirilen ZIP dosyasını açar ve veritabanı ile anahtarı doğru yerlerine yerleştirir. */
  private fun extractBackupZip(zipFile: java.io.File) {
    ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (entry.name == "key.config") {
          // Anahtarı geri yükle
          val keyB64 = zis.readBytes().toString(Charsets.UTF_8)
          val passphrase = Base64.decode(keyB64, Base64.NO_WRAP)
          encryptionHelper.restoreDatabasePassphrase(passphrase)
        } else if (entry.name.startsWith(DB_NAME)) {
          // Veritabanı dosyalarını geri yükle
          val dbFile = context.getDatabasePath(entry.name)
          FileOutputStream(dbFile).use { fos ->
            zis.copyTo(fos)
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }
  }
}
