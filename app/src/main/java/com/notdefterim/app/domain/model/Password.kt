package com.notdefterim.app.domain.model

data class Password(
  val id: Long = 0,
  val platformName: String,
  val username: String,
  val passwordValue: String,
  val usageCount: Int = 0,
  val createdAt: Long = System.currentTimeMillis()
)
