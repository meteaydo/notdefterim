package com.notdefterim.app.data.repository

import com.notdefterim.app.data.repository.GoogleDriveBackupRepositoryImpl.Companion.LEGACY_WRAP_PASSWORD
import com.notdefterim.app.data.repository.GoogleDriveBackupRepositoryImpl.Companion.unwrapPassphrase
import com.notdefterim.app.data.repository.GoogleDriveBackupRepositoryImpl.Companion.wrapPassphrase
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Drive yedeğindeki anahtar: şifre tanımlanmadan derlenmiş sürümlerin yer tutucuyla aldığı
 * yedekler, gerçek şifreyle derlenen sürümde de açılabilmeli.
 */
class DriveBackupKeyWrapTest {

  private val passphrase = ByteArray(32) { it.toByte() }
  private val restoreOrder = listOf("gercek-sifre", LEGACY_WRAP_PASSWORD)

  @Test
  fun gercek_sifreyle_alinan_yedek_acilir() {
    val payload = wrapPassphrase(passphrase, "gercek-sifre")
    assertArrayEquals(passphrase, unwrapPassphrase(payload, restoreOrder))
  }

  @Test
  fun yer_tutucuyla_alinan_eski_yedek_acilir() {
    val payload = wrapPassphrase(passphrase, LEGACY_WRAP_PASSWORD)
    assertArrayEquals(passphrase, unwrapPassphrase(payload, restoreOrder))
  }

  @Test
  fun bilinmeyen_sifreyle_alinan_yedek_null_doner() {
    val payload = wrapPassphrase(passphrase, "baska-bir-sifre")
    assertNull(unwrapPassphrase(payload, restoreOrder))
  }
}
