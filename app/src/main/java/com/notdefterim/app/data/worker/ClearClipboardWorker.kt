package com.notdefterim.app.data.worker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Kopyalanan parolayı süre dolunca panodan siler (bkz. SensitiveClipboard).
 *
 * Android 10+ arka plandaki uygulamaların panoyu okumasına izin vermez; bu yüzden içerik
 * kontrol edilmeden silinir. Arada başka bir şey kopyalandıysa o da silinir.
 */
class ClearClipboardWorker(
  context: Context,
  workerParams: WorkerParameters
) : Worker(context, workerParams) {

  override fun doWork(): Result {
    val clipboardManager = applicationContext.getSystemService(ClipboardManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      clipboardManager.clearPrimaryClip()
    } else {
      clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
    }
    return Result.success()
  }
}
