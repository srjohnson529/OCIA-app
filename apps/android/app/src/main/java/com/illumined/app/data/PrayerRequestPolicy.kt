package com.illumined.app.data

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object PrayerRequestPolicy {
    const val LIFETIME_MILLIS: Long = 3L * 24 * 60 * 60 * 1000

    fun expirationDate(now: Date = Date(), timeZone: TimeZone = TimeZone.getDefault()): Date =
        Calendar.getInstance(timeZone).run {
            time = now
            add(Calendar.DAY_OF_YEAR, 3)
            time
        }

    fun creationError(authenticatedUserId: String?, title: String, classId: String): String? = when {
        authenticatedUserId.isNullOrBlank() -> "Please sign in before posting a prayer request."
        title.isBlank() -> "Please add a title for the prayer request."
        classId.isBlank() -> "Please join a class before posting a prayer request."
        else -> null
    }

    fun <T> recentActive(
        values: List<T>,
        nowSeconds: Long,
        expiresAtSeconds: (T) -> Long?,
        createdAtSeconds: (T) -> Long?,
    ): List<T> = values
        .filter { (expiresAtSeconds(it) ?: Long.MIN_VALUE) > nowSeconds }
        .sortedByDescending { createdAtSeconds(it) ?: Long.MIN_VALUE }
        .take(5)
}
