package com.notdefterim.app.data.local

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * Sıfır anahtarla şifrelenmiş veritabanını gerçek anahtara taşır.
 *
 * 1.0.0 sürümünde anahtar, Room veritabanını açmadan önce bellekte sıfırlandığı için bütün
 * veritabanları 32 sıfır baytlık anahtarla oluştu. Bu durum algılanırsa veritabanı bir kez gerçek
 * anahtarla yeniden şifrelenir (rekey). O sürümden alınmış yedekler geri yüklendiğinde de aynı yol
 * çalışır; yedekteki veritabanı sıfır anahtarlıdır.
 */
object DatabaseKeyMigration {

  private const val TAG = "DatabaseKeyMigration"

  /** 1.0.0'ın yanlışlıkla kullandığı anahtar. */
  private fun legacyZeroKey() = ByteArray(32)

  /**
   * @return veritabanı yoksa ya da [key] ile açılıyorsa (gerekirse taşındıktan sonra) true;
   * hiçbir bilinen anahtarla açılamıyorsa false (dosyaya dokunulmaz).
   */
  fun migrateIfNeeded(context: Context, key: ByteArray): Boolean {
    val dbFile = context.getDatabasePath(NoteDatabase.DATABASE_NAME)
    if (!dbFile.exists()) return true
    System.loadLibrary("sqlcipher")
    if (canOpen(dbFile.path, key)) return true

    if (!canOpen(dbFile.path, legacyZeroKey())) {
      Log.e(TAG, "Veritabanı bilinen hiçbir anahtarla açılamadı")
      return false
    }

    SQLiteDatabase.openDatabase(dbFile.path, legacyZeroKey(), null, SQLiteDatabase.OPEN_READWRITE, null, null).use { db ->
      // Rekey'den önce WAL'daki kayıtları ana dosyaya yaz ve tek dosyalı kipe geç;
      // Room veritabanını açarken yeniden WAL kipine alır.
      db.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
      db.query("PRAGMA journal_mode = DELETE").close()
      db.changePassword(key.copyOf())
    }
    Log.i(TAG, "Veritabanı gerçek anahtara taşındı")
    return canOpen(dbFile.path, key)
  }

  /**
   * [delegate]'in açtığı veritabanını, ilk açılıştan hemen önce [beforeFirstOpen]'dan geçirir.
   * Room veritabanını arka plan iş parçacığında açtığı için anahtar kontrolü ve taşıma ana iş
   * parçacığını kilitlemez; DI sağlayıcısında yapıldığında anahtar türetimi ANR'a yol açıyordu.
   */
  fun openHelperFactory(
    delegate: SupportSQLiteOpenHelper.Factory,
    beforeFirstOpen: () -> Unit
  ): SupportSQLiteOpenHelper.Factory = object : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
      val helper = delegate.create(configuration)
      return object : SupportSQLiteOpenHelper by helper {
        private var ready = false

        private fun ensureReady() = synchronized(this) {
          if (!ready) {
            beforeFirstOpen()
            ready = true
          }
        }

        override val writableDatabase: SupportSQLiteDatabase
          get() = ensureReady().let { helper.writableDatabase }

        override val readableDatabase: SupportSQLiteDatabase
          get() = ensureReady().let { helper.readableDatabase }
      }
    }
  }

  private fun canOpen(path: String, key: ByteArray): Boolean = try {
    SQLiteDatabase.openDatabase(path, key.copyOf(), null, SQLiteDatabase.OPEN_READONLY, null, null).use { db ->
      db.query("SELECT count(*) FROM sqlite_master").use { it.moveToFirst() }
    }
    true
  } catch (e: Exception) {
    false
  }
}
