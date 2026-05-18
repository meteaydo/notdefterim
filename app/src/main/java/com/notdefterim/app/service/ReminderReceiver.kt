package com.notdefterim.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.notdefterim.app.MainActivity
import com.notdefterim.app.R
import com.notdefterim.app.domain.model.RepeatInterval
import com.notdefterim.app.domain.repository.NoteRepository
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

  @Inject lateinit var repository: NoteRepository
  @Inject lateinit var reminderManager: ReminderManager

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
      // Boot tamamlandığında, aslında veritabanındaki kayıtlı alarmları yeniden kurmak gerekir.
      // Ancak şu anki konseptte temel hatırlatıcı akışı hedeflendiği için atlanmıştır.
      return
    }

    val noteId = intent.getLongExtra("NOTE_ID", -1)
    val noteTitle = intent.getStringExtra("NOTE_TITLE") ?: context.getString(R.string.app_name)
    val rawContent = intent.getStringExtra("NOTE_CONTENT") ?: ""
    val isChecklist = intent.getBooleanExtra("IS_CHECKLIST", false)
    
    var displayContent = rawContent
    if (isChecklist || (rawContent.trim().startsWith("[") && rawContent.contains("\"text\""))) {
      try {
        val items = kotlinx.serialization.json.Json.decodeFromString<List<com.notdefterim.app.domain.model.ChecklistItem>>(rawContent)
        val checkedCount = items.count { it.isChecked }
        val summary = "[$checkedCount/${items.size} tamamlandı]"
        val previewLines = items.joinToString("\n") { (if (it.isChecked) "☑ " else "☐ ") + it.text }
        displayContent = "$summary\n$previewLines"
      } catch (e: Exception) {
        // JSON parse ba\u015far\u0131s\u0131z \u2014 ham i\u00e7erik bildirimde g\u00f6sterilecek
        android.util.Log.w("ReminderReceiver", "Checklist parse hatas\u0131, ham i\u00e7erik kullan\u0131l\u0131yor", e)
      }
    }

    showNotification(context, noteId, noteTitle, displayContent)

    if (noteId > 0) {
      val pendingResult = goAsync()
      // ProcessLifecycleOwner: uygulama process'ı boyunca yaşayan lifecycle-aware scope.
      // Bağımsız CoroutineScope(Dispatchers.IO) yerine kullanılır; orphan scope bırakmız.
      ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
        try {
          val note = repository.getNoteById(noteId)
          if (note != null && note.repeatInterval != RepeatInterval.NONE) {
            val nextReminderTime = when (note.repeatInterval) {
              RepeatInterval.DAILY -> note.reminderAt?.plusDays(1)
              RepeatInterval.WEEKLY -> note.reminderAt?.plusWeeks(1)
              RepeatInterval.MONTHLY -> note.reminderAt?.plusMonths(1)
              RepeatInterval.YEARLY -> note.reminderAt?.plusYears(1)
              else -> note.reminderAt
            }
            val updatedNote = note.copy(reminderAt = nextReminderTime)
            repository.updateNote(updatedNote)
            reminderManager.scheduleReminder(updatedNote)
          }
        } finally {
          pendingResult.finish()
        }
      }
    }
  }

  private fun showNotification(context: Context, noteId: Long, title: String, content: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "notdefterim_reminders"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Not Hatırlatıcıları",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Notlarınız için belirlenen hatırlatıcı bildirimleri"
      }
      notificationManager.createNotificationChannel(channel)
    }

    val activityIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      putExtra("NOTE_ID", noteId)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      noteId.toInt(),
      activityIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_stat_notification_icon)
      .setContentTitle(title.ifBlank { context.getString(R.string.reminder_title) })
      .setContentText(content)
      .setStyle(NotificationCompat.BigTextStyle().bigText(content))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    notificationManager.notify(noteId.toInt(), notification)
  }
}
