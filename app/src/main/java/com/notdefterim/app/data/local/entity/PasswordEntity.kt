package com.notdefterim.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val platformName: String,
  val username: String,
  val passwordValue: String,
  val usageCount: Int = 0,
  val createdAt: Long
)
