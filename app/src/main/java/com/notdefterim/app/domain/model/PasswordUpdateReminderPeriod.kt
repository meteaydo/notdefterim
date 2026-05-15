package com.notdefterim.app.domain.model

import com.notdefterim.app.R

enum class PasswordUpdateReminderPeriod(val periodMs: Long, val titleResId: Int) {
  MONTH_3(7_776_000_000L, R.string.reminder_3_months), // ~90 days
  MONTH_6(15_552_000_000L, R.string.reminder_6_months), // ~180 days
  MONTH_9(23_328_000_000L, R.string.reminder_9_months), // ~270 days
  MONTH_12(31_536_000_000L, R.string.reminder_12_months), // ~365 days
  NEVER(-1L, R.string.timeout_never);
  
  companion object {
    fun fromMs(ms: Long): PasswordUpdateReminderPeriod {
       return entries.find { it.periodMs == ms } ?: MONTH_6
    }
  }
}
