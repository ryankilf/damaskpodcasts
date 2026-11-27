import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Compose Multiplatform screens. Depends on :shared:presentation only, never :shared:core.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        // v1 iOS app hosts Compose. When the SwiftUI layer arrives it gets its own framework that
        // exports :shared:presentation and NOT this module.
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(projects.shared.presentation)
        }
    }
    jvm()
    android {
        namespace = "uk.co.kilfeder.damask.podcasts.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            api(projects.shared.presentation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.backhandler)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.compose.uiTest)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testRunner)
        }
    }
}

compose.resources {
    packageOfResClass = "uk.co.kilfeder.damask.podcasts.ui.resources"
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// Opt-in screenshot tour of the real app: -Pdamask.screenshots=<dir> [-Pdamask.opml=<file>].
val tourDir = providers.gradleProperty("damask.screenshots").map { rootProject.file(it).absolutePath }
val tourOpml = providers.gradleProperty("damask.opml").map { rootProject.file(it).absolutePath }
tasks.withType<Test>().configureEach {
    tourDir.orNull?.let { systemProperty("damask.screenshots", it) }
    tourOpml.orNull?.let { systemProperty("damask.opml", it) }
}
