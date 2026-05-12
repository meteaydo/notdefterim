package com.notdefterim.app.domain.usecase.category

import com.notdefterim.app.domain.model.Category
import com.notdefterim.app.domain.repository.CategoryRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
  private val repository: CategoryRepository
) {
  suspend operator fun invoke(category: Category): Long = repository.addCategory(category)
}
