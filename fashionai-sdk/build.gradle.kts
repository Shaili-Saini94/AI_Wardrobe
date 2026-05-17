import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.fashionai.sdk"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        buildConfig = true
        mlModelBinding = true
    }
}

dependencies {
    // LiteRT — on-device AI inference
    implementation(libs.litertcore)
    implementation(libs.litertsupport)
    implementation(libs.litertgpu)

    // MediaPipe — image classification + segmentation
    implementation(libs.mediapipe.tasks.vision)

    // ML Kit — subject segmentation (background removal)
    implementation(libs.play.services.mlkit.subject.segmentation)

    // Palette — color extraction
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation("androidx.annotation:annotation:1.9.1")

    // Testing
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Publishing config — run ./gradlew :fashionai-sdk:publishToMavenLocal
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "io.closetiq"
                artifactId = "fashionai-sdk"
                version = "1.0.0"
            }
        }
    }
}
