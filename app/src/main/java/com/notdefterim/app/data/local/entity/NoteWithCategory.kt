package com.notdefterim.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class NoteWithCategory(
  @Embedded val note: NoteEntity,
  @Relation(
    parentColumn = "categoryId",
    entityColumn = "id"
  )
  val category: CategoryEntity?
)
