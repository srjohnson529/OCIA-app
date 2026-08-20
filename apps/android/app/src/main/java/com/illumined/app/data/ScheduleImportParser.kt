package com.illumined.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class ImportedScheduleRow(val rowNumber: Int, val date: LocalDate, val topic: String, val details: String)

sealed interface ScheduleParseResult {
    data class Success(val rows: List<ImportedScheduleRow>) : ScheduleParseResult
    data class Failure(val message: String) : ScheduleParseResult
}

object ScheduleImportParser {
    private val formats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("M/d/uuuu"),
        DateTimeFormatter.ofPattern("MM/dd/uuuu"),
        DateTimeFormatter.ofPattern("M-d-uuuu"),
    )
    private val headers = setOf("date", "topic", "details", "description", "notes")

    fun parse(input: String): ScheduleParseResult {
        val sourceRows = input.lineSequence().mapIndexed { index, line -> index + 1 to line.trim() }.filter { it.second.isNotEmpty() }.toList()
        if (sourceRows.isEmpty()) return ScheduleParseResult.Failure("Paste your schedule first. Use one row for each class.")
        val parsed = mutableListOf<ImportedScheduleRow>(); val errors = mutableListOf<String>()
        sourceRows.forEachIndexed { sourceIndex, (rowNumber, text) ->
            val fields = parseDelimitedRow(text)
            if (sourceIndex == 0 && fields.any { it.trim().lowercase() in headers }) return@forEachIndexed
            if (fields.size < 2) { errors += "Row $rowNumber: add at least a date and a topic."; return@forEachIndexed }
            val date = parseDate(fields[0]); if (date == null) { errors += "Row $rowNumber: '${fields[0]}' is not a date I recognize. Use YYYY-MM-DD or MM/DD/YYYY."; return@forEachIndexed }
            val topic = fields[1].trim(); if (topic.isEmpty()) { errors += "Row $rowNumber: add a topic."; return@forEachIndexed }
            parsed += ImportedScheduleRow(rowNumber, date, topic, fields.drop(2).joinToString(", ").trim())
        }
        if (errors.isNotEmpty()) return ScheduleParseResult.Failure(errors.take(6).joinToString("\n"))
        if (parsed.isEmpty()) return ScheduleParseResult.Failure("No class rows were found. Make sure the first columns are date and topic.")
        return ScheduleParseResult.Success(parsed.sortedWith(compareBy({ it.date }, { it.rowNumber })))
    }

    private fun parseDate(value: String): LocalDate? = formats.firstNotNullOfOrNull { formatter ->
        try { LocalDate.parse(value.trim(), formatter) } catch (_: DateTimeParseException) { null }
    }

    private fun parseDelimitedRow(row: String): List<String> {
        val delimiter = if ('\t' in row) '\t' else ','; val values = mutableListOf<String>(); val current = StringBuilder(); var quoted = false; var index = 0
        while (index < row.length) { val char = row[index]; when { char == '"' && quoted && index + 1 < row.length && row[index + 1] == '"' -> { current.append('"'); index++ }; char == '"' -> quoted = !quoted; char == delimiter && !quoted -> { values += current.toString().trim(); current.clear() }; else -> current.append(char) }; index++ }
        values += current.toString().trim(); return values
    }
}
