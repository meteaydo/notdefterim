package com.notdefterim.app.domain.usecase.backup

import com.notdefterim.app.domain.repository.BackupRepository
import javax.inject.Inject

/** Drive'daki en son yedeği yerel DB'ye geri yükler. */
class RestoreFromCloudUseCase @Inject constructor(
  private val repository: BackupRepository
) {
  /** @return geri yüklenen not sayısı */
  suspend operator fun invoke(): Result<Int> = repository.restoreFromCloud()
}
