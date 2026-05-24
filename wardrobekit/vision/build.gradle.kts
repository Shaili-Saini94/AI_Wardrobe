plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.wardrobekit.vision"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {
    api(project(":wardrobekit:core"))
    implementation(libs.androidx.exifinterface)
    // TFLite (optional — provided by consumer app)
    compileOnly(libs.litertcore)
    compileOnly(libs.litertsupport)
    compileOnly(libs.litertgpu)
}
