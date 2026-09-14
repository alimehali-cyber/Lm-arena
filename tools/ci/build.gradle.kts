plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
}

tasks.register<Exec>("checkProvenanceValid") {
    group = "museum"
    description = "Provenance gate — valid fixture must pass per §16.3"
    commandLine("python3", "tools/ci/check_provenance.py", "--test-fixtures", "tools/ci/fixtures/provenance/valid")
}

tasks.register<Exec>("checkProvenanceBroken") {
    group = "museum"
    description = "Provenance gate — broken fixture must fail per §16.3"
    // This task is expected to fail, so we handle it in CI workflow with manual check
    commandLine("python3", "tools/ci/check_provenance.py", "--test-fixtures", "tools/ci/fixtures/provenance/broken")
}

tasks.register<Exec>("checkProvenance") {
    group = "museum"
    description = "Provenance gate — real manifests per §16.3"
    commandLine("python3", "tools/ci/check_provenance.py", "--manifests-dir", "manifests")
}

tasks.register<Exec>("checkScope") {
    group = "museum"
    description = "Scope gate per §16.4"
    commandLine("python3", "tools/ci/check_scope.py", "--base", "origin/Obra-with-key")
}

tasks.register("museumGates") {
    group = "museum"
    description = "Run all museum gates (provenance valid + real + scope)"
    dependsOn("checkProvenanceValid", "checkProvenance", "checkScope")
}
