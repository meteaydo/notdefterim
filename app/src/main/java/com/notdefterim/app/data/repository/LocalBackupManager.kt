package com.notdefterim.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.notdefterim.app.core.security.EncryptionHelper
import com.notdefterim.app.data.local.NoteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class LocalBackupManager @Inject constructor(
  @ApplicationContext private val context: Context,
  private val encryptionHelper: EncryptionHelper,
  private val noteDatabase: NoteDatabase
) {

  companion object {
    private const val DB_NAME = "notdefterim_encrypted.db"
    private const val MAGIC_HEADER = "NOTDEF"
    /** v2: DB passphrase artık yedek şifresiyle ayrıca şifreleniyor */
    private const val VERSION = 2
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256
    /** Passphrase sarmalama anahtarı için ayrı salt boyutu */
    private const val KEY_WRAP_SALT_SIZE = 16
  }

  suspend fun exportToFile(password: String, hint: String): Result<File> = withContext(Dispatchers.IO) {
    try {
      noteDatabase.query(androidx.sqlite.db.SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))

      // Passphrase'ı yedek şifresiyle sar (wrap) — ZIP oluşturmadan önce
      val zipFile = createBackupZip(password)

      val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
      val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
      val tmp = factory.generateSecret(spec)
      val secretKey = SecretKeySpec(tmp.encoded, "AES")

      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
      val gcmSpec = GCMParameterSpec(128, iv)
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

      val backupDir = File(context.cacheDir, "backups")
      if (!backupDir.exists()) backupDir.mkdirs()
      
      val dateFormat = android.text.format.DateFormat.format("dd.MM.yyyy", java.util.Date())
      val prefix = context.getString(com.notdefterim.app.R.string.backup_file_prefix)
      val finalFile = File(backupDir, "$prefix ($dateFormat).notdefterim")

      FileOutputStream(finalFile).use { os ->
        DataOutputStream(BufferedOutputStream(os)).use { dos ->
          dos.writeUTF(MAGIC_HEADER)
          dos.writeInt(VERSION)
          dos.writeUTF(hint)
          
          dos.writeInt(salt.size)
          dos.write(salt)
          
          dos.writeInt(iv.size)
          dos.write(iv)

          FileInputStream(zipFile).use { fis ->
            val inputBytes = fis.readBytes() 
            val encryptedBytes = cipher.doFinal(inputBytes)
            dos.writeInt(encryptedBytes.size)
            dos.write(encryptedBytes)
          }
        }
      }

      zipFile.delete()
      Result.success(finalFile)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun exportToUri(uri: Uri, password: String, hint: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val fileResult = exportToFile(password, hint)
      if (fileResult.isSuccess) {
        val file = fileResult.getOrNull()!!
        context.contentResolver.openOutputStream(uri)?.use { os ->
          FileInputStream(file).use { fis ->
            fis.copyTo(os)
          }
        } ?: throw Exception(context.getString(com.notdefterim.app.R.string.target_file_error))
        Result.success(Unit)
      } else {
        Result.failure(fileResult.exceptionOrNull() ?: Exception("Unknown error"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getHintFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
    try {
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        DataInputStream(BufferedInputStream(inputStream)).use { dis ->
          val magic = dis.readUTF()
          if (magic != MAGIC_HEADER) throw Exception(context.getString(com.notdefterim.app.R.string.invalid_file_format))
          val version = dis.readInt()
          val hint = dis.readUTF()
          Result.success(hint)
        }
      } ?: Result.failure(Exception(context.getString(com.notdefterim.app.R.string.file_read_failed, "")))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun importFromUri(uri: Uri, password: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val zipFile = File(context.cacheDir, "decrypted_backup.zip")

      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        DataInputStream(BufferedInputStream(inputStream)).use { dis ->
          val magic = dis.readUTF()
          if (magic != MAGIC_HEADER) throw Exception(context.getString(com.notdefterim.app.R.string.invalid_backup_file))
          
          val version = dis.readInt()
          val hint = dis.readUTF()

          val saltSize = dis.readInt()
          val salt = ByteArray(saltSize)
          dis.readFully(salt)

          val ivSize = dis.readInt()
          val iv = ByteArray(ivSize)
          dis.readFully(iv)

          val encryptedSize = dis.readInt()
          val encryptedBytes = ByteArray(encryptedSize)
          dis.readFully(encryptedBytes)

          val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
          val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
          val tmp = factory.generateSecret(spec)
          val secretKey = SecretKeySpec(tmp.encoded, "AES")

          val cipher = Cipher.getInstance("AES/GCM/NoPadding")
          val gcmSpec = GCMParameterSpec(128, iv)
          cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

          val decryptedBytes = try {
             cipher.doFinal(encryptedBytes)
          } catch(e: Exception) {
             throw Exception(context.getString(com.notdefterim.app.R.string.incorrect_password))
          }

          FileOutputStream(zipFile).use { fos ->
            fos.write(decryptedBytes)
          }

          // v2 yedeklerinde passphrase ZIP içinde şifrelendi;
          // v1 yedeklerinde ise passphrase ZIP içinde açıkça yer alıyordu.
          extractBackupZip(zipFile, if (version >= 2) password else null)
        }
      } ?: throw Exception(context.getString(com.notdefterim.app.R.string.file_read_failed, ""))

      noteDatabase.close()
      zipFile.delete()

      val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
      if (intent != null) {
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
      }

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Veritabanı dosyalarını ve şifreleme anahtarını (passphrase) tek bir ZIP'te birleştirir.
   *
   * Güvenlik: Passphrase'i plain-text yazmak yerine kullanıcının yedek şifresiyle
   * türetilmiş ayrı bir AES-GCM anahtarıyla şifreler.
   * Format: [salt(16)] + [iv(12)] + [AES-GCM(passphrase)]
   */
  private fun createBackupZip(backupPassword: String): File {
    val zipFile = File(context.cacheDir, "local_backup.zip")
    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
      val dbFile = context.getDatabasePath(DB_NAME)
      val filesToZip = listOf(
        dbFile,
        File(dbFile.path + "-wal"),
        File(dbFile.path + "-shm")
      ).filter { it.exists() }

      filesToZip.forEach { file ->
        zos.putNextEntry(ZipEntry(file.name))
        FileInputStream(file).use { fis -> fis.copyTo(zos) }
        zos.closeEntry()
      }

      // Passphrase'ı yedek şifresiyle türetilen anahtar ile şifreliyoruz
      val passphrase = encryptionHelper.getDatabasePassphrase()
      val wrapSalt = ByteArray(KEY_WRAP_SALT_SIZE).apply { SecureRandom().nextBytes(this) }
      val wrapSpec: KeySpec = PBEKeySpec(backupPassword.toCharArray(), wrapSalt, ITERATION_COUNT, KEY_LENGTH)
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
   * ZIP içindeki passphrase'i çözer ve veritabanı dosyalarını geri yükler.
   *
   * @param backupPassword v2 yedekler için gerekli; null ise passphrase plain-text olarak okunur (v1 uyumluluğu).
   */
  private fun extractBackupZip(zipFile: File, backupPassword: String?) {
    ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        // Güvenlik: ZIP Slip saldırısını önlemek için entry adını temizle
        val safeEntryName = entry.name.replace("..", "").trimStart('/')

        if (safeEntryName == "key.config") {
          val keyPayload = zis.readBytes()
          val passphrase = if (backupPassword != null && keyPayload.size > 28) {
            // v2: şifrelemiş payload'ı çöz
            val wrapSalt = keyPayload.copyOfRange(0, KEY_WRAP_SALT_SIZE)
            val wrapIv = keyPayload.copyOfRange(KEY_WRAP_SALT_SIZE, KEY_WRAP_SALT_SIZE + 12)
            val encryptedPassphrase = keyPayload.copyOfRange(KEY_WRAP_SALT_SIZE + 12, keyPayload.size)

            val wrapSpec: KeySpec = PBEKeySpec(backupPassword.toCharArray(), wrapSalt, ITERATION_COUNT, KEY_LENGTH)
            val wrapFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val wrapKey = SecretKeySpec(wrapFactory.generateSecret(wrapSpec).encoded, "AES")

            val wrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
            wrapCipher.init(Cipher.DECRYPT_MODE, wrapKey, GCMParameterSpec(128, wrapIv))
            wrapCipher.doFinal(encryptedPassphrase)
          } else {
            // v1 uyumluluk: Base64 plain-text
            android.util.Base64.decode(keyPayload.toString(Charsets.UTF_8), android.util.Base64.NO_WRAP)
          }
          encryptionHelper.restoreDatabasePassphrase(passphrase)
        } else if (safeEntryName.startsWith(DB_NAME)) {
          // Güvenli yol doğrulaması — getDatabasePath ile kanonik yol al
          val targetFile = context.getDatabasePath(safeEntryName)
          val canonicalDb = context.getDatabasePath(DB_NAME).canonicalPath
          if (targetFile.canonicalPath.startsWith(canonicalDb.removeSuffix(DB_NAME))) {
            FileOutputStream(targetFile).use { fos ->
              zis.copyTo(fos)
            }
          } else {
            android.util.Log.w("LocalBackupManager", "Güvensiz ZIP entry'si atlandı: ${entry.name}")
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }
  }
}
