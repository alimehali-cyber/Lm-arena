plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.zig.museum.tools.assetkit.MainKt")
}

dependencies {
    implementation(project(":core:model"))
}
