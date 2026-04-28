package com.notdefterim.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Uygulama giriş noktası.
 *
 * @HiltAndroidApp: Hilt bağımlılık enjeksiyonunu başlatır.
 * Configuration.Provider: HiltWorkerFactory'yi WorkManager'a entegre eder;
 * bu olmadan @HiltWorker ile işaretlenmiş worker'lar DI alamaz.
 */
@HiltAndroidApp
class NotDefterimApp : Application(), Configuration.Provider {

  @Inject
  lateinit var workerFactory: HiltWorkerFactory

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder()
      .setWorkerFactory(workerFactory)
      .build()
}
