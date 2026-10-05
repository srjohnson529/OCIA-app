plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

// Keep generated artifacts out of macOS Documents/iCloud sync. A checkout-specific
// directory also prevents different worktrees from sharing compiler intermediates.
val externalBuildBase = providers.environmentVariable("ILLUMINED_ANDROID_BUILD_ROOT").orNull
    ?: if (System.getProperty("os.name").startsWith("Mac")) {
        java.io.File(System.getProperty("user.home"), "Library/Caches/Illumined/Android").path
    } else null

if (externalBuildBase != null) {
    val checkoutKey = java.security.MessageDigest.getInstance("SHA-256")
        .digest(rootDir.canonicalPath.toByteArray())
        .joinToString("") { "%02x".format(it) }.take(12)
    val checkoutBuild = java.io.File(externalBuildBase, checkoutKey)
    allprojects {
        val projectFolder = if (path == ":") "root" else path.removePrefix(":").replace(':', '/')
        layout.buildDirectory.set(java.io.File(checkoutBuild, projectFolder))
    }
}
