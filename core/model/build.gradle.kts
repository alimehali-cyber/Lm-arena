plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
