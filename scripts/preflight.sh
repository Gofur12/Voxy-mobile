#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"
if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java is not installed. Install a JDK 17 or newer." >&2
  exit 2
fi
JAVA_VERSION="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
case "$JAVA_VERSION" in
  ''|*[!0-9]*) echo "ERROR: Could not parse Java runtime version." >&2; exit 2 ;;
esac
if [ "$JAVA_VERSION" -lt 17 ]; then
  echo "ERROR: Java 17 or newer is required; found Java $JAVA_VERSION." >&2
  exit 2
fi
echo "PASS: Java $JAVA_VERSION detected."
if [ -x ./gradlew ]; then
  echo "Gradle source: ./gradlew"
elif command -v gradle >/dev/null 2>&1; then
  echo "Gradle source: $(command -v gradle)"
else
  echo "BLOCKED: Gradle is not installed and no executable ./gradlew exists."
  echo "Install Gradle 8.6+ or provide the Gradle Wrapper distribution and wrapper JAR."
  exit 3
fi
