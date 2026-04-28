package com.notdefterim.app.core.di

import android.content.Context
import com.notdefterim.app.data.remote.DriveServiceHelper
import com.notdefterim.app.data.remote.GoogleAuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DriveModule {

  @Provides
  @Singleton
  fun provideGoogleAuthManager(
    @ApplicationContext context: Context
  ): GoogleAuthManager = GoogleAuthManager(context)

  @Provides
  @Singleton
  fun provideDriveServiceHelper(
    @ApplicationContext context: Context
  ): DriveServiceHelper = DriveServiceHelper(context)
}
