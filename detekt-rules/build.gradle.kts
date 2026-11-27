plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    // Provided by the Detekt runtime at analysis time.
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test)
    testImplementation(libs.kotlin.testJunit)
}
