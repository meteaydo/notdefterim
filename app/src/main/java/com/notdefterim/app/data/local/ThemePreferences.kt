package com.notdefterim.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow<Boolean?>(
        if (prefs.contains("is_dark")) prefs.getBoolean("is_dark", false) else null
    )
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    fun toggleTheme(currentSystemDark: Boolean) {
        val current = _isDarkTheme.value ?: currentSystemDark
        val next = !current
        prefs.edit().putBoolean("is_dark", next).apply()
        _isDarkTheme.value = next
    }

    /** Yerel yedek için (K7); tema hiç seçilmediyse (sistem teması) boş kalır. */
    fun backupSettings(): JSONObject = JSONObject().apply {
        _isDarkTheme.value?.let { put("is_dark", it) }
    }

    /** Yedekten gelen temayı yazar; süreç hemen yeniden başlatıldığı için commit() ile. */
    fun restoreSettings(json: JSONObject) {
        val editor = prefs.edit()
        if (json.has("is_dark")) editor.putBoolean("is_dark", json.getBoolean("is_dark")) else editor.remove("is_dark")
        editor.commit()
    }
}
