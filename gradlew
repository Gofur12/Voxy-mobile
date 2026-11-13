#!/usr/bin/env sh
# Lightweight, checksum-pinned Gradle bootstrap for environments without an installed Gradle.
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
VERSION=8.6
SHA256=9631d53cf3e74bfa726893aee1f8994fee4e060c401335946dba2156f440f24c
BASE="${GRADLE_USER_HOME:-${HOME}/.gradle}/wrapper/dists/gradle-${VERSION}-bin/voxymobile-bootstrap"
GRADLE="$BASE/gradle-${VERSION}/bin/gradle"
if [ ! -x "$GRADLE" ]; then
  if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java 17+ is required before Gradle can start." >&2
    exit 2
  fi
  mkdir -p "$BASE"
  ARCHIVE="$BASE/gradle-${VERSION}-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip"
  if [ -f "$ARCHIVE" ]; then
    if command -v sha256sum >/dev/null 2>&1; then
      CACHED="$(sha256sum "$ARCHIVE" | awk '{print $1}')"
    elif command -v shasum >/dev/null 2>&1; then
      CACHED="$(shasum -a 256 "$ARCHIVE" | awk '{print $1}')"
    else
      echo "ERROR: sha256sum or shasum is required to verify Gradle." >&2
      exit 2
    fi
    if [ "$CACHED" != "$SHA256" ]; then rm -f "$ARCHIVE"; fi
  fi
  if [ ! -f "$ARCHIVE" ]; then
    TEMP="$ARCHIVE.part"
    rm -f "$TEMP"
    echo "Downloading Gradle ${VERSION} from the official distribution service..."
    if command -v curl >/dev/null 2>&1; then
      if ! curl -fL --retry 2 --connect-timeout 15 "$URL" -o "$TEMP"; then rm -f "$TEMP"; exit 2; fi
    elif command -v wget >/dev/null 2>&1; then
      if ! wget -O "$TEMP" "$URL"; then rm -f "$TEMP"; exit 2; fi
    else
      echo "ERROR: Install curl or wget to bootstrap Gradle." >&2
      exit 2
    fi
    mv "$TEMP" "$ARCHIVE"
  fi
  if command -v sha256sum >/dev/null 2>&1; then
    ACTUAL="$(sha256sum "$ARCHIVE" | awk '{print $1}')"
  elif command -v shasum >/dev/null 2>&1; then
    ACTUAL="$(shasum -a 256 "$ARCHIVE" | awk '{print $1}')"
  else
    echo "ERROR: sha256sum or shasum is required to verify Gradle." >&2
    exit 2
  fi
  if [ "$ACTUAL" != "$SHA256" ]; then
    rm -f "$ARCHIVE"
    echo "ERROR: Gradle ZIP checksum mismatch; refusing to execute it." >&2
    exit 3
  fi
  command -v unzip >/dev/null 2>&1 || { echo "ERROR: unzip is required to extract Gradle." >&2; exit 2; }
  unzip -q -o "$ARCHIVE" -d "$BASE"
  rm -f "$ARCHIVE"
fi
if [ ! -x "$GRADLE" ]; then
  echo "ERROR: Gradle bootstrap extraction did not produce $GRADLE" >&2
  exit 3
fi
exec "$GRADLE" "$@"
