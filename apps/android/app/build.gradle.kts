import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val hasFirebaseConfig = file("google-services.json").exists()
val releaseSigningFile = rootProject.file("keystore.properties")
val releaseSigning = Properties().apply {
    if (releaseSigningFile.exists()) releaseSigningFile.inputStream().use(::load)
}
val releaseVersionCode = providers.gradleProperty("ILLUMINED_VERSION_CODE").orNull?.toIntOrNull() ?: 1
val releaseVersionName = providers.gradleProperty("ILLUMINED_VERSION_NAME").orNull ?: "0.1.0"
if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.illumined.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.illumined.app"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        buildConfigField("boolean", "FIREBASE_CONFIGURED", hasFirebaseConfig.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningFile.exists()) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseSigning.getProperty("storeFile")) { "keystore.properties requires storeFile" })
                storePassword = requireNotNull(releaseSigning.getProperty("storePassword")) { "keystore.properties requires storePassword" }
                keyAlias = requireNotNull(releaseSigning.getProperty("keyAlias")) { "keystore.properties requires keyAlias" }
                keyPassword = requireNotNull(releaseSigning.getProperty("keyPassword")) { "keystore.properties requires keyPassword" }
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug").apply {
                storeFile = rootProject.file("work/debug.keystore")
            }
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    val firebaseBom = platform("com.google.firebase:firebase-bom:34.16.0")

    implementation(composeBom)
    implementation(firebaseBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
