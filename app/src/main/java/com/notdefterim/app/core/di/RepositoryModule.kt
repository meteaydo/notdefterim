package com.notdefterim.app.core.di

import com.notdefterim.app.data.repository.GoogleDriveBackupRepositoryImpl
import com.notdefterim.app.data.repository.NoteRepositoryImpl
import com.notdefterim.app.domain.repository.BackupRepository
import com.notdefterim.app.domain.repository.NoteRepository
import com.notdefterim.app.domain.repository.PasswordRepository
import com.notdefterim.app.data.repository.PasswordRepositoryImpl
import com.notdefterim.app.domain.repository.CategoryRepository
import com.notdefterim.app.data.repository.CategoryRepositoryImpl
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
  abstract fun bindBackupRepository(impl: GoogleDriveBackupRepositoryImpl): BackupRepository

  @Binds
  @Singleton
  abstract fun bindPasswordRepository(impl: PasswordRepositoryImpl): PasswordRepository

  @Binds
  @Singleton
  abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
}
