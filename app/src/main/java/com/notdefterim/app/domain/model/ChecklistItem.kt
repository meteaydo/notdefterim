package com.notdefterim.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var isChecked: Boolean = false,
    var hasCheckbox: Boolean = true
)
