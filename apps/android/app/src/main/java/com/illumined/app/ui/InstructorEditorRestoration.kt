package com.illumined.app.ui

internal fun <T> restoreEditedRecord(values: List<T>, savedId: String?, id: (T) -> String): T? =
    savedId?.let { target -> values.firstOrNull { id(it) == target } }
