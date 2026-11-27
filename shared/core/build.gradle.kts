import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Feeds, DB, sync, rotation logic. NO UI dependencies (no Compose, no lifecycle).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    jvm()
    android {
        namespace = "uk.co.kilfeder.damask.podcasts.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutinesCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
