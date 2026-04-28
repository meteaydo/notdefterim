package com.notdefterim.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.notdefterim.app.data.local.entity.NoteEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * SQLCipher ile şifrelenmiş Room veritabanı.
 *
 * Neden singleton pattern?
 * Room bağlantısı oluşturmak maliyetlidir; uygulama boyunca tek
 * bir instance kullanmak hem performansı hem de tutarlılığı sağlar.
 * Hilt, bu sınıfı singleton olarak yönetir — burada ayrıca tutmuyoruz.
 */
@Database(
  entities = [NoteEntity::class],
  version = 1,
  exportSchema = true
)
abstract class NoteDatabase : RoomDatabase() {

  abstract fun noteDao(): NoteDao

  companion object {
    const val DATABASE_NAME = "notdefterim_encrypted.db"

    /**
     * SQLCipher SupportFactory ile şifrelenmiş Room veritabanı oluşturur.
     *
     * [passphrase] güvenlik katmanından gelen 256-bit anahtar.
     * Veritabanı açıldıktan sonra passphrase byte dizisi bellekten silinmelidir.
     */
    fun create(context: Context, passphrase: ByteArray): NoteDatabase {
      SQLiteDatabase.loadLibs(context)

      val factory = SupportFactory(passphrase)

      return Room.databaseBuilder(
        context.applicationContext,
        NoteDatabase::class.java,
        DATABASE_NAME
      )
        .openHelperFactory(factory)
        // Migrasyon stratejisi: şimdilik yıkım-yeniden yapım
        // Üretimde Migration sınıfları eklenmelidir
        .fallbackToDestructiveMigration()
        .build()
    }
  }
}
