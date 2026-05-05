plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
    id("dev.detekt") version "2.0.0-alpha.2" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.5" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
}
