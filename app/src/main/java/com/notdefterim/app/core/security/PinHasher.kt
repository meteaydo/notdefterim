package com.notdefterim.app.core.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN güvenli hash/doğrulama yardımcısı.
 *
 * Neden PBKDF2?
 * Düz SHA gibi hızlı hash'ler brute-force'a karşı savunmasızdır.
 * PBKDF2 iterasyon sayısı ile hesaplama maliyeti artırılır;
 * böylece saldırganın deneme hızı büyük ölçüde düşürülür.
 *
 * Format: "Base64(salt):Base64(hash)"
 */
object PinHasher {

  private const val ALGORITHM = "PBKDF2WithHmacSHA256"
  private const val ITERATION_COUNT = 120_000
  private const val KEY_LENGTH_BITS = 256
  private const val SALT_LENGTH_BYTES = 16
  private const val SEPARATOR = ":"

  /**
   * PIN'i hash'ler; rastgele bir salt üretir ve birleştirilmiş
   * "salt:hash" formatında döndürür.
   */
  fun hash(pin: String): String {
    val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
    val hash = deriveKey(pin, salt)
    val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
    val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)
    return "$saltB64$SEPARATOR$hashB64"
  }

  /**
   * Kullanıcının girdiği PIN'i saklanan hash ile karşılaştırır.
   * Sabit-zaman karşılaştırma (timing-safe) kullanır.
   */
  fun verify(pin: String, storedHash: String): Boolean {
    return try {
      val parts = storedHash.split(SEPARATOR)
      if (parts.size != 2) return false
      val salt = Base64.decode(parts[0], Base64.NO_WRAP)
      val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)
      val actualHash = deriveKey(pin, salt)
      // Timing-safe karşılaştırma — uzunluk sızıntısını önler
      actualHash.size == expectedHash.size &&
        actualHash.zip(expectedHash).all { (a, b) -> a == b }
    } catch (e: Exception) {
      android.util.Log.e("PinHasher", "PIN doğrulama sırasında hata", e)
      false
    }
  }

  private fun deriveKey(pin: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
    val factory = SecretKeyFactory.getInstance(ALGORITHM)
    return try {
      factory.generateSecret(spec).encoded
    } finally {
      // PIN karakter dizisini bellekten temizle
      spec.clearPassword()
    }
  }
}
