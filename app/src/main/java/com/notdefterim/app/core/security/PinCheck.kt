package com.notdefterim.app.core.security

/** Uygulama içi PIN denemesinin sonucu. */
sealed interface PinCheck {
  data object Correct : PinCheck
  data object Wrong : PinCheck

  /** Çok fazla hatalı deneme; [seconds] saniye boyunca deneme kabul edilmez. */
  data class LockedOut(val seconds: Long) : PinCheck
}
