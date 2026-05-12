package com.notdefterim.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
}
