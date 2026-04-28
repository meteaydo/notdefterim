package com.notdefterim.app.core.di

import android.content.Context
import com.notdefterim.app.core.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

  @Provides
  @Singleton
  fun provideSecurityManager(
    @ApplicationContext context: Context
  ): SecurityManager = SecurityManager(context)
}
