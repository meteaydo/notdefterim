package com.notdefterim.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room veritabanı entity'si.
 *
 * SQLCipher tüm veritabanını AES-256 ile şifrelediğinden,
 * title ve content düz metin olarak saklanabilir.
 * Şifreleme veritabanı dosyası seviyesinde gerçekleşir.
 */
@Entity(tableName = "notes")
data class NoteEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val content: String,
  /** Unix epoch milisaniye — oluşturma zamanı */
  val createdAt: Long,
  /** Unix epoch milisaniye — son güncellenme zamanı */
  val updatedAt: Long,
  /** Notun üste sabitlenip sabitlenmediği */
  val isPinned: Boolean = false,
  /**
   * Not kartı arka plan rengi indeksi (0–7).
   * 0 = varsayılan (tema rengi), 1-7 = özel renkler.
   */
  val colorIndex: Int = 0,
  /** Hatırlatıcı zamanı — Unix epoch milisaniye */
  val reminderAt: Long? = null,
  /** Tekrarlama periyodu */
  val repeatInterval: String = "NONE",
  /** Kategori ID'si (varsa) */
  val categoryId: Long? = null,
  /** Notun görüntülenme sayısı (En Sık sekmesi için) */
  val viewCount: Int = 0,
  /** Notun kilitli olup olmadığı */
  val isLocked: Boolean = false,
  /** Notun checklist modunda olup olmadığı */
  val isChecklist: Boolean = false
)
