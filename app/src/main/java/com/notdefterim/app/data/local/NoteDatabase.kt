package com.notdefterim.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.notdefterim.app.BuildConfig
import com.notdefterim.app.R
import com.notdefterim.app.domain.model.ChecklistItem
import com.notdefterim.app.data.local.entity.CategoryEntity
import com.notdefterim.app.data.local.entity.NoteEntity
import com.notdefterim.app.data.local.entity.PasswordEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
  version = 11,
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

    val MIGRATION_10_11 = object : Migration(10, 11) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN repeatDays INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE notes ADD COLUMN repeatDayOfMonth INTEGER NOT NULL DEFAULT 0")
      }
    }

    /**
     * SQLCipher ile şifrelenmiş Room veritabanı oluşturur.
     *
     * [passphrase] güvenlik katmanından gelen 256-bit anahtar. Room veritabanını ilk sorguda
     * açtığı için fabrikaya bir kopya verilir; çağıran kendi dizisini hemen silebilir.
     * (1.0.0'da dizinin kendisi verilip hemen silindiği için veritabanları sıfır anahtarla
     * oluşmuştu, bkz. [DatabaseKeyMigration].)
     *
     * [beforeFirstOpen] Room veritabanını ilk kez açmadan hemen önce, arka plan iş parçacığında
     * çalışır.
     */
    fun create(context: Context, passphrase: ByteArray, beforeFirstOpen: () -> Unit = {}): NoteDatabase {
      System.loadLibrary("sqlcipher")

      val factory = DatabaseKeyMigration.openHelperFactory(
        SupportOpenHelperFactory(passphrase.copyOf()),
        beforeFirstOpen
      )

      return Room.databaseBuilder(
        context.applicationContext,
        NoteDatabase::class.java,
        DATABASE_NAME
      )
        .openHelperFactory(factory)
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
        // Güvenli migrasyon stratejisi: yalnızca debug build'de yıkım-yeniden yapma izni
        // Release'de migration eksikse crash edilir; kullanıcı verisi sessizce SİLİNMEZ
        .apply { if (BuildConfig.DEBUG) fallbackToDestructiveMigration() }
        .addCallback(object : RoomDatabase.Callback() {
          override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            insertSampleData(context, db)
          }
        })
        .build()
    }

    /**
     * İlk açılıştaki kategoriler, örnek notlar ve örnek parolalar. Metinler cihazın dilinde
     * (Türkçe ya da İngilizce) oluşur; varsayılan kategori simgeleri iki dildeki adı da tanır.
     */
    private fun insertSampleData(context: Context, db: SupportSQLiteDatabase) {
      val time = System.currentTimeMillis()

      fun category(nameRes: Int, colorHex: String, type: String) = db.execSQL(
        "INSERT INTO categories (name, colorHex, type, createdAt) VALUES (?, ?, ?, ?)",
        arrayOf(context.getString(nameRes), colorHex, type, time)
      )

      fun note(titleRes: Int, content: String, colorIndex: Int, isChecklist: Boolean, categoryId: Int) = db.execSQL(
        "INSERT INTO notes (title, content, colorIndex, isPinned, createdAt, updatedAt, repeatInterval, viewCount, isLocked, isChecklist, categoryId) " +
          "VALUES (?, ?, ?, 0, ?, ?, 'NONE', 0, 0, ?, ?)",
        arrayOf(context.getString(titleRes), content, colorIndex, time, time, if (isChecklist) 1 else 0, categoryId)
      )

      // Parolalar — gerçek görünümlü kimlik bilgisi kullanılmaz (KVKK / Play Store uyumu)
      fun password(nameRes: Int, usernameRes: Int, categoryId: Int) = db.execSQL(
        "INSERT INTO passwords (platformName, username, passwordValue, createdAt, updatedAt, usageCount, categoryId) " +
          "VALUES (?, ?, '***', ?, ?, 0, ?)",
        arrayOf(context.getString(nameRes), context.getString(usernameRes), time, time, categoryId)
      )

      // Kategoriler
      category(R.string.default_category_shopping, "#FFD5E5", "NOTE")
      category(R.string.default_category_todo, "#FFF0D5", "NOTE")
      category(R.string.default_category_ideas, "#E5FFD5", "NOTE")
      category(R.string.default_category_work, "#D5E5FF", "NOTE")

      // Parola Kategorileri
      category(R.string.default_category_bank, "#D5E5FF", "PASSWORD")
      category(R.string.default_category_google, "#FFD5E5", "PASSWORD")
      category(R.string.default_category_government, "#E5FFD5", "PASSWORD")
      category(R.string.default_category_social_media, "#FFF0D5", "PASSWORD")
      category(R.string.default_category_family, "#E6E6FA", "PASSWORD")

      // Notlar
      val shoppingContent = Json.encodeToString(
        listOf(
          ChecklistItem(id = "1", text = context.getString(R.string.sample_item_milk), isChecked = true),
          ChecklistItem(id = "2", text = context.getString(R.string.sample_item_bread)),
          ChecklistItem(id = "3", text = context.getString(R.string.sample_item_eggs))
        )
      )
      note(R.string.sample_note_shopping_title, shoppingContent, colorIndex = 1, isChecklist = true, categoryId = 1)

      val todoContent = Json.encodeToString(
        listOf(
          ChecklistItem(id = "4", text = context.getString(R.string.sample_item_clean_house)),
          ChecklistItem(id = "5", text = context.getString(R.string.sample_item_wash_car)),
          ChecklistItem(id = "6", text = context.getString(R.string.sample_item_read_book))
        )
      )
      note(R.string.sample_note_weekend_title, todoContent, colorIndex = 4, isChecklist = true, categoryId = 2)

      note(R.string.sample_note_ideas_title, context.getString(R.string.sample_note_ideas_content), colorIndex = 2, isChecklist = false, categoryId = 3)

      password(R.string.sample_password_email, R.string.sample_username_email, categoryId = 6)
      password(R.string.sample_password_government, R.string.sample_username_government, categoryId = 7)
    }
  }
}
