package com.notdefterim.app.domain.model

/**
 * PIN'in hangi içerikleri koruduğunu temsil eder.
 *
 * Neden enum?
 * Magic int (0/1/2) yerine enum kullanmak; refactoring sırasında
 * derleyici hata yakalamasını sağlar ve anlamı açık eder.
 *
 * Int karşılıkları: SharedPreferences geriye dönük uyumluluğu için korunur.
 */
enum class PinScope(val value: Int) {
  /** PIN hem notları hem parolaları korur. */
  ALL(0),

  /** PIN yalnızca notları korur. */
  NOTES_ONLY(1),

  /** PIN yalnızca parolaları korur. */
  PASSWORDS_ONLY(2);

  companion object {
    fun fromValue(value: Int): PinScope =
      entries.find { it.value == value } ?: ALL
  }
}

/**
 * Uygulama açılışında hangi sekmenin gösterileceğini temsil eder.
 *
 * Int karşılıkları: SharedPreferences geriye dönük uyumluluğu için korunur.
 */
enum class StartupBehavior(val value: Int) {
  /** Son açık sekme açılır. */
  LAST_OPENED(0),

  /** Her zaman Notlar sekmesi açılır. */
  NOTES(1),

  /** Her zaman Parolalar sekmesi açılır. */
  PASSWORDS(2);

  companion object {
    fun fromValue(value: Int): StartupBehavior =
      entries.find { it.value == value } ?: LAST_OPENED
  }
}
