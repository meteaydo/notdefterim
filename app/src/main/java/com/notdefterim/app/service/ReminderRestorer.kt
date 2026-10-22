package com.notdefterim.app.service

import android.content.Context
import android.util.Log
import com.notdefterim.app.domain.model.Note
import com.notdefterim.app.domain.model.RepeatInterval
import com.notdefterim.app.domain.repository.NoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hatırlatıcıları veritabanından yeniden kurar; telefon yeniden başlayınca ve uygulama açılınca çalışır.
 * Yeniden başlatma ve zorla durdurma alarmları siler. Xiaomi gibi cihazlarda otomatik başlatma izni
 * yoksa yeniden başlatma bildirimi hiç gelmez; bu yüzden açılışta da çalışır.
 *
 * - Gelecektekiler yeniden zamanlanır.
 * - Vakti geçen tekrarlayanlar bir sonraki zamana ilerletilir; yoksa kalıcı olarak dururlar.
 * - Son kontrolden beri vakti gelip gösterilmemiş hatırlatıcılar (telefon kapalıyken) hemen bildirilir.
 */
@Singleton
class ReminderRestorer @Inject constructor(
  @ApplicationContext private val context: Context,
  private val repository: NoteRepository,
  private val reminderManager: ReminderManager
) {

  private val prefs = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
  private val mutex = Mutex()

  companion object {
    private const val TAG = "ReminderRestorer"
    private const val KEY_LAST_CHECK = "last_check_at"
    private const val KEY_DELIVERED = "delivered"
    /** Gösterilen hatırlatıcı kayıtları bu kadar eskiyince silinir */
    private const val DELIVERED_KEEP_MS = 24 * 60 * 60 * 1000L
  }

  suspend fun restore() {
    mutex.withLock {
      try {
        val now = System.currentTimeMillis()
        // İlk çalıştırmada eski hatırlatıcılar kaçırılmış sayılmasın
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, now)

        for (note in repository.getAllNotesSnapshot()) {
          val reminderAtMillis = note.reminderAt?.toEpochMillis() ?: continue

          if (reminderAtMillis > now) {
            reminderManager.scheduleReminder(note)
            continue
          }

          if (reminderAtMillis > lastCheck && !isDelivered(note.id, reminderAtMillis)) {
            ReminderReceiver.showReminder(
              context = context,
              noteId = note.id,
              title = note.title,
              rawContent = note.content,
              isChecklist = note.isChecklist,
              isLocked = note.isLocked,
              whenMillis = reminderAtMillis
            )
            markDelivered(note.id, reminderAtMillis)
            // Gecikmeli gelebilecek eski alarm aynı bildirimi tekrar göstermesin
            reminderManager.cancelReminder(note.id)
          }

          advance(note, now)
        }

        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        pruneDelivered(now - DELIVERED_KEEP_MS)
      } catch (e: Exception) {
        Log.e(TAG, "Hatırlatıcılar yeniden kurulamadı", e)
      }
    }
  }

  /** Tekrarlayan hatırlatıcı çaldıktan sonra bir sonraki zamanı kurar. */
  suspend fun advanceRepeating(noteId: Long) {
    mutex.withLock {
      val note = repository.getNoteById(noteId) ?: return@withLock
      advance(note, System.currentTimeMillis())
    }
  }

  /**
   * Vakti geçmiş tekrarlayan hatırlatıcıyı şimdiden sonraki ilk tekrara ilerletir.
   * Telefon kapalıyken birkaç tekrar kaçtıysa hepsi atlanır; zaten ilerletildiyse dokunmaz.
   */
  private suspend fun advance(note: Note, now: Long) {
    var next = note.reminderAt ?: return
    if (note.repeatInterval == RepeatInterval.NONE || next.toEpochMillis() > now) return

    while (next.toEpochMillis() <= now) {
      next = when (note.repeatInterval) {
        RepeatInterval.DAILY -> next.plusDays(1)
        RepeatInterval.WEEKLY -> next.plusWeeks(1)
        RepeatInterval.MONTHLY -> next.plusMonths(1)
        RepeatInterval.YEARLY -> next.plusYears(1)
        RepeatInterval.NONE -> return
      }
    }

    val updatedNote = note.copy(reminderAt = next)
    repository.updateNote(updatedNote)
    reminderManager.scheduleReminder(updatedNote)
  }

  /** Bu hatırlatıcının bildirimi gösterildi mi? Alarm ile açılıştaki kontrol aynı bildirimi iki kez göstermesin. */
  fun isDelivered(noteId: Long, reminderAtMillis: Long): Boolean = synchronized(this) {
    key(noteId, reminderAtMillis) in deliveredKeys()
  }

  fun markDelivered(noteId: Long, reminderAtMillis: Long) {
    synchronized(this) {
      prefs.edit().putStringSet(KEY_DELIVERED, deliveredKeys() + key(noteId, reminderAtMillis)).apply()
    }
  }

  private fun pruneDelivered(olderThan: Long) {
    synchronized(this) {
      val kept = deliveredKeys().filterTo(HashSet()) { (it.substringAfter('_').toLongOrNull() ?: 0L) >= olderThan }
      prefs.edit().putStringSet(KEY_DELIVERED, kept).apply()
    }
  }

  private fun deliveredKeys(): Set<String> = prefs.getStringSet(KEY_DELIVERED, null)?.toSet() ?: emptySet()

  private fun key(noteId: Long, reminderAtMillis: Long) = "${noteId}_$reminderAtMillis"

  private fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
