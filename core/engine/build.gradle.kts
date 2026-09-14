plugins {
    id("com.android.library") version "9.1.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
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
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.kotlinx.coroutines.test)
}

// M4: matc offline compilation task per §5.7 T2
// All materials authored offline with Filament's material compiler (matc) into .filamat files and shipped precompiled
// No runtime shader compilation, no GLSL strings inside Kotlin per T2
// This task compiles .mat sources in src/main/materials/ to .filamat in src/main/assets/filamat/ if matc binary available
// If matc not available (sandbox offline), uses filamat-android runtime as temporary fallback per D-020, to be replaced offline in M4

val matcPath = System.getenv("FILAMENT_MATC") ?: "/usr/local/bin/matc"
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
        if (!matcFile.exists() && !file("/tmp/filament/bin/matc").exists()) {
            println("matc not found at $matcPath or /tmp/filament/bin/matc, skipping offline compilation — using runtime MaterialBuilder temporary per D-020, to be replaced in M4 with offline .filamat")
            println("To compile offline: FILAMENT_MATC=/path/to/matc ./gradlew :core:engine:compileFilamat")
            println("Or download filament release: gh release download v1.71.5 --pattern filament-v1.71.5-linux.tgz && tar xzf filament-v1.71.5-linux.tgz")
            return@doLast
        }
        val actualMatc = if (matcFile.exists()) matcFile.absolutePath else "/tmp/filament/bin/matc"
        materialsDir.listFiles { f -> f.extension == "mat" }?.forEach { matFile ->
            val outFile = File(filamatOutputDir, matFile.nameWithoutExtension + ".filamat")
            println("Compiling ${matFile.name} -> ${outFile.name} via $actualMatc")
            val result = exec {
                commandLine(actualMatc, "-p", "mobile", "-a", "opengl", "-o", outFile.absolutePath, matFile.absolutePath)
                isIgnoreExitValue = true
            }
            if (result.exitValue != 0) {
                println("matc failed for ${matFile.name} with exit ${result.exitValue}, keeping runtime fallback")
            } else {
                println("Compiled ${outFile.name} ${outFile.length()} bytes")
            }
        }
    }
}

// Ensure filamat compilation runs before assets packaging if matc available
tasks.matching { it.name == "mergeDebugAssets" || it.name == "mergeReleaseAssets" }.configureEach {
    dependsOn("compileFilamat")
}
