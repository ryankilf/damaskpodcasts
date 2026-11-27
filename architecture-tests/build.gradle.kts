plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.testJunit)
}

tasks.test {
    // Konsist reads source files directly, so declare them as inputs or Gradle will treat the
    // test task as up-to-date / cached after an architecture-breaking change.
    inputs.files(
        rootProject.fileTree("shared") { include("**/src/**/*.kt"); exclude("**/build/**") },
        rootProject.fileTree("androidApp/src") { include("**/*.kt"); exclude("**/build/**") },
        rootProject.fileTree("desktopApp/src") { include("**/*.kt"); exclude("**/build/**") },
        rootProject.fileTree("server") { include("**/src/**/*.kt", "*/build.gradle.kts"); exclude("**/build/**") },
    ).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("architectureSources")
}
