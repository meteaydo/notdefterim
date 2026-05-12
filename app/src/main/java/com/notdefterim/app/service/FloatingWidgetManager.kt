package com.notdefterim.app.service

import android.content.ClipData
import android.content.ClipboardManager
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

object FloatingWidgetManager {
    private var floatingView: View? = null
    private var windowManager: WindowManager? = null

    fun show(context: Context, platform: String, username: String, passwordValue: String) {
        try {
            val appCtx = context.applicationContext
            windowManager = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // Varsa eski pencereyi kapat
            if (floatingView != null && floatingView?.isAttachedToWindow == true) {
                windowManager?.removeView(floatingView)
            }

            val inflater = appCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            floatingView = inflater.inflate(R.layout.layout_floating_widget, null)

            val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                (appCtx.resources.displayMetrics.widthPixels * 0.6f).toInt(),
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

            val tvPlatform = floatingView?.findViewById<TextView>(R.id.tvPlatform)
            val tvUsername = floatingView?.findViewById<TextView>(R.id.tvUsername)
            val btnClose = floatingView?.findViewById<ImageView>(R.id.btnClose)
            val btnCopyUsername = floatingView?.findViewById<View>(R.id.btnCopyUsername)
            val btnCopyPassword = floatingView?.findViewById<View>(R.id.btnCopyPassword)

            tvPlatform?.text = platform
            tvUsername?.text = username

            btnClose?.setOnClickListener {
                close()
            }

            fun launchClipboardActivity(label: String, text: String) {
                val intent = android.content.Intent(appCtx, ClipboardActivity::class.java).apply {
                    putExtra("EXTRA_LABEL", label)
                    putExtra("EXTRA_TEXT", text)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                appCtx.startActivity(intent)
            }

            btnCopyUsername?.setOnClickListener {
                launchClipboardActivity("Username", username)
            }

            btnCopyPassword?.setOnClickListener {
                launchClipboardActivity("Password", passwordValue)
            }

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

            // Tüm öğelere aynı dinleyiciyi ata
            floatingView?.setOnTouchListener(dragAndClickListener)
            tvPlatform?.setOnTouchListener(dragAndClickListener)
            btnClose?.setOnTouchListener(dragAndClickListener)
            btnCopyUsername?.setOnTouchListener(dragAndClickListener)
            btnCopyPassword?.setOnTouchListener(dragAndClickListener)

            windowManager?.addView(floatingView, params)
            Log.d("FloatingWidget", "View added successfully")
        } catch (e: Exception) {
            Log.e("FloatingWidget", "Error showing floating widget", e)
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
            Log.e("FloatingWidget", "Error closing widget", e)
        }
    }
}
