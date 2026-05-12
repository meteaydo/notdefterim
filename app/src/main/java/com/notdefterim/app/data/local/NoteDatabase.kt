package com.notdefterim.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.notdefterim.app.data.local.entity.CategoryEntity
import com.notdefterim.app.data.local.entity.NoteEntity
import com.notdefterim.app.data.local.entity.PasswordEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * SQLCipher ile şifrelenmiş Room veritabanı.
 *
 * Neden singleton pattern?
 * Room bağlantısı oluşturmak maliyetlidir; uygulama boyunca tek
 * bir instance kullanmak hem performansı hem de tutarlılığı sağlar.
 * Hilt, bu sınıfı singleton olarak yönetir — burada ayrıca tutmuyoruz.
 */
@Database(
  entities = [NoteEntity::class, PasswordEntity::class, CategoryEntity::class],
  version = 8,
  exportSchema = true
)
abstract class NoteDatabase : RoomDatabase() {

  abstract fun noteDao(): NoteDao
  abstract fun passwordDao(): PasswordDao
  abstract fun categoryDao(): CategoryDao

  companion object {
    const val DATABASE_NAME = "notdefterim_encrypted.db"

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE passwords ADD COLUMN usageCount INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN reminderAt INTEGER")
      }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN repeatInterval TEXT NOT NULL DEFAULT 'NONE'")
      }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN categoryId INTEGER")
        db.execSQL("ALTER TABLE notes ADD COLUMN viewCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        
        // Pre-populate varsayılan kategoriler
        val time = System.currentTimeMillis()
        db.execSQL("INSERT INTO categories (name, colorHex, createdAt) VALUES ('Alışveriş', '#FFD5E5', $time)")
        db.execSQL("INSERT INTO categories (name, colorHex, createdAt) VALUES ('Yapılacaklar', '#FFF0D5', $time)")
        db.execSQL("INSERT INTO categories (name, colorHex, createdAt) VALUES ('Fikirler', '#E5FFD5', $time)")
        db.execSQL("INSERT INTO categories (name, colorHex, createdAt) VALUES ('İş', '#D5E5FF', $time)")
      }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN isChecklist INTEGER NOT NULL DEFAULT 0")
      }
    }

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
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
        // Migrasyon stratejisi: şimdilik yıkım-yeniden yapım
        // Üretimde Migration sınıfları eklenmelidir
        .fallbackToDestructiveMigration()
        .build()
    }
  }
}
