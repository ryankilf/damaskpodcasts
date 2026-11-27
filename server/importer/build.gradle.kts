// Weekly Podcast Index dump -> Meilisearch CLI (spec › Search › Weekly import). JVM only. May
// depend on :shared:api and nothing else from shared/ (enforced by Konsist in :architecture-tests).
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

application {
    mainClass.set("uk.co.kilfeder.damask.server.importer.MainKt")
}

dependencies {
    implementation(projects.shared.api)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.sqlite.jdbc)
    implementation(libs.kotlinx.coroutinesCore)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.testcontainers)
    testImplementation(libs.slf4j.simple)
}

tasks.test {
    systemProperty("damask.meili.image", "getmeili/meilisearch:v1.58.0")
}
