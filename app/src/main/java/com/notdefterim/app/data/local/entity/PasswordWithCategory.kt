package com.notdefterim.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PasswordWithCategory(
  @Embedded val password: PasswordEntity,
  @Relation(
    parentColumn = "categoryId",
    entityColumn = "id"
  )
  val category: CategoryEntity?
)
