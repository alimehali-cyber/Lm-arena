#!/usr/bin/env sh

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
elif [ -n "$GRADLE_HOME" ] && [ -x "$GRADLE_HOME/bin/gradle" ]; then
  exec "$GRADLE_HOME/bin/gradle" "$@"
else
  echo "Error: gradle executable not found in PATH or GRADLE_HOME" >&2
  exit 1
fi
