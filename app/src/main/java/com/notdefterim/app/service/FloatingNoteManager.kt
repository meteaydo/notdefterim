package com.notdefterim.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.notdefterim.app.R
import android.util.Log

object FloatingNoteManager {
    private var floatingView: View? = null
    private var windowManager: WindowManager? = null

    fun show(context: Context, title: String, content: String) {
        try {
            val appCtx = context.applicationContext
            windowManager = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // Varsa eski pencereyi kapat
            if (floatingView != null && floatingView?.isAttachedToWindow == true) {
                windowManager?.removeView(floatingView)
            }

            val inflater = appCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            floatingView = inflater.inflate(R.layout.layout_floating_note, null)

            val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                (appCtx.resources.displayMetrics.widthPixels * 0.65f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamsType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            // Opaklık %90
            params.alpha = 0.9f

            params.gravity = Gravity.TOP or Gravity.START
            params.x = 100
            params.y = 200

            val tvNoteTitle = floatingView?.findViewById<TextView>(R.id.tvNoteTitle)
            val tvNoteContent = floatingView?.findViewById<TextView>(R.id.tvNoteContent)
            val btnClose = floatingView?.findViewById<ImageView>(R.id.btnClose)
            val btnCopyContent = floatingView?.findViewById<View>(R.id.btnCopyContent)
            val svNoteContent = floatingView?.findViewById<View>(R.id.svNoteContent)

            // ScrollView için maksimum yükseklik sınırı (ekranın %45'i)
            svNoteContent?.viewTreeObserver?.addOnGlobalLayoutListener {
                val maxHeight = (appCtx.resources.displayMetrics.heightPixels * 0.45f).toInt()
                if (svNoteContent.height > maxHeight) {
                    val svParams = svNoteContent.layoutParams
                    svParams.height = maxHeight
                    svNoteContent.layoutParams = svParams
                }
            }

            tvNoteTitle?.text = if (title.isBlank()) "Not" else title
            tvNoteContent?.text = content

            btnClose?.setOnClickListener {
                close()
            }
            // Dokunarak kopyalama iptal edildi.
            // Her yerden sürüklenebilirlik ve tıklama mekanizması
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false

            val dragAndClickListener = View.OnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = Math.abs(event.rawX - initialTouchX)
                        val deltaY = Math.abs(event.rawY - initialTouchY)
                        if (deltaX > 10 || deltaY > 10) {
                            isDragging = true
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(floatingView, params)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            view.performClick()
                        }
                        true
                    }
                    else -> false
                }
            }

            // Sürükleme dinleyicilerini başlık, kapat butonu ve kök görünüme ata.
            // btnCopyContent'e atamıyoruz ki ScrollView dikey kaydırma işlemlerini algılayabilsin.
            floatingView?.setOnTouchListener(dragAndClickListener)
            tvNoteTitle?.setOnTouchListener(dragAndClickListener)
            btnClose?.setOnTouchListener(dragAndClickListener)

            windowManager?.addView(floatingView, params)
            Log.d("FloatingNote", "View added successfully")
        } catch (e: Exception) {
            Log.e("FloatingNote", "Error showing floating note", e)
            Toast.makeText(context, "Pencere açılamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun close() {
        try {
            if (floatingView != null && floatingView?.isAttachedToWindow == true) {
                windowManager?.removeView(floatingView)
            }
            floatingView = null
        } catch (e: Exception) {
            Log.e("FloatingNote", "Error closing note widget", e)
        }
    }
}
