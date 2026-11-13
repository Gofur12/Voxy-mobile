#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

if [ -x ./gradlew ]; then
  GRADLE="./gradlew"
elif command -v gradle >/dev/null 2>&1; then
  GRADLE="gradle"
else
  echo "ERROR: Gradle is unavailable. Install Gradle 8.6+ or add the Gradle wrapper (gradlew)." >&2
  exit 2
fi

GRADLE_VERSION="$($GRADLE --version | awk '/^Gradle / { print $2; exit }')"
GRADLE_MAJOR="$(printf '%s' "$GRADLE_VERSION" | cut -d. -f1)"
GRADLE_MINOR="$(printf '%s' "$GRADLE_VERSION" | cut -d. -f2)"
case "$GRADLE_MAJOR:$GRADLE_MINOR" in
  *[!0-9:]*|:*) echo "ERROR: Could not parse Gradle version: ${GRADLE_VERSION:-unknown}" >&2; exit 2 ;;
esac
if [ "$GRADLE_MAJOR" -lt 8 ] || { [ "$GRADLE_MAJOR" -eq 8 ] && [ "$GRADLE_MINOR" -lt 6 ]; }; then
  echo "ERROR: Gradle 8.6 or newer is required; found ${GRADLE_VERSION:-unknown}." >&2
  exit 2
fi

VERSION="$(sed -n "s/^version = '\([^']*\)'/\1/p" build.gradle | head -n 1)"
if [ -z "$VERSION" ]; then
  echo "ERROR: Could not read project version from build.gradle" >&2
  exit 2
fi

JAVA_SPEC="$($GRADLE --version 2>/dev/null | awk '/JVM:/ { print $2; exit }')"
echo "Build tool: Gradle $GRADLE_VERSION"
echo "Build JVM: ${JAVA_SPEC:-system Java}"
echo "Building Voxy Mobile $VERSION for Minecraft 1.20.1 / Fabric..."
"$GRADLE" --no-daemon clean build
ARCHIVE_BASE="$(awk -F"\047" '/rootProject.name/ { print $2; exit }' settings.gradle)"
JAR="build/libs/${ARCHIVE_BASE}-${VERSION}.jar"
if [ ! -f "$JAR" ]; then
  echo "ERROR: Expected remapped mod JAR was not produced: $JAR" >&2
  find build/libs -maxdepth 1 -type f -print 2>/dev/null || true
  exit 3
fi
"$GRADLE" --no-daemon verifyFabricJar
echo "BUILD SUCCESS: $JAR"
