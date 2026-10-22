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
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

  // Lazy: veritabanı (anahtar okuma dahil) ana iş parçacığında değil, aşağıdaki IO işinde hazırlansın.
  // Açılıştan hemen sonra yavaş cihazlarda yayın zaman aşımına (ANR) uğramasın.
  @Inject lateinit var reminderRestorerLazy: dagger.Lazy<ReminderRestorer>

  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    // ProcessLifecycleOwner: uygulama process'ı boyunca yaşayan lifecycle-aware scope.
    // Bağımsız CoroutineScope(Dispatchers.IO) yerine kullanılır; orphan scope bırakmız.
    ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
      try {
        val reminderRestorer = reminderRestorerLazy.get()
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
          // Yeniden başlatma tüm alarmları siler; hatırlatıcıları veritabanından yeniden kur
          reminderRestorer.restore()
        } else {
          onReminder(context, intent, reminderRestorer)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  private suspend fun onReminder(context: Context, intent: Intent, reminderRestorer: ReminderRestorer) {
    val noteId = intent.getLongExtra("NOTE_ID", -1)
    // Bu sürümden önce kurulan alarmlarda yok (-1)
    val reminderAtMillis = intent.getLongExtra("REMINDER_AT", -1)

    // Açılıştaki kontrol bu hatırlatıcıyı kaçırılmış sayıp zaten gösterdiyse ikinci kez gösterme
    if (reminderAtMillis <= 0 || !reminderRestorer.isDelivered(noteId, reminderAtMillis)) {
      showReminder(
        context = context,
        noteId = noteId,
        title = intent.getStringExtra("NOTE_TITLE") ?: context.getString(R.string.app_name),
        rawContent = intent.getStringExtra("NOTE_CONTENT") ?: "",
        isChecklist = intent.getBooleanExtra("IS_CHECKLIST", false),
        isLocked = intent.getBooleanExtra("IS_LOCKED", false)
      )
      if (reminderAtMillis > 0) reminderRestorer.markDelivered(noteId, reminderAtMillis)
    }

    if (noteId > 0) reminderRestorer.advanceRepeating(noteId)
  }

  companion object {

    /**
     * Hatırlatıcı bildirimini gösterir.
     * [whenMillis] verilirse bildirimde hatırlatıcının asıl zamanı görünür (kaçırılan hatırlatıcılar için).
     */
    fun showReminder(
      context: Context,
      noteId: Long,
      title: String,
      rawContent: String,
      isChecklist: Boolean,
      isLocked: Boolean,
      whenMillis: Long? = null
    ) {
      var displayContent = rawContent
      if (isLocked) {
        displayContent = context.getString(R.string.reminder_locked_note)
      } else if (isChecklist || (rawContent.trim().startsWith("[") && rawContent.contains("\"text\""))) {
        try {
          val items = kotlinx.serialization.json.Json.decodeFromString<List<com.notdefterim.app.domain.model.ChecklistItem>>(rawContent)
          val checkedCount = items.count { it.isChecked }
          val summary = "[$checkedCount/${items.size} tamamlandı]"
          val previewLines = items.joinToString("\n") { (if (it.isChecked) "☑ " else "☐ ") + it.text }
          displayContent = "$summary\n$previewLines"
        } catch (e: Exception) {
          // JSON parse başarısız — ham içerik bildirimde gösterilecek
          android.util.Log.w("ReminderReceiver", "Checklist parse hatası, ham içerik kullanılıyor", e)
        }
      }

      showNotification(context, noteId, title, displayContent, isLocked, whenMillis)
    }

    private fun showNotification(
      context: Context,
      noteId: Long,
      title: String,
      content: String,
      isLocked: Boolean,
      whenMillis: Long?
    ) {
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

      val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_stat_notification_icon)
        .setContentTitle(title.ifBlank { context.getString(R.string.reminder_title) })
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      if (whenMillis != null) {
        builder.setWhen(whenMillis).setShowWhen(true)
      }

      if (isLocked) {
        // Kilit ekranı hassas içeriği gizleyecek şekilde ayarlıysa başlık yerine bu genel sürüm görünür
        val publicVersion = NotificationCompat.Builder(context, channelId)
          .setSmallIcon(R.drawable.ic_stat_notification_icon)
          .setContentTitle(context.getString(R.string.reminder_title))
          .setContentText(context.getString(R.string.reminder_locked_note))
          .build()
        builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
          .setPublicVersion(publicVersion)
      }

      notificationManager.notify(noteId.toInt(), builder.build())
    }
  }
}
