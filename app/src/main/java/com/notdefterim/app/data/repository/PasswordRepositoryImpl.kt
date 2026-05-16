package com.notdefterim.app.data.repository

import com.notdefterim.app.data.local.PasswordDao
import com.notdefterim.app.data.local.entity.PasswordEntity
import com.notdefterim.app.domain.model.Password
import com.notdefterim.app.domain.repository.PasswordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import com.notdefterim.app.data.local.entity.PasswordWithCategory
import com.notdefterim.app.data.mapper.toDomain

class PasswordRepositoryImpl @Inject constructor(
  private val dao: PasswordDao
) : PasswordRepository {

  override fun getAllPasswords(): Flow<List<Password>> {
    return dao.getAllPasswords().map { entities ->
      entities.map { it.toDomain() }
    }
  }

  override fun searchPasswords(query: String): Flow<List<Password>> {
    return dao.searchPasswords(query).map { entities ->
      entities.map { it.toDomain() }
    }
  }

  override suspend fun getPasswordById(id: Long): Password? {
    return dao.getPasswordById(id)?.toDomain()
  }

  override suspend fun insertPassword(password: Password): Long {
    return dao.insertPassword(password.toEntity())
  }

  override suspend fun updatePassword(password: Password) {
    dao.updatePassword(password.toEntity())
  }

  override suspend fun deletePassword(password: Password) {
    dao.deletePassword(password.toEntity())
  }
}

fun PasswordEntity.toDomain() = Password(
  id = id,
  platformName = platformName,
  username = username,
  passwordValue = passwordValue,
  usageCount = usageCount,
  createdAt = createdAt,
  updatedAt = updatedAt
)

fun PasswordWithCategory.toDomain() = password.toDomain().copy(
  category = category?.toDomain()
)

fun Password.toEntity() = PasswordEntity(
  id = id,
  platformName = platformName,
  username = username,
  passwordValue = passwordValue,
  usageCount = usageCount,
  categoryId = category?.id,
  createdAt = createdAt,
  updatedAt = updatedAt
)
