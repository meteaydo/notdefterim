package com.notdefterim.app.domain.repository

import com.notdefterim.app.domain.model.Password
import kotlinx.coroutines.flow.Flow

interface PasswordRepository {
  fun getAllPasswords(): Flow<List<Password>>
  fun searchPasswords(query: String): Flow<List<Password>>
  suspend fun getPasswordById(id: Long): Password?
  suspend fun insertPassword(password: Password): Long
  suspend fun updatePassword(password: Password)
  suspend fun deletePassword(password: Password)
}
