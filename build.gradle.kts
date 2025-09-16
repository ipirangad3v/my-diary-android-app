// Top-level build file where you can add configuration options common to all sub-projects/modules.
val appVersionCode = project.findProperty("APP_VERSION_CODE") ?: "1"
val appVersionName = project.findProperty("APP_VERSION_NAME") ?: "1.0.0"

extra.set("APP_VERSION_CODE", appVersionCode)
extra.set("APP_VERSION_NAME", appVersionName)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias (libs.plugins.hilt.android) apply false
    alias (libs.plugins.kotlin.ksp) apply false
    alias (libs.plugins.spotless) apply false
    alias (libs.plugins.crashlytics) apply false
}
