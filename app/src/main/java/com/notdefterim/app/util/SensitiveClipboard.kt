package com.notdefterim.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.notdefterim.app.data.worker.ClearClipboardWorker
import java.util.concurrent.TimeUnit

/**
 * Parola gibi hassas metinleri panoya kopyalar.
 * - Hassas olarak işaretler: Android 13+ pano önizlemesinde ve klavyelerin pano geçmişinde gizlenir.
 * - [CLEAR_DELAY_MINUTES] dakika sonra panoyu temizler; her kopyalamada süre baştan başlar.
 */
object SensitiveClipboard {
  private const val CLEAR_WORK_NAME = "notdefterim_clear_clipboard"
  private const val CLEAR_DELAY_MINUTES = 2L

  // ClipDescription.EXTRA_IS_SENSITIVE (API 33) ile aynı anahtar; eski sürümlerde de klavyeler okur
  private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

  fun copy(context: Context, label: String, text: String) {
    val clip = ClipData.newPlainText(label, text).apply {
      description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
    }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)

    val clearRequest = OneTimeWorkRequestBuilder<ClearClipboardWorker>()
      .setInitialDelay(CLEAR_DELAY_MINUTES, TimeUnit.MINUTES)
      .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
      CLEAR_WORK_NAME,
      ExistingWorkPolicy.REPLACE,
      clearRequest
    )
  }
}
