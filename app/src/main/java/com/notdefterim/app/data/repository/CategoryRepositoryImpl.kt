package com.notdefterim.app.data.repository

import com.notdefterim.app.data.local.CategoryDao
import com.notdefterim.app.data.mapper.toDomain
import com.notdefterim.app.data.mapper.toEntity
import com.notdefterim.app.domain.model.Category
import com.notdefterim.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
  private val categoryDao: CategoryDao
) : CategoryRepository {
  override fun getAllCategories(): Flow<List<Category>> =
    categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }

  override fun getCategoriesByType(type: com.notdefterim.app.domain.model.CategoryType): Flow<List<Category>> =
    categoryDao.getCategoriesByType(type.name).map { list -> list.map { it.toDomain() } }

  override suspend fun addCategory(category: Category): Long =
    categoryDao.insertCategory(category.toEntity())

  override suspend fun updateCategory(category: Category) =
    categoryDao.updateCategory(category.toEntity())

  override suspend fun deleteCategory(id: Long) =
    categoryDao.deleteCategory(id)
}
