package com.notdefterim.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.notdefterim.app.BuildConfig
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
  version = 10,
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

    val MIGRATION_8_9 = object : Migration(8, 9) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE passwords ADD COLUMN updatedAt INTEGER")
      }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN type TEXT NOT NULL DEFAULT 'NOTE'")
        db.execSQL("ALTER TABLE passwords ADD COLUMN categoryId INTEGER")
        
        val time = System.currentTimeMillis()
        db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Banka', '#D5E5FF', 'PASSWORD', $time)")
        db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Google', '#FFD5E5', 'PASSWORD', $time)")
        db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Resmi', '#E5FFD5', 'PASSWORD', $time)")
        db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('S. Medya', '#FFF0D5', 'PASSWORD', $time)")
        db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Aile', '#E6E6FA', 'PASSWORD', $time)")
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
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
        // Güvenli migrasyon stratejisi: yalnızca debug build'de yıkım-yeniden yapma izni
        // Release'de migration eksikse crash edilir; kullanıcı verisi sessizce SİLİNMEZ
        .apply { if (BuildConfig.DEBUG) fallbackToDestructiveMigration() }
        .addCallback(object : RoomDatabase.Callback() {
          override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val time = System.currentTimeMillis()
            // Kategoriler
            db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Alışveriş', '#FFD5E5', 'NOTE', $time)")
            db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Yapılacaklar', '#FFF0D5', 'NOTE', $time)")
            db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Fikirler', '#E5FFD5', 'NOTE', $time)")
            db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('İş', '#D5E5FF', 'NOTE', $time)")

            // Parola Kategorileri
            db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Banka', '#D5E5FF', 'PASSWORD', $time)")
            db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Google', '#FFD5E5', 'PASSWORD', $time)")
            db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Resmi', '#E5FFD5', 'PASSWORD', $time)")
            db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('S. Medya', '#FFF0D5', 'PASSWORD', $time)")
            db.execSQL("INSERT INTO categories (name, colorHex, type, createdAt) VALUES ('Aile', '#E6E6FA', 'PASSWORD', $time)")

            // Notlar
            val shoppingContent = "[{\"id\":\"1\",\"text\":\"Süt\",\"isChecked\":true,\"hasCheckbox\":true},{\"id\":\"2\",\"text\":\"Ekmek\",\"isChecked\":false,\"hasCheckbox\":true},{\"id\":\"3\",\"text\":\"Yumurta\",\"isChecked\":false,\"hasCheckbox\":true}]"
            db.execSQL("INSERT INTO notes (title, content, colorIndex, isPinned, createdAt, updatedAt, repeatInterval, viewCount, isLocked, isChecklist, categoryId) VALUES ('(Örnek) Market İhtiyaçları', '$shoppingContent', 1, 0, $time, $time, 'NONE', 0, 0, 1, 1)")
            
            val todoContent = "[{\"id\":\"4\",\"text\":\"Evi temizle\",\"isChecked\":false,\"hasCheckbox\":true},{\"id\":\"5\",\"text\":\"Arabayı yıka\",\"isChecked\":false,\"hasCheckbox\":true},{\"id\":\"6\",\"text\":\"Kitap oku\",\"isChecked\":false,\"hasCheckbox\":true}]"
            db.execSQL("INSERT INTO notes (title, content, colorIndex, isPinned, createdAt, updatedAt, repeatInterval, viewCount, isLocked, isChecklist, categoryId) VALUES ('(Örnek) Hafta Sonu Planı', '$todoContent', 4, 0, $time, $time, 'NONE', 0, 0, 1, 2)")
            
            val ideaContent = "Yapay zeka entegreli not alma uygulaması yapabilirim. Çevrimdışı ve güvenli olmalı."
            db.execSQL("INSERT INTO notes (title, content, colorIndex, isPinned, createdAt, updatedAt, repeatInterval, viewCount, isLocked, isChecklist, categoryId) VALUES ('(Örnek) Yeni Proje Fikirleri', '$ideaContent', 2, 0, $time, $time, 'NONE', 0, 0, 0, 3)")

            // Parolalar — gerçek görünümlü kimlik bilgisi kullanılmaz (KVKK / Play Store uyumu)
            db.execSQL("INSERT INTO passwords (platformName, username, passwordValue, createdAt, updatedAt, usageCount, categoryId) VALUES ('(Örnek) E-posta', 'ornek.kullanici', '***', $time, $time, 0, 6)")
            db.execSQL("INSERT INTO passwords (platformName, username, passwordValue, createdAt, updatedAt, usageCount, categoryId) VALUES ('(Örnek) Kamu Portalı', 'kullanici_adi', '***', $time, $time, 0, 7)")
          }
        })
        .build()
    }
  }
}
