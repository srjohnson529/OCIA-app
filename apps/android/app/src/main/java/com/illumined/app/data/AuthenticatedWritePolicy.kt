package com.illumined.app.data

internal object AuthenticatedWritePolicy {
    fun primaryClassId(profile: UserProfile) = profile.selectedClassId
    fun hasWritableClass(profile: UserProfile) = primaryClassId(profile).isNotBlank()
}
