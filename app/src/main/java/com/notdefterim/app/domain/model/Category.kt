package com.notdefterim.app.domain.model

data class Category(
  val id: Long = 0,
  val name: String,
  val colorHex: String,
  val type: CategoryType = CategoryType.NOTE
)

enum class CategoryType {
    NOTE,
    PASSWORD
}
