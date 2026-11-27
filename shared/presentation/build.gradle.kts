import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ViewModels, UiState, intents, navigation state. NO Compose dependency: this is the layer a
// future SwiftUI app consumes exactly as Compose does. Enforced by `verifyNoComposeDependency`
// (root build) and Konsist (:architecture-tests).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    jvm()
    android {
        namespace = "uk.co.kilfeder.damask.podcasts.presentation"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            // `implementation`, not `api`: :shared:ui must not see :shared:core types.
            implementation(projects.shared.core)
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
        jvmTest.dependencies {
            implementation(libs.sqldelight.sqliteDriver)
            implementation(libs.ktor.client.mock)
        }
    }
}
