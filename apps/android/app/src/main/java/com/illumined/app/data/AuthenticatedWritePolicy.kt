package com.illumined.app.data

internal object AuthenticatedWritePolicy {
    fun primaryClassId(profile: UserProfile) = profile.classIds.firstOrNull().orEmpty()
    fun hasWritableClass(profile: UserProfile) = primaryClassId(profile).isNotBlank()
}
