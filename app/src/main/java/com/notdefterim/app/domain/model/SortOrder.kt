package com.notdefterim.app.domain.model

import com.notdefterim.app.R
import java.text.Collator

/** Not listesinin sırası. Sabitlenen notlar ve filtrenin öne aldıkları her zaman önce gelir. */
enum class NoteSortOrder(val titleResId: Int) {
  LAST_EDITED(R.string.sort_last_edited),
  OLDEST_EDITED(R.string.sort_last_changed),
  MOST_USED(R.string.sort_most_used),
  CREATED(R.string.sort_created),
  TITLE(R.string.sort_title_az);

  fun comparator(collator: Collator = Collator.getInstance()): Comparator<Note> = when (this) {
    LAST_EDITED -> compareByDescending { it.updatedAt }
    OLDEST_EDITED -> compareBy { it.updatedAt }
    // Açılma sayısı; eşitlikte son düzenlenen önce
    MOST_USED -> compareByDescending<Note> { it.viewCount }.thenByDescending { it.updatedAt }
    CREATED -> compareByDescending { it.createdAt }
    // Başlıksız notlar sonda, kendi aralarında son düzenlenen önce
    TITLE -> compareBy<Note> { it.title.isBlank() }
      .then(compareBy(collator) { it.title.trim() })
      .thenByDescending { it.updatedAt }
  }

  companion object {
    fun fromName(name: String?): NoteSortOrder = entries.firstOrNull { it.name == name } ?: LAST_EDITED
  }
}

/** Parola listesinin sırası. */
enum class PasswordSortOrder(val titleResId: Int) {
  /** Hiç değiştirilmeyenler oluşturulma zamanıyla; en yeni başta */
  RECENTLY_CHANGED(R.string.sort_last_edited),
  /** Hiç değiştirilmeyenler oluşturulma zamanıyla; en eski başta (değiştirme sırası gelenler) */
  LAST_CHANGED(R.string.sort_last_changed),
  MOST_USED(R.string.sort_most_used),
  NAME(R.string.sort_name_az);

  fun comparator(collator: Collator = Collator.getInstance()): Comparator<Password> = when (this) {
    RECENTLY_CHANGED -> compareByDescending { it.updatedAt ?: it.createdAt }
    LAST_CHANGED -> compareBy { it.updatedAt ?: it.createdAt }
    MOST_USED -> compareByDescending<Password> { it.usageCount }.thenByDescending { it.createdAt }
    NAME -> compareBy(collator) { it.platformName.trim() }
  }

  companion object {
    fun fromName(name: String?): PasswordSortOrder = entries.firstOrNull { it.name == name } ?: MOST_USED
  }
}
