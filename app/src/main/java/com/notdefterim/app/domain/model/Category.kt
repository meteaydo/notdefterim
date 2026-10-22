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

/**
 * İlk açılışta oluşturulan not kategorileri. Simge ve düzenleme koruması kategori adıyla eşleşir;
 * kategori kurulumdaki dile göre Türkçe ya da İngilizce adla oluşur.
 */
enum class DefaultNoteCategory(private vararg val names: String) {
  SHOPPING("Alışveriş", "Shopping"),
  TODO("Yapılacaklar", "To-Do"),
  IDEAS("Fikirler", "Ideas");

  companion object {
    fun of(name: String): DefaultNoteCategory? =
      entries.firstOrNull { category -> category.names.any { it.equals(name, ignoreCase = true) } }
  }
}

/** İlk açılışta oluşturulan parola kategorileri; simge kategori adıyla eşleşir (Türkçe ya da İngilizce). */
enum class DefaultPasswordCategory(private vararg val names: String) {
  BANK("Banka", "Bank"),
  GOOGLE("Google"),
  SOCIAL_MEDIA("S. Medya", "Social Media"),
  GOVERNMENT("Resmi", "Government"),
  FAMILY("Aile", "Family");

  companion object {
    fun of(name: String): DefaultPasswordCategory? =
      entries.firstOrNull { category -> category.names.any { it.equals(name, ignoreCase = true) } }
  }
}
