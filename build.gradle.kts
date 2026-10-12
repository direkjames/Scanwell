// Top-level build file. The plugins are only declared here;
// they are switched on inside app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
