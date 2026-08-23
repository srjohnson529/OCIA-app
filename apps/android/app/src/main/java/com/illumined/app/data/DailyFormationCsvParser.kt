package com.illumined.app.data

import java.time.LocalDate
import java.time.format.DateTimeParseException

data class ImportedDailyFormationRow(
    val rowNumber: Int,
    val date: String,
    val type: String,
    val title: String,
    val details: String,
    val colorCode: String,
)

data class DailyFormationCsvIssue(val rowNumber: Int, val message: String)

data class DailyFormationCsvPreview(
    val totalRows: Int,
    val validRows: List<ImportedDailyFormationRow>,
    val issues: List<DailyFormationCsvIssue>,
)

object DailyFormationCsvParser {
    private val requiredHeaders = setOf("date", "type", "title", "details")
    private val validTypes = setOf("fact", "saint", "note")
    private val validColors = setOf("WHITE", "GOLD", "GREEN", "RED", "PURPLE", "ROSE")

    fun parse(input: String): DailyFormationCsvPreview {
        val records = parseRecords(input)
        if (records.isEmpty()) return DailyFormationCsvPreview(
            0,
            emptyList(),
            listOf(DailyFormationCsvIssue(1, "Choose a CSV file or paste CSV content first.")),
        )
        val headers = records.first().fields.map { it.trim().lowercase() }
        val missing = requiredHeaders.filterNot(headers::contains).toMutableList()
        if ("color" !in headers && "colorcode" !in headers) missing += "color"
        if (missing.isNotEmpty()) return DailyFormationCsvPreview(
            (records.size - 1).coerceAtLeast(0),
            emptyList(),
            listOf(DailyFormationCsvIssue(records.first().rowNumber, "Missing required columns: ${missing.joinToString()}.") ),
        )

        val valid = mutableListOf<ImportedDailyFormationRow>()
        val issues = mutableListOf<DailyFormationCsvIssue>()
        records.drop(1).forEach { record ->
            val values = headers.mapIndexedNotNull { index, header ->
                header.takeIf { it.isNotEmpty() }?.let { it to record.fields.getOrElse(index) { "" }.trim() }
            }.toMap()
            val date = values["date"].orEmpty()
            val type = values["type"].orEmpty().lowercase()
            val title = values["title"].orEmpty()
            val details = values["details"].orEmpty()
            val color = (values["color"] ?: values["colorcode"]).orEmpty().uppercase()
            val problems = mutableListOf<String>()
            if (!isValidDate(date)) problems += "invalid date"
            if (type !in validTypes) problems += "invalid type"
            if (title.isBlank()) problems += "missing title"
            if (details.isBlank()) problems += "missing details"
            if (color !in validColors) problems += "invalid color"
            if (problems.isEmpty()) {
                valid += ImportedDailyFormationRow(record.rowNumber, date, type, title, details, color)
            } else {
                issues += DailyFormationCsvIssue(record.rowNumber, problems.joinToString())
            }
        }
        return DailyFormationCsvPreview(records.size - 1, valid, issues)
    }

    private fun isValidDate(value: String): Boolean {
        if (!value.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) return false
        return try { LocalDate.parse(value); true } catch (_: DateTimeParseException) { false }
    }

    private data class CsvRecord(val rowNumber: Int, val fields: List<String>)

    private fun parseRecords(input: String): List<CsvRecord> {
        val records = mutableListOf<CsvRecord>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var line = 1
        var recordStart = 1
        var index = 0

        fun finishRecord() {
            fields += field.toString()
            if (fields.any { it.isNotBlank() }) records += CsvRecord(recordStart, fields.toList())
            fields = mutableListOf()
            field.clear()
            recordStart = line + 1
        }

        while (index < input.length) {
            val char = input[index]
            when {
                char == '"' && quoted && index + 1 < input.length && input[index + 1] == '"' -> {
                    field.append('"'); index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> { fields += field.toString(); field.clear() }
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < input.length && input[index + 1] == '\n') index++
                    finishRecord(); line++; recordStart = line
                }
                else -> { field.append(char); if (char == '\n') line++ }
            }
            index++
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) finishRecord()
        return records
    }
}
