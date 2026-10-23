package com.notdefterim.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.notdefterim.app.domain.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
  @ApplicationContext private val context: Context
) {

  private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  companion object {
    private const val TAG = "ReminderManager"

    /** Bildirimdeki "Ertele" düğmesi; kaç dakika olduğu [EXTRA_SNOOZE_MINUTES]'ta */
    const val ACTION_SNOOZE = "com.notdefterim.app.action.REMINDER_SNOOZE"
    /** Bildirimdeki "Tamam" düğmesi */
    const val ACTION_DONE = "com.notdefterim.app.action.REMINDER_DONE"
    /** Tekrarlayan hatırlatıcının ertelenmiş alarmı. Asıl alarmın yerine geçmesin diye ayrı bir eylemle kurulur. */
    const val ACTION_SNOOZED_ALARM = "com.notdefterim.app.action.REMINDER_SNOOZED_ALARM"
    const val EXTRA_SNOOZE_MINUTES = "SNOOZE_MINUTES"
  }

  fun scheduleReminder(note: Note) {
    val reminderAt = note.reminderAt ?: return
    // Asıl alarmın intent'i eylemsizdir; önceki sürümlerin kurduğu alarmlarla aynı kalsın ki iptal edilebilsinler
    schedule(note, reminderAt, action = null)
  }

  /**
   * Tekrarlayan hatırlatıcıyı [at] zamanına erteler; asıl tekrar zamanı değişmez.
   * Telefon yeniden başlarsa ertelenmiş alarm kaybolur, bir sonraki tekrar yine gelir.
   */
  fun scheduleSnooze(note: Note, at: LocalDateTime) {
    schedule(note, at, action = ACTION_SNOOZED_ALARM)
  }

  private fun schedule(note: Note, at: LocalDateTime, action: String?) {
    val timeInMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    if (timeInMillis <= System.currentTimeMillis()) {
      return
    }

    val intent = Intent(context, ReminderReceiver::class.java).apply {
      this.action = action
      putExtra("NOTE_ID", note.id)
      putExtra("NOTE_TITLE", note.title)
      // Kilitli notun içeriği alarm kaydına ve bildirime konmaz
      putExtra("NOTE_CONTENT", if (note.isLocked) "" else note.content)
      putExtra("IS_CHECKLIST", note.isChecklist)
      putExtra("IS_LOCKED", note.isLocked)
      // Alarm ile açılıştaki kontrol aynı hatırlatıcıyı iki kez göstermesin (bkz. ReminderRestorer)
      putExtra("REMINDER_AT", timeInMillis)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      note.id.toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
          alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } else {
          alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
      } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
      }
    } catch (e: SecurityException) {
      android.util.Log.e(TAG, "Exact alarm için izin yok, normal alarm deneniyor", e)
      try {
          alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
      } catch (ex: Exception) {
          android.util.Log.e(TAG, "Alarm ayarlanamadı (fallback da başarısız)", ex)
      }
    }
  }

  /** Notun asıl alarmını da ertelenmiş alarmını da iptal eder. */
  fun cancelReminder(noteId: Long) {
    cancel(noteId, action = null)
    cancelSnooze(noteId)
  }

  fun cancelSnooze(noteId: Long) {
    cancel(noteId, action = ACTION_SNOOZED_ALARM)
  }

  private fun cancel(noteId: Long, action: String?) {
    val intent = Intent(context, ReminderReceiver::class.java).apply { this.action = action }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      noteId.toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
  }
}
