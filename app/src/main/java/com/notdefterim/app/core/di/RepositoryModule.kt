package com.notdefterim.app.core.di

import com.notdefterim.app.data.repository.BackupRepositoryImpl
import com.notdefterim.app.data.repository.NoteRepositoryImpl
import com.notdefterim.app.domain.repository.BackupRepository
import com.notdefterim.app.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds: interface → implementasyon bağlantısı.
 * Provides yerine Binds kullanmak daha verimlidir (proxy sınıfı oluşturmaz).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

  @Binds
  @Singleton
  abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

  @Binds
  @Singleton
  abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
