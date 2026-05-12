package com.notdefterim.app.domain.usecase.category

import com.notdefterim.app.domain.model.Category
import com.notdefterim.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
  private val repository: CategoryRepository
) {
  operator fun invoke(): Flow<List<Category>> = repository.getAllCategories()
}
