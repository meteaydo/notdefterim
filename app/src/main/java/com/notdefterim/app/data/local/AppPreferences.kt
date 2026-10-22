package com.notdefterim.app.data.local

import android.content.Context
import com.notdefterim.app.core.security.PinHasher
import com.notdefterim.app.core.security.PinCheck
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

    companion object {
        /** Bu kadar hatalı denemeden sonra bekleme süresi başlar */
        private const val PIN_FREE_ATTEMPTS = 5
        private const val PIN_FIRST_LOCK_MS = 30_000L
        private const val PIN_MAX_LOCK_MS = 30 * 60_000L
    }

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

    /**
     * Ham PIN asla saklanmaz; yalnızca PBKDF2 hash'i tutulur.
     * null → PIN kurulmamış demektir.
     */
    private val _appPinHash = MutableStateFlow(prefs.getString("app_pin_hash", null))
    val appPinHash: StateFlow<String?> = _appPinHash.asStateFlow()

    /** PIN belirlenip belirlenmediğini bildiren kolaylık alanı. */
    val appPin: StateFlow<String?> = _appPinHash  // Geriye dönük uyumluluk için (null = yok)

    private val _appPinHint = MutableStateFlow(prefs.getString("app_pin_hint", null))
    val appPinHint: StateFlow<String?> = _appPinHint.asStateFlow()

    /** Bkz. [com.notdefterim.app.domain.model.PinScope] */
    private val _appPinScope = MutableStateFlow(prefs.getInt("app_pin_scope", 0)) // PinScope.ALL.value
    val appPinScope: StateFlow<Int> = _appPinScope.asStateFlow()

    /** Bkz. [com.notdefterim.app.domain.model.StartupBehavior] */
    private val _startupBehavior = MutableStateFlow(prefs.getInt("startup_behavior", 0)) // StartupBehavior.LAST_OPENED.value
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

    /**
     * PIN'i kaydeder. Ham PIN disk'e hiç yazılmaz;
     * PBKDF2-HMAC-SHA256 hash'i + salt saklanır.
     * Doğrulama için [checkPin] kullanılmalıdır.
     */
    fun setAppPin(pin: String?, hint: String? = null) {
        if (pin == null) {
            prefs.edit()
                .remove("app_pin_hash")
                .remove("app_pin_hint")
                .remove("pin_failed_attempts")
                .remove("pin_locked_until")
                // Eski düz metin alanını da temizle (migration)
                .remove("app_pin")
                .apply()
            _appPinHash.value = null
            _appPinHint.value = null
        } else {
            val hash = PinHasher.hash(pin)
            prefs.edit()
                .putString("app_pin_hash", hash)
                .putString("app_pin_hint", hint)
                .remove("pin_failed_attempts")
                .remove("pin_locked_until")
                // Eski düz metin alanını temizle (migration)
                .remove("app_pin")
                .apply()
            _appPinHash.value = hash
            _appPinHint.value = hint
        }
    }

    /**
     * Kullanıcının girdiği PIN'i saklanan hash ile karşılaştırır.
     *
     * Deneme sınırı: [PIN_FREE_ATTEMPTS] hatalı denemeden sonra bekleme süresi başlar (30 sn, sonraki
     * her hatada iki katı, en çok 30 dk). Süre dolmadan yapılan deneme kontrol edilmeden reddedilir.
     * Sayaç uygulama kapansa da korunur; doğru PIN ve yeni PIN onu sıfırlar.
     */
    fun checkPin(pin: String): PinCheck {
        val now = System.currentTimeMillis()
        val lockedUntil = prefs.getLong("pin_locked_until", 0L)
        if (now < lockedUntil) return PinCheck.LockedOut(secondsUntil(lockedUntil, now))

        val storedHash = _appPinHash.value ?: return PinCheck.Wrong
        if (PinHasher.verify(pin, storedHash)) {
            prefs.edit().remove("pin_failed_attempts").remove("pin_locked_until").apply()
            return PinCheck.Correct
        }

        val failures = prefs.getInt("pin_failed_attempts", 0) + 1
        val editor = prefs.edit().putInt("pin_failed_attempts", failures)
        if (failures < PIN_FREE_ATTEMPTS) {
            editor.apply()
            return PinCheck.Wrong
        }
        val doublings = minOf(failures - PIN_FREE_ATTEMPTS, 10)
        val lockMs = minOf(PIN_FIRST_LOCK_MS shl doublings, PIN_MAX_LOCK_MS)
        editor.putLong("pin_locked_until", now + lockMs).apply()
        return PinCheck.LockedOut(lockMs / 1000)
    }

    private fun secondsUntil(time: Long, now: Long): Long = (time - now + 999) / 1000
}
