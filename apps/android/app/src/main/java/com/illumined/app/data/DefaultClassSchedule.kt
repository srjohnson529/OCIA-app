package com.illumined.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

internal data class ClassSession(val date: Date, val topic: String)

internal object DefaultClassSchedule {
    private val sessions = listOf(
        "2026-08-09" to "Introduction & The O.C.I.A.",
        "2026-08-16" to "Revelation — Scripture",
        "2026-08-23" to "Revelation — Tradition",
        "2026-08-30" to "Salvation History & The Creed",
        "2026-09-06" to "God & the Blessed Trinity",
        "2026-09-13" to "Creation & Humanity",
        "2026-09-20" to "Jesus Christ, Incarnation & Public Ministry",
        "2026-09-27" to "The Paschal Mystery",
        "2026-10-04" to "Holy Spirit & The Church",
        "2026-10-11" to "Communion of the Saints",
        "2026-10-18" to "The Blessed Virgin Mary",
        "2026-10-25" to "Last Things — Death, Judgement, Heaven, Hell",
        "2026-11-01" to "The Early Church and the Development of Doctrine",
        "2026-11-08" to "Q&A (Last Session of Pre-Catechumenate)",
        "2026-11-15" to "Rite of Acceptance and Welcome with Sponsor",
        "2026-11-15" to "Introduction to the Seven Sacraments",
        "2026-11-22" to "Sacraments of Initiation Pt. I: Baptism & Confirmation",
        "2026-11-29" to "Sacraments of Initiation Pt. II: Holy Eucharist",
        "2026-12-06" to "Sacraments of Vocation: Marriage & Holy Orders",
        "2026-12-13" to "Sacraments of Healing: Reconciliation & Anointing",
        "2026-12-20" to "Sacred Liturgy & The Mass",
        "2026-12-27" to "Christmas: No Class",
        "2027-01-03" to "Foundations of Morality I",
        "2027-01-10" to "Foundations of Morality II",
        "2027-01-17" to "Foundations of Morality III",
        "2027-01-24" to "Catholic Social Doctrine",
        "2027-01-31" to "Church: Mother and Teacher",
        "2027-02-07" to "Q&A Catechumenate Wrap-up & Purification and Enlightenment Preparation",
        "2027-02-10" to "Ash Wednesday",
        "2027-02-14" to "Rite of Sending",
        "2027-02-14" to "Rite of Election",
        "2027-02-14" to "Introduction to the Ten Commandments & Commandments 1–3",
        "2027-02-21" to "Commandments 4 & 5",
        "2027-02-28" to "First Scrutiny",
        "2027-02-28" to "Commandments 6 & 9",
        "2027-03-07" to "Second Scrutiny",
        "2027-03-08" to "Commandments 7, 8, & 10",
        "2027-03-14" to "Third Scrutiny",
        "2027-03-15" to "Christian Prayer & the Lord's Prayer",
        "2027-03-20" to "Lectio Divina & The Rosary + Rehearsal (Saturday)",
        "2027-03-21" to "Liturgy of the Hours & Adoration",
        "2027-03-25" to "Holy Thursday: The Lord's Supper",
        "2027-03-26" to "Good Friday: Stations of the Cross and Good Friday Service",
        "2027-03-27" to "Holy Saturday",
        "2027-03-27" to "Easter Vigil: Baptism, Confirmation, and Eucharist",
        "2027-03-28" to "Easter Sunday",
        "2027-04-11" to "Reflection on Easter Vigil",
        "2027-05-02" to "Living the Sacramental Life",
        "2027-06-06" to "Prayer and Discernment",
        "2027-07-04" to "Mission and Evangelization",
    ).map { (date, topic) -> LocalDate.parse(date) to topic }

    internal val size: Int get() = sessions.size

    fun next(remote: List<ScheduleItem>, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): ClassSession {
        val today = now.atZone(zone).toLocalDate()
        val remoteNext = remote.asSequence()
            .mapNotNull { item -> item.date?.toDate()?.let { date -> ClassSession(date, item.topic) } }
            .filter { it.date.toInstant().atZone(zone).toLocalDate() > today }
            .minByOrNull { it.date.time }
        if (remoteNext != null) return remoteNext

        val fallback = sessions.firstOrNull { it.first > today } ?: sessions.first()
        return ClassSession(Date.from(fallback.first.atStartOfDay(zone).toInstant()), fallback.second)
    }
}
