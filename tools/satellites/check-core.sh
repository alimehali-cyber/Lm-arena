#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/../.."
out=$(mktemp -d); trap 'rm -rf "$out"' EXIT
src=com/alijafari/red/astronomy/ui/satellites/core
kotlinc "app/src/main/java/$src" "app/src/test/java/$src/CoreContract.kt" -include-runtime -d "$out/check.jar"
java -cp "$out/check.jar" com.alijafari.red.astronomy.ui.satellites.core.CoreContract
