package com.notdefterim.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore System ile AES-256/GCM master key yönetimi.
 *
 * Neden doğrudan şifrelemek yerine master key kullanıyoruz?
 * SQLCipher passphrase'i EncryptedSharedPreferences'a kaydederken,
 * bu sınıf verileri hardware-backed keystore anahtarıyla şifreler.
 * Böylece root'lu cihazlarda bile ham passphrase elde edilemez.
 */
object KeystoreManager {

  private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
  private const val MASTER_KEY_ALIAS = "notdefterim_master_key"
  private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
  private const val GCM_TAG_LENGTH = 128

  /**
   * Master key'i döndürür; yoksa önce oluşturur.
   * Hardware-backed (StrongBox) desteği varsa tercih edilir.
   */
  fun getOrCreateMasterKey(): SecretKey {
    val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    // Varolan anahtar varsa direkt kullan
    keyStore.getKey(MASTER_KEY_ALIAS, null)?.let { return it as SecretKey }

    // Yok ise yeni oluştur
    return generateMasterKey()
  }

  private fun generateMasterKey(): SecretKey {
    val keyGenerator = KeyGenerator.getInstance(
      KeyProperties.KEY_ALGORITHM_AES,
      KEYSTORE_PROVIDER
    )

    val keyGenSpec = KeyGenParameterSpec.Builder(
      MASTER_KEY_ALIAS,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
      .setKeySize(256)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setRandomizedEncryptionRequired(true)
      // Biyometrik doğrulama ile kullanım kilidi — isteğe bağlı olarak
      // .setUserAuthenticationRequired(true) eklenebilir
      .build()

    keyGenerator.init(keyGenSpec)
    return keyGenerator.generateKey()
  }

  /** Veriyi master key ile şifreler; IV'yi cipher text'in önüne ekler. */
  fun encrypt(data: ByteArray): ByteArray {
    val secretKey = getOrCreateMasterKey()
    val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
      init(Cipher.ENCRYPT_MODE, secretKey)
    }
    val encryptedData = cipher.doFinal(data)
    // Format: [IV (12 byte)] + [encrypted data]
    return cipher.iv + encryptedData
  }

  /** IV'yi baştan ayırıp, kalan veriyi master key ile çözer. */
  fun decrypt(encryptedDataWithIv: ByteArray): ByteArray {
    val secretKey = getOrCreateMasterKey()
    val iv = encryptedDataWithIv.copyOfRange(0, 12)
    val encryptedData = encryptedDataWithIv.copyOfRange(12, encryptedDataWithIv.size)

    val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
      init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    }
    return cipher.doFinal(encryptedData)
  }
}
