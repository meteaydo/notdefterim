package com.notdefterim.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.notdefterim.app.domain.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
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
  }

  fun scheduleReminder(note: Note) {
    if (note.reminderAt == null) return

    val timeInMillis = note.reminderAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    if (timeInMillis <= System.currentTimeMillis()) {
      return
    }

    val intent = Intent(context, ReminderReceiver::class.java).apply {
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

  fun cancelReminder(noteId: Long) {
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      noteId.toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
  }
}
