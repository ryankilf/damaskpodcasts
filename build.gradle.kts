import app.cash.licensee.LicenseeExtension
import dev.detekt.gradle.extensions.DetektExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.licensee) apply false
    alias(libs.plugins.sqldelight) apply false
}

// ---------------------------------------------------------------------------------------------
// Static analysis: detekt + Compose rules + project accessibility rules (:detekt-rules).
// `./gradlew detekt` per module; also wired into `./gradlew check`.
// ---------------------------------------------------------------------------------------------
val composeRules = libs.composeRules.detekt

subprojects {
    // `:shared` is only a directory container for core/presentation/ui.
    if (path == ":shared") return@subprojects

    apply(plugin = "dev.detekt")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        if (path == ":detekt-rules") {
            config.from(rootProject.file("config/detekt/detekt-rules-module.yml"))
        }
        // Analyse every source set (commonMain, androidMain, iosMain, tests...), not just src/main.
        source.setFrom(files("src"))
        basePath.set(rootProject.layout.projectDirectory)
        parallel.set(true)
    }

    dependencies {
        add("detektPlugins", composeRules)
        if (path != ":detekt-rules") {
            add("detektPlugins", project(":detekt-rules"))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Layering guard: core and presentation must never (even transitively) pull in Compose.
// This is what makes presentation consumable unchanged by a future SwiftUI layer.
// ---------------------------------------------------------------------------------------------
val composeFreeModules = listOf(":shared:core", ":shared:presentation")
val forbiddenGroups = listOf("org.jetbrains.compose", "androidx.compose")

composeFreeModules.forEach { modulePath ->
    project(modulePath) {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val verify = tasks.register("verifyNoComposeDependency") {
                group = "verification"
                description = "Fails if $modulePath depends on Compose, directly or transitively."
                val resolved = configurations.named("jvmCompileClasspath").flatMap { classpath ->
                    classpath.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                        artifacts.map { it.id.componentIdentifier.displayName }
                    }
                }
                // Copy into task-local values: doLast must not capture build-script objects
                // (configuration cache).
                val groups = forbiddenGroups.toList()
                val label = modulePath
                inputs.property("resolvedComponents", resolved)
                inputs.property("forbiddenGroups", groups)
                doLast {
                    val offenders = resolved.get().filter { id ->
                        groups.any { id.startsWith("$it:") || id.startsWith("$it.") }
                    }
                    if (offenders.isNotEmpty()) {
                        throw GradleException(
                            "$label must not depend on Compose, but resolves:\n  " + offenders.joinToString("\n  "),
                        )
                    }
                }
            }
            tasks.named("check") { dependsOn(verify) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Licences (spec: permissive or LGPL only, nothing GPL). Licensee fails the build on any licence
// not allowed below, so a GPL transitive dependency can't slip in. Applied to the modules whose
// runtime classpaths cover everything that ships: both apps plus the iOS framework module.
// When adding FFmpeg/JavaCV, allow LGPL-2.1-or-later deliberately, in a reviewed change.
// ---------------------------------------------------------------------------------------------
val shippingModules = listOf(":androidApp", ":desktopApp", ":shared:ui")

// Banned by coordinates or file name regardless of declared licence (spec › Licences). Matched
// against "group:name:version (file.jar)" so classifier-only GPL builds are caught too.
val bannedArtifactPatterns = listOf(
    """org\.bytedeco:[^ ]*-gpl""", // e.g. ffmpeg-platform-gpl
    """\(.*-gpl\.jar\)""", // e.g. ffmpeg:7.1-1.5.11:macosx-arm64-gpl classifier
    """org\.bytedeco:javacv-platform:""", // pulls every native for every OS
    """uk\.co\.caprica:vlcj""", // GPL; rejected in the spec
)
val shippingClasspaths = mapOf(
    ":androidApp" to listOf("debugRuntimeClasspath", "releaseRuntimeClasspath"),
    ":desktopApp" to listOf("runtimeClasspath"),
    ":shared:ui" to listOf("jvmRuntimeClasspath"),
)

shippingModules.forEach { modulePath ->
    project(modulePath) {
        apply(plugin = "app.cash.licensee")
        extensions.configure<LicenseeExtension> {
            allow("Apache-2.0")
            allow("MIT")
            allow("BSD-2-Clause")
            allow("BSD-3-Clause")
            allow("ISC")
            allow("CC0-1.0")
            // SLF4J (via Ktor) declares MIT using opensource.org's newer URL, which Licensee
            // doesn't map to an SPDX ID yet.
            allowUrl("https://opensource.org/license/mit") { because("MIT (SLF4J)") }
            // FileKit (OPML file picker) and dbus-java (FileKit's Linux desktop portal) link to their
            // LICENSE files; both read and confirmed MIT, 27 Nov 2026.
            allowUrl("https://github.com/vinceglb/FileKit/blob/main/LICENSE") { because("MIT (FileKit)") }
            allowUrl("https://github.com/hypfvieh/dbus-java/blob/master/LICENSE") { because("MIT (dbus-java)") }
        }

        val verifyBanned = tasks.register("verifyNoBannedArtifacts") {
            group = "verification"
            description = "Fails if $modulePath ships an artifact the spec bans (GPL FFmpeg, vlcj, ...)."
            // Android classpaths have several artifact variants per library (aar, jar, art profile),
            // so walk the resolved component graph there; JVM classpaths also check jar file names,
            // which is where bytedeco's GPL classifiers show up (desktop is where FFmpeg lives).
            val isAndroid = modulePath == ":androidApp"
            val resolved = shippingClasspaths.getValue(modulePath).map { configName ->
                configurations.named(configName).flatMap { classpath ->
                    if (isAndroid) {
                        classpath.incoming.resolutionResult.rootComponent.map { root ->
                            val seen = mutableSetOf<String>()
                            val queue = ArrayDeque(listOf(root))
                            while (queue.isNotEmpty()) {
                                val component = queue.removeFirst()
                                if (!seen.add(component.id.displayName)) continue
                                component.dependencies
                                    .filterIsInstance<org.gradle.api.artifacts.result.ResolvedDependencyResult>()
                                    .forEach { queue.add(it.selected) }
                            }
                            seen.toList()
                        }
                    } else {
                        classpath.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                            artifacts.map { "${it.id.componentIdentifier.displayName} (${it.file.name})" }
                        }
                    }
                }
            }.reduce { acc, next -> acc.zip(next) { a, b -> a + b } }
            val patterns = bannedArtifactPatterns.toList()
            val label = modulePath
            inputs.property("resolvedArtifacts", resolved)
            inputs.property("bannedPatterns", patterns)
            doLast {
                val regexes = patterns.map(::Regex)
                val offenders = resolved.get().distinct().filter { id -> regexes.any { it.containsMatchIn(id) } }
                if (offenders.isNotEmpty()) {
                    throw GradleException(
                        "$label ships artifacts banned by the spec's licence rules:\n  " +
                            offenders.joinToString("\n  "),
                    )
                }
            }
        }
        tasks.matching { it.name == "check" }.configureEach { dependsOn(verifyBanned) }
    }
}
