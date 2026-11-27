// Server-only: the Meilisearch HTTP client, the index's document shape and settings. Shared by
// server/search and server/importer so the two can't drift apart. Not part of the app.
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    `java-test-fixtures`
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    api(libs.ktor.client.core)
    api(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.json)

    // A throwaway Meilisearch container for integration tests in this and dependent modules.
    testFixturesApi(libs.testcontainers)
    testFixturesApi(libs.kotlin.testJunit)

    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.slf4j.simple)
}

tasks.test {
    systemProperty("damask.meili.image", "getmeili/meilisearch:${libs.versions.meilisearch.get()}")
}
