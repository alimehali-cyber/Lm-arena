plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.zig.museum.tools.blackhole.MainKt")
}

dependencies {
    implementation(project(":core:model"))
}
