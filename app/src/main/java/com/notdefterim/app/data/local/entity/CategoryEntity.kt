package com.notdefterim.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val colorHex: String,
  val type: String = "NOTE",
  val createdAt: Long = System.currentTimeMillis()
)
