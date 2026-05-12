package com.notdefterim.app.core.di

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.notdefterim.app.core.security.EncryptionHelper
import com.notdefterim.app.data.local.NoteDao
import com.notdefterim.app.data.local.CategoryDao
import com.notdefterim.app.data.local.NoteDatabase
import com.notdefterim.app.data.worker.BackupWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
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
    val database = NoteDatabase.create(context, passphrase)
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

  /**
   * Periyodik yedekleme işini kaydeder.
   * - 24 saatte bir çalışır
   * - Wi-Fi bağlı ve şarjda olma koşulları
   * - enqueueUniquePeriodicWork: aynı iş birden fazla kez zamanlanmaz
   */
  @Provides
  @Singleton
  fun provideWorkManager(
    @ApplicationContext context: Context
  ): WorkManager {
    val workManager = WorkManager.getInstance(context)

    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi
      .setRequiresCharging(true)
      .build()

    val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
      .setConstraints(constraints)
      .build()

    workManager.enqueueUniquePeriodicWork(
      "notdefterim_daily_backup",
      ExistingPeriodicWorkPolicy.KEEP,
      backupRequest
    )

    return workManager
  }
}
