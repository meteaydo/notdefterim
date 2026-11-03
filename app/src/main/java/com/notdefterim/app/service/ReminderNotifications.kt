package com.notdefterim.app.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Bildirim çekmecesinde hâlâ duran hatırlatıcı bildirimleri.
 * Uygulama simgesindeki sayı bunlardan gelir; ana sayfadaki şerit hangi notlar olduğunu buradan gösterir.
 * Bildirim kimliği not kimliğidir (bkz. [ReminderReceiver.showReminder]).
 */
object ReminderNotifications {

  const val CHANNEL_ID = "notdefterim_reminders"

  private val _noteIds = MutableStateFlow<Set<Long>>(emptySet())
  val noteIds: StateFlow<Set<Long>> = _noteIds.asStateFlow()

  /**
   * Listeyi sistemden baştan okur (ekrana her dönüşte; süreç yeniden başladıysa liste boştur).
   * notify()/cancel() sistemde sıraya alınıp sonra işlendiği için hemen ardından okunan liste eski olabilir;
   * bu yüzden bilinen değişiklikler [posted] ve [removed] ile doğrudan işlenir.
   */
  fun refresh(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    _noteIds.value = runCatching {
      manager.activeNotifications
        .filter { it.notification.channelId == CHANNEL_ID && it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
        .map { it.id.toLong() }
        .toSet()
    }.getOrDefault(emptySet())
  }

  /** Bildirim gönderildi. Bildirimler ya da kanal kapalıysa sistem onu göstermez; şeride de eklenmez. */
  fun posted(context: Context, noteId: Long) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelBlocked = manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
    if (manager.areNotificationsEnabled() && !channelBlocked) _noteIds.update { it + noteId }
  }

  /** Bildirim kapandı: Ertele/Tamam'a basıldı ya da kullanıcı kaydırıp sildi. */
  fun removed(noteId: Long) {
    _noteIds.update { it - noteId }
  }

  /** Not açıldı ya da silindi: bildirimi görülmüş sayılır, kapatılır. */
  fun dismiss(context: Context, noteId: Long) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(noteId.toInt())
    removed(noteId)
  }
}
