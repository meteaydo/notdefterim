package com.notdefterim.app.core.di

import android.content.Context
import com.notdefterim.app.core.security.EncryptionHelper
import com.notdefterim.app.data.local.NoteDao
import com.notdefterim.app.data.local.CategoryDao
import com.notdefterim.app.data.local.DatabaseKeyMigration
import com.notdefterim.app.data.local.NoteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideEncryptionHelper(
    @ApplicationContext context: Context
  ): EncryptionHelper = EncryptionHelper(context)

  @Provides
  @Singleton
  fun provideNoteDatabase(
    @ApplicationContext context: Context,
    encryptionHelper: EncryptionHelper
  ): NoteDatabase {
    // Passphrase oluştur, DB'yi aç, sonra passphrase'i bellekten temizle
    val passphrase = encryptionHelper.getDatabasePassphrase()
    // Güncellemeden ya da yedek geri yüklemeden sonra bir kez: sıfır anahtarlı veritabanını taşı.
    // Room'un ilk açılışında arka planda çalışır.
    val pendingKeyCheck = if (encryptionHelper.isDatabaseKeyVerified()) null else passphrase.copyOf()
    val database = NoteDatabase.create(context, passphrase) {
      pendingKeyCheck?.let { key ->
        if (DatabaseKeyMigration.migrateIfNeeded(context, key)) {
          encryptionHelper.markDatabaseKeyVerified()
        }
        key.fill(0)
      }
    }
    encryptionHelper.wipePassphraseFromMemory(passphrase)
    return database
  }

  @Provides
  @Singleton
  fun provideNoteDao(database: NoteDatabase): NoteDao = database.noteDao()

  @Provides
  @Singleton
  fun providePasswordDao(database: NoteDatabase) = database.passwordDao()

  @Provides
  @Singleton
  fun provideCategoryDao(database: NoteDatabase): CategoryDao = database.categoryDao()
}
