package com.notdefterim.app.service

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.notdefterim.app.R

class FloatingPasswordService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val platform = intent?.getStringExtra("PLATFORM") ?: "Platform"
        val username = intent?.getStringExtra("USERNAME") ?: "Username"
        val passwordValue = intent?.getStringExtra("PASSWORD") ?: ""

        setupFloatingView(platform, username, passwordValue)
        
        return START_NOT_STICKY
    }

    private fun setupFloatingView(platform: String, username: String, passwordValue: String) {
        // Eğer zaten açıksa kapat
        if (::floatingView.isInitialized && floatingView.isAttachedToWindow) {
            windowManager.removeView(floatingView)
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val ctx = android.view.ContextThemeWrapper(this, R.style.Theme_NotDefterim)
        floatingView = LayoutInflater.from(ctx).inflate(R.layout.layout_floating_widget, null)

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        windowManager.addView(floatingView, params)

        val tvPlatform = floatingView.findViewById<TextView>(R.id.tvPlatform)
        val tvUsername = floatingView.findViewById<TextView>(R.id.tvUsername)
        val btnClose = floatingView.findViewById<ImageView>(R.id.btnClose)
        val btnCopyUsername = floatingView.findViewById<Button>(R.id.btnCopyUsername)
        val btnCopyPassword = floatingView.findViewById<Button>(R.id.btnCopyPassword)

        tvPlatform.text = platform
        tvUsername.text = username

        btnClose.setOnClickListener {
            stopSelf()
        }

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        btnCopyUsername.setOnClickListener {
            val clip = ClipData.newPlainText("Username", username)
            clipboardManager.setPrimaryClip(clip)
            Toast.makeText(this, "Kullanıcı adı kopyalandı", Toast.LENGTH_SHORT).show()
        }

        btnCopyPassword.setOnClickListener {
            val clip = ClipData.newPlainText("Password", passwordValue)
            clipboardManager.setPrimaryClip(clip)
            Toast.makeText(this, "Parola kopyalandı", Toast.LENGTH_SHORT).show()
        }

        // Sürükleme özelliği
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized && floatingView.isAttachedToWindow) {
            windowManager.removeView(floatingView)
        }
    }
}
