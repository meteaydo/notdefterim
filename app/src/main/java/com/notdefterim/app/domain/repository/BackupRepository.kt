package com.notdefterim.app.domain.repository

/**
 * Yedekleme repository sözleşmesi.
 *
 * backupToCloud: Tüm notları Drive'a yükler.
 * restoreFromCloud: Drive'daki en son yedeği yerel DB'ye yazar.
 * Dönen Int: geri yüklenen not sayısı.
 */
import com.notdefterim.app.data.remote.BackupInfo

interface BackupRepository {
  suspend fun backupToCloud(): Result<Unit>
  suspend fun restoreFromCloud(backupId: String? = null): Result<Int>
  suspend fun listBackups(): Result<List<BackupInfo>>
}
