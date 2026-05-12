package com.notdefterim.app.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
      "secure_db_prefs",
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
  }

  companion object {
    private const val KEY_DB_PASSPHRASE = "db_passphrase_b64"
    private const val PASSPHRASE_LENGTH_BYTES = 32 // 256 bit
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

    securePreferences.edit()
      .putString(KEY_DB_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
      .apply()

    return passphrase
  }

  /**
   * Buluttan indirilen yedeğin anahtarını yerel cihaza kaydeder.
   */
  fun restoreDatabasePassphrase(passphrase: ByteArray) {
    securePreferences.edit()
      .putString(KEY_DB_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
      .apply()
  }

  /**
   * Passphrase'i bellekten güvenli biçimde siler.
   * Room bağlantısı kurulduktan sonra çağrılmalıdır.
   */
  fun wipePassphraseFromMemory(passphrase: ByteArray) {
    passphrase.fill(0)
  }
}
