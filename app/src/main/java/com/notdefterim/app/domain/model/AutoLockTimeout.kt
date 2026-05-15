package com.notdefterim.app.domain.model

import com.notdefterim.app.R

enum class AutoLockTimeout(val timeoutMs: Long, val titleResId: Int) {
  IMMEDIATELY(0L, R.string.timeout_immediately),
  MIN_2(120_000L, R.string.timeout_2_min),
  MIN_5(300_000L, R.string.timeout_5_min),
  MIN_10(600_000L, R.string.timeout_10_min),
  MIN_30(1800_000L, R.string.timeout_30_min),
  NEVER(-1L, R.string.timeout_never);
  
  companion object {
    fun fromMs(ms: Long): AutoLockTimeout {
       return entries.find { it.timeoutMs == ms } ?: MIN_5
    }
  }
}
