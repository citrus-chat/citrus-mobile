// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("dev.detekt") version "2.0.0-alpha.2"
}

subprojects {
    // 1. Apply the plugin
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // 2. Configure using the string name to avoid unresolved reference errors
    val ktlintExt = extensions.getByName("ktlint")
    (ktlintExt as org.gradle.api.plugins.ExtensionAware).withGroovyBuilder {
        setProperty("android", true)
        setProperty("verbose", true)
    }
}
