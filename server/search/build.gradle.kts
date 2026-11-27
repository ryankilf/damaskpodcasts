// The public search gateway (spec › Search). JVM only. May depend on :shared:api and nothing else
// from shared/ (enforced by Konsist in :architecture-tests).
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
    mainClass.set("uk.co.kilfeder.damask.server.search.MainKt")
}

dependencies {
    implementation(projects.shared.api)
    implementation(projects.server.meili)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.authJwt)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.rateLimit)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(testFixtures(projects.server.meili))
    testImplementation(libs.slf4j.simple)
}

tasks.test {
    // The Meilisearch integration tests need Docker; they're skipped (not failed) without it.
    systemProperty("damask.meili.image", "getmeili/meilisearch:v1.58.0")
}
