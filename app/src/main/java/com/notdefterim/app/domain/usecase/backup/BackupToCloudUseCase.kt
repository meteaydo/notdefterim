package com.notdefterim.app.domain.usecase.backup

import com.notdefterim.app.domain.repository.BackupRepository
import javax.inject.Inject

/** Manuel veya WorkManager tetiklemeli yedekleme. */
class BackupToCloudUseCase @Inject constructor(
  private val repository: BackupRepository
) {
  suspend operator fun invoke(): Result<Unit> = repository.backupToCloud()
}
