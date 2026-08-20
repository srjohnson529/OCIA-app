package com.illumined.app.data

import java.time.Instant
import java.time.ZoneId
import java.util.Date

internal data class ClassSession(
    val id: String,
    val date: Date,
    val topic: String,
    val sortOrder: Long?,
)

internal data class ClassScheduleDay(
    val date: Date,
    val sessions: List<ClassSession>,
)

internal object ClassScheduleSelection {
    fun nextDay(
        schedule: List<ScheduleItem>,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): ClassScheduleDay? {
        val today = now.atZone(zone).toLocalDate()
        val upcoming = schedule.mapNotNull { item ->
            item.date?.toDate()?.let { date ->
                ClassSession(item.id, date, item.topic, item.sortOrder)
            }
        }.filter { session ->
            !session.date.toInstant().atZone(zone).toLocalDate().isBefore(today)
        }
        val nextDate = upcoming.minOfOrNull {
            it.date.toInstant().atZone(zone).toLocalDate()
        } ?: return null
        val sessions = upcoming.filter {
            it.date.toInstant().atZone(zone).toLocalDate() == nextDate
        }.sortedWith(compareBy(
            { it.sortOrder ?: Long.MAX_VALUE },
            { it.topic.lowercase() },
            { it.id },
        ))
        return ClassScheduleDay(
            Date.from(nextDate.atStartOfDay(zone).toInstant()),
            sessions,
        )
    }
}
