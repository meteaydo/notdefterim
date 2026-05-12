package com.notdefterim.app.domain.usecase.backup

import com.notdefterim.app.data.remote.BackupInfo
import com.notdefterim.app.domain.repository.BackupRepository
import javax.inject.Inject

class GetCloudBackupsUseCase @Inject constructor(
  private val repository: BackupRepository
) {
  suspend operator fun invoke(): Result<List<BackupInfo>> = repository.listBackups()
}
