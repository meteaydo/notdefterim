package com.notdefterim.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.notdefterim.app.data.remote.GoogleAuthManager
import com.notdefterim.app.domain.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager arka plan yedekleme işçisi.
 *
 * @HiltWorker: Hilt'in WorkManager entegrasyonu ile DI sağlar.
 * CoroutineWorker: suspend fonksiyonları doğrudan kullanmamıza imkân tanır.
 *
 * Çalışma koşulları (kısıtlamalar) [schedule] içinde tanımlanmıştır:
 * - UNMETERED network (Wi-Fi)
 * - Şarjda olma
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
  @Assisted context: Context,
  @Assisted workerParams: WorkerParameters,
  private val backupRepository: BackupRepository,
  private val authManager: GoogleAuthManager
) : CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result {
    // Google hesabıyla giriş yapılmamışsa yedeklenecek yer yok; yeniden denemeden ertesi güne bırak
    if (authManager.getCurrentAccount() == null) return Result.success()

    return try {
      backupRepository.backupToCloud().fold(
        onSuccess = { Result.success() },
        onFailure = { throwable ->
          android.util.Log.e(
            "BackupWorker",
            "Yedekleme başarısız: ${throwable.message}",
            throwable
          )
          // Geçici hata: yeniden dene (maksimum 3 kez)
          if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
      )
    } catch (e: Exception) {
      android.util.Log.e("BackupWorker", "Beklenmeyen hata: ${e.message}", e)
      Result.failure()
    }
  }

  companion object {
    private const val WORK_NAME = "notdefterim_daily_backup"

    /**
     * Periyodik yedekleme işini kaydeder; uygulama her açıldığında çağrılır.
     * - 24 saatte bir çalışır
     * - Wi-Fi bağlı ve şarjda olma koşulları
     * - enqueueUniquePeriodicWork: aynı iş birden fazla kez zamanlanmaz;
     *   UPDATE, koşullar değişirse mevcut işi takvimini bozmadan günceller
     */
    fun schedule(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi
        .setRequiresCharging(true)
        .build()

      val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
        .setConstraints(constraints)
        .build()

      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        backupRequest
      )
    }
  }
}
