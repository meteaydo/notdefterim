package com.notdefterim.app.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom

/**
 * SQLCipher için AES-256 passphrase yönetimi.
 *
 * Neden iki katmanlı bir yapı?
 * - EncryptedSharedPreferences zaten Android Keystore'u kullanır.
 * - Passphrase'i bu şekilde saklayarak hem Keystore koruması
 *   hem de SQLCipher'ın tam veritabanı şifrelemesinden faydalanıyoruz.
 * - İlk çalıştırmada SecureRandom ile passphrase üretilir; sonraki
 *   açılışlarda aynı passphrase geri okunur.
 */
class EncryptionHelper(context: Context) {

  private val appContext = context.applicationContext

  private val securePreferences: SharedPreferences by lazy {
    val masterKey = MasterKey.Builder(appContext)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()

    EncryptedSharedPreferences.create(
      appContext,
      PREFS_NAME,
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
  }

  companion object {
    private const val TAG = "EncryptionHelper"
    private const val PREFS_NAME = "secure_db_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase_b64"
    private const val KEY_DB_KEY_VERIFIED = "db_key_verified"
    private const val PASSPHRASE_LENGTH_BYTES = 32 // 256 bit
  }

  /**
   * Saklanan anahtar okunabiliyor mu?
   *
   * Keystore anahtarı bozulur ya da silinirse (sistem güncellemesi, güvenlik ayarlarının
   * sıfırlanması) şifreli tercihler açılamaz. Veritabanı varken anahtar kaybolmuşsa da veriler
   * açılamaz; bu durumda yeni anahtar üretmek yerine kullanıcıya açıklama gösterilmeli.
   */
  fun isKeyUsable(databaseExists: Boolean): Boolean = try {
    securePreferences.getString(KEY_DB_PASSPHRASE, null) != null || !databaseExists
  } catch (e: Exception) {
    Log.e(TAG, "Şifreli tercihler açılamadı", e)
    false
  }

  /**
   * Anahtar kaybından sonra şifreli tercihleri ve Keystore anahtarını siler; sonraki açılışta
   * yeni anahtar üretilir. Bu anahtarla şifrelenmiş veritabanını çağıran ayrıca silmelidir.
   */
  fun resetAfterKeyLoss() {
    appContext.deleteSharedPreferences(PREFS_NAME)
    try {
      KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
    } catch (e: Exception) {
      Log.e(TAG, "Keystore anahtarı silinemedi", e)
    }
  }

  /**
   * SQLCipher veritabanı için passphrase döndürür.
   * İlk çağrıda yeni passphrase üretir ve güvenli biçimde saklar.
   */
  fun getDatabasePassphrase(): ByteArray {
    val stored = securePreferences.getString(KEY_DB_PASSPHRASE, null)
    if (stored != null) {
      return Base64.decode(stored, Base64.NO_WRAP)
    }

    // İlk kurulum: 256-bit güçlü rastgele passphrase üret
    val passphrase = ByteArray(PASSPHRASE_LENGTH_BYTES).also {
      SecureRandom().nextBytes(it)
    }

    // commit(): anahtar diske yazılmadan veritabanı bu anahtarla oluşursa veri açılamaz hale gelir
    securePreferences.edit()
      .putString(KEY_DB_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
      .commit()

    return passphrase
  }

  /**
   * Yedekten gelen anahtarı yerel cihaza kaydeder.
   *
   * Hemen ardından süreç yeniden başlatıldığı için commit() ile eşzamanlı yazılır. Yedekteki
   * veritabanı 1.0.0'dan kalma (sıfır anahtarlı) olabileceği için doğrulama bayrağı sıfırlanır.
   */
  fun restoreDatabasePassphrase(passphrase: ByteArray) {
    securePreferences.edit()
      .putString(KEY_DB_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
      .putBoolean(KEY_DB_KEY_VERIFIED, false)
      .commit()
  }

  /** Veritabanının saklanan anahtarla açıldığı doğrulandı mı (bkz. DatabaseKeyMigration). */
  fun isDatabaseKeyVerified(): Boolean = securePreferences.getBoolean(KEY_DB_KEY_VERIFIED, false)

  fun markDatabaseKeyVerified() {
    securePreferences.edit().putBoolean(KEY_DB_KEY_VERIFIED, true).commit()
  }

  /**
   * Passphrase'i bellekten güvenli biçimde siler.
   * Room bağlantısı kurulduktan sonra çağrılmalıdır.
   */
  fun wipePassphraseFromMemory(passphrase: ByteArray) {
    passphrase.fill(0)
  }
}
