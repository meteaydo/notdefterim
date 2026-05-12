package com.notdefterim.app.service

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast

class ClipboardActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val label = intent.getStringExtra("EXTRA_LABEL") ?: "Panoya"
        val text = intent.getStringExtra("EXTRA_TEXT") ?: ""
        
        if (text.isNotEmpty()) {
            try {
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(label, text)
                clipboardManager.setPrimaryClip(clip)
                
                val successMsg = if (label == "Username") "Kullanıcı adı kopyalandı" else "Parola kopyalandı"
                Toast.makeText(applicationContext, successMsg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Kopyalama başarısız oldu", Toast.LENGTH_SHORT).show()
            }
        }
        
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
