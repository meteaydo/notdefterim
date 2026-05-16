package com.notdefterim.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _autoLockTimeout = MutableStateFlow(
        prefs.getLong("auto_lock_timeout", 300_000L) // Default is 5 Min (300,000 ms)
    )
    val autoLockTimeout: StateFlow<Long> = _autoLockTimeout.asStateFlow()

    private val _passwordReminderPeriod = MutableStateFlow(
        prefs.getLong("password_reminder_period", 15_552_000_000L) // Default is 6 Months
    )
    val passwordReminderPeriod: StateFlow<Long> = _passwordReminderPeriod.asStateFlow()

    private val _dismissedReminders = MutableStateFlow(
        prefs.getStringSet("dismissed_reminders", emptySet())?.toSet() ?: emptySet()
    )
    val dismissedReminders: StateFlow<Set<String>> = _dismissedReminders.asStateFlow()

    private val _appPin = MutableStateFlow(prefs.getString("app_pin", null))
    val appPin: StateFlow<String?> = _appPin.asStateFlow()

    private val _appPinHint = MutableStateFlow(prefs.getString("app_pin_hint", null))
    val appPinHint: StateFlow<String?> = _appPinHint.asStateFlow()

    private val _appPinScope = MutableStateFlow(prefs.getInt("app_pin_scope", 0)) // 0: All, 1: Notes, 2: Passwords
    val appPinScope: StateFlow<Int> = _appPinScope.asStateFlow()

    private val _startupBehavior = MutableStateFlow(prefs.getInt("startup_behavior", 0)) // 0: Last Opened, 1: Notes, 2: Passwords
    val startupBehavior: StateFlow<Int> = _startupBehavior.asStateFlow()

    private val _lastOpenedTab = MutableStateFlow(prefs.getInt("last_opened_tab", 0)) // 0: Notes, 1: Passwords
    val lastOpenedTab: StateFlow<Int> = _lastOpenedTab.asStateFlow()

    fun setAutoLockTimeout(timeoutMs: Long) {
        prefs.edit().putLong("auto_lock_timeout", timeoutMs).apply()
        _autoLockTimeout.value = timeoutMs
    }

    fun setAppPinScope(scope: Int) {
        prefs.edit().putInt("app_pin_scope", scope).apply()
        _appPinScope.value = scope
    }

    fun setStartupBehavior(behavior: Int) {
        prefs.edit().putInt("startup_behavior", behavior).apply()
        _startupBehavior.value = behavior
    }

    fun setLastOpenedTab(tab: Int) {
        prefs.edit().putInt("last_opened_tab", tab).apply()
        _lastOpenedTab.value = tab
    }

    fun setPasswordReminderPeriod(periodMs: Long) {
        prefs.edit().putLong("password_reminder_period", periodMs).apply()
        _passwordReminderPeriod.value = periodMs
    }

    fun dismissReminder(passwordId: Long, timeToCompare: Long) {
        val key = "${passwordId}_${timeToCompare}"
        val currentSet = _dismissedReminders.value.toMutableSet()
        currentSet.add(key)
        prefs.edit().putStringSet("dismissed_reminders", currentSet).apply()
        _dismissedReminders.value = currentSet
    }

    fun setAppPin(pin: String?, hint: String? = null) {
        if (pin == null) {
            prefs.edit().remove("app_pin").remove("app_pin_hint").apply()
            _appPinHint.value = null
        } else {
            prefs.edit().putString("app_pin", pin).putString("app_pin_hint", hint).apply()
            _appPinHint.value = hint
        }
        _appPin.value = pin
    }
}
