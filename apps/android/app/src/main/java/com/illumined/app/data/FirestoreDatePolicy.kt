package com.illumined.app.data

import java.util.Calendar
import java.util.TimeZone

internal object FirestoreDatePolicy {
    fun localStartOfDayMillis(value: Long, timeZone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(timeZone).run {
            timeInMillis = value
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
}
