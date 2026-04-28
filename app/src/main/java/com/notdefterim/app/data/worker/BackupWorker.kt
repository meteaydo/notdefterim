package com.notdefterim.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notdefterim.app.domain.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager arka plan yedekleme işçisi.
 *
 * @HiltWorker: Hilt'in WorkManager entegrasyonu ile DI sağlar.
 * CoroutineWorker: suspend fonksiyonları doğrudan kullanmamıza imkân tanır.
 *
 * Çalışma koşulları (kısıtlamalar) DatabaseModule'de tanımlanmıştır:
 * - UNMETERED network (Wi-Fi)
 * - Şarjda olma
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
  @Assisted context: Context,
  @Assisted workerParams: WorkerParameters,
  private val backupRepository: BackupRepository
) : CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result {
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
}
