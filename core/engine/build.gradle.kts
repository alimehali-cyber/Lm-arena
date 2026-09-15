plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zig.museum.core.engine"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Filament 1.71.5 per §3 pinned — added in M1
    implementation("com.google.android.filament:filament-android:1.71.5")
    implementation("com.google.android.filament:gltfio-android:1.71.5")
    implementation("com.google.android.filament:filament-utils-android:1.71.5")
    implementation("com.google.android.filament:filamat-android:1.71.5")

    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.kotlinx.coroutines.test)
}

// M4 / Foundational Rebuild Phase 0.3: matc offline compilation task per §5.7 T2.
//
// Prior behavior (kept only for the *local dev, no FILAMENT_MATC set* case): if matc could not be
// found anywhere, this task printed an informative message and no-op'd, silently leaving the
// runtime MaterialBuilder/GLSL-string path as the only material pipeline forever. That is the
// exact "temporary workaround becomes permanent" failure mode called out in
// docs/audit/MILESTONE_AUDIT.md (M1/M4) and the roadmap's own S2 condition.
//
// New behavior: when FILAMENT_MATC is explicitly set in the environment (this is what
// .github/workflows/build.yml's "Install matc" step does after a real download+extract+--help
// smoke test), this task treats matc as required and FAILS THE BUILD if the binary is missing or
// a compile invocation fails non-zero -- it does not silently fall back. Only when FILAMENT_MATC
// is unset (nobody asked for it -- e.g. a plain local `./gradlew build` without the CI env var) is
// the old "skip with instructions" behavior preserved, since matc's own upstream release archive
// is a many-hundred-MB download not reasonable to force on every local build by default.
val matcEnv = System.getenv("FILAMENT_MATC")
val matcRequired = matcEnv != null
val matcPath = matcEnv ?: "/usr/local/bin/matc"
val materialsDir = file("src/main/materials")
val filamatOutputDir = file("src/main/assets/filamat")

tasks.register("compileFilamat") {
    group = "museum"
    description = "Compile .mat materials to .filamat via matc per T2"
    doLast {
        if (!materialsDir.exists()) {
            println("No materials dir")
            return@doLast
        }
        filamatOutputDir.mkdirs()
        val matcFile = file(matcPath)
        val fallbackMatc = file("/tmp/filament/bin/matc")
        if (!matcFile.exists() && !fallbackMatc.exists()) {
            if (matcRequired) {
                throw org.gradle.api.GradleException(
                    "FILAMENT_MATC=$matcPath was set but no matc binary exists there (and /tmp/filament/bin/matc " +
                    "is also absent). Per the Foundational Rebuild roadmap's S2 condition, this is a hard stop: " +
                    "do not fall back to the runtime MaterialBuilder/GLSL-string path silently. Fix the matc " +
                    "install step instead of ignoring this failure."
                )
            }
            println("matc not found at $matcPath or /tmp/filament/bin/matc, skipping offline compilation — using runtime MaterialBuilder temporary per D-020, to be replaced in M4 with offline .filamat")
            println("To compile offline: FILAMENT_MATC=/path/to/matc ./gradlew :core:engine:compileFilamat")
            println("Or download filament release: gh release download v1.71.5 --pattern filament-v1.71.5-linux.tgz && tar xzf filament-v1.71.5-linux.tgz")
            return@doLast
        }
        val actualMatc = if (matcFile.exists()) matcFile.absolutePath else fallbackMatc.absolutePath

        // Foundational Rebuild Phase 1 scope note: the other 15 .mat files under materialsDir
        // (atmosphereShell, cloudDeck, gasGiantSurface, etc.) were authored per the original
        // roadmap milestones but -- per docs/audit/MILESTONE_AUDIT.md M1/M4 -- were never actually
        // run through matc before this pass, so it is unknown whether their GLSL bodies are even
        // valid. Fixing/validating those is explicitly Phase 2+ scope (materials), which this task
        // is forbidden from starting without owner go-ahead. So: compile everything found (useful
        // signal either way) and print every result, but only hard-fail the build over the one
        // material this phase's DoD actually requires, phase1MaterialNames below. Failures in the
        // other, not-yet-in-scope materials are reported clearly, not hidden, but are non-fatal
        // here so this task does not silently expand its own scope into Phase 2.
        val phase1MaterialNames = setOf("m1SurfaceLit")

        var phase1Failed = false
        val phase1Missing = phase1MaterialNames.toMutableSet()
        val matFiles = materialsDir.listFiles { f -> f.extension == "mat" } ?: emptyArray()
        if (matFiles.isEmpty()) {
            println("No .mat files found under $materialsDir")
        }
        matFiles.forEach { matFile ->
            val name = matFile.nameWithoutExtension
            val isPhase1 = name in phase1MaterialNames
            phase1Missing.remove(name)
            val outFile = File(filamatOutputDir, name + ".filamat")
            println("Compiling ${matFile.name} -> ${outFile.name} via $actualMatc" + if (isPhase1) " [Phase 1 material -- failure here is a hard stop]" else " [not yet in Phase 1 scope -- failure logged, non-fatal for this task]")
            val proc = ProcessBuilder(actualMatc, "-p", "mobile", "-a", "opengl", "-o", outFile.absolutePath, matFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText()
            val exit = proc.waitFor()
            println(output)
            if (exit != 0) {
                println("matc failed for ${matFile.name} with exit $exit")
                if (isPhase1) phase1Failed = true
            } else {
                println("Compiled ${outFile.name} ${outFile.length()} bytes")
            }
        }
        if (matcRequired && (phase1Failed || phase1Missing.isNotEmpty())) {
            throw org.gradle.api.GradleException(
                "Phase 1's required material(s) failed real matc compilation or were not found: " +
                "failed=$phase1Failed missing=$phase1Missing. Per the Foundational Rebuild roadmap's S2 " +
                "condition, this is a hard stop -- fix the material or the matc invocation, do not silently " +
                "keep the runtime MaterialBuilder path."
            )
        }
    }
}

tasks.matching { it.name == "mergeDebugAssets" || it.name == "mergeReleaseAssets" }.configureEach {
    dependsOn("compileFilamat")
}
