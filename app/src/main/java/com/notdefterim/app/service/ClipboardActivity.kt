package com.notdefterim.app.service

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.notdefterim.app.R
import com.notdefterim.app.util.SensitiveClipboard

class ClipboardActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val label = intent.getStringExtra("EXTRA_LABEL") ?: "Panoya"
        val text = intent.getStringExtra("EXTRA_TEXT") ?: ""
        
        if (text.isNotEmpty()) {
            try {
                if (label == "Password") {
                    SensitiveClipboard.copy(this, label, text)
                } else {
                    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(label, text)
                    clipboardManager.setPrimaryClip(clip)
                }

                val successMsg = getString(if (label == "Username") R.string.username_copied else R.string.password_copied)
                Toast.makeText(applicationContext, successMsg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(applicationContext, getString(R.string.copy_failed), Toast.LENGTH_SHORT).show()
            }
        }
        
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
