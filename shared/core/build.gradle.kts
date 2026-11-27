import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Feeds, DB, sync, rotation logic. NO UI dependencies (no Compose, no lifecycle).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.sqldelight)
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
    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.xmlutil.core)
            implementation(libs.okio)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.androidDriver)
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqliteDriver)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.nativeDriver)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutinesTest)
        }
        getByName("androidHostTest").dependencies {
            // Host tests run on the JVM, so they use the JDBC SQLite driver.
            implementation(libs.sqldelight.sqliteDriver)
        }
    }
}

sqldelight {
    databases {
        create("DamaskDatabase") {
            packageName.set("uk.co.kilfeder.damask.podcasts.core.db")
            // Android 12 (minSdk 31) ships SQLite 3.32; don't use newer syntax.
            dialect(libs.sqldelight.dialect)
            // Every released schema is snapshotted here and migrations are verified against it.
            // After changing the schema: add migrations/<n>.sqm, then run
            // `./gradlew :shared:core:generateCommonMainDamaskDatabaseSchema` on its own.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
