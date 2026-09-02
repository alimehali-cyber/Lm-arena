#!/usr/bin/env sh
set -e

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

GRADLE_DIR="$HOME/.gradle-installs/gradle-9.3.1"
if [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
  mkdir -p "$HOME/.gradle-installs"
  echo "Downloading Gradle 9.3.1..."
  if command -v curl >/dev/null 2>&1; then
    curl -sSL "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip" -o "$HOME/.gradle-installs/gradle-9.3.1-bin.zip"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip" -O "$HOME/.gradle-installs/gradle-9.3.1-bin.zip"
  fi
  unzip -q "$HOME/.gradle-installs/gradle-9.3.1-bin.zip" -d "$HOME/.gradle-installs"
fi

exec "$GRADLE_DIR/bin/gradle" "$@"
