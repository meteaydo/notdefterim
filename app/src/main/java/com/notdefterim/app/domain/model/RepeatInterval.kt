package com.notdefterim.app.domain.model

enum class RepeatInterval {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    /** Haftanın seçili günlerinde (Note.repeatDays) */
    DAYS_OF_WEEK
}
