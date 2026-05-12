package com.notdefterim.app.domain.repository

import com.notdefterim.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
  fun getAllCategories(): Flow<List<Category>>
  suspend fun addCategory(category: Category): Long
  suspend fun updateCategory(category: Category)
  suspend fun deleteCategory(id: Long)
}
