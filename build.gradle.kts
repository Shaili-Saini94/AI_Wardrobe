// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
}

subprojects {
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.tensorflow:tensorflow-lite")).using(module("com.google.ai.edge.litert:litert:${libs.versions.litert.get()}"))
            substitute(module("org.tensorflow:tensorflow-lite-api")).using(module("com.google.ai.edge.litert:litert-api:${libs.versions.litert.get()}"))
            substitute(module("org.tensorflow:tensorflow-lite-gpu")).using(module("com.google.ai.edge.litert:litert-gpu:${libs.versions.litert.get()}"))
            substitute(module("org.tensorflow:tensorflow-lite-support")).using(module("com.google.ai.edge.litert:litert-support:${libs.versions.litert.get()}"))
        }
    }
}
