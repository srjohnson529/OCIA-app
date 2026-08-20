package com.illumined.app.data

import com.google.firebase.functions.FirebaseFunctions

class ClassManagementRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    fun archiveClass(classId: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        changeArchiveState("archiveClass", classId, onSuccess, onError)
    }

    fun restoreClass(classId: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        changeArchiveState("restoreClass", classId, onSuccess, onError)
    }

    private fun changeArchiveState(
        functionName: String,
        classId: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        functions.getHttpsCallable(functionName)
            .call(mapOf("classId" to classId))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }
}
