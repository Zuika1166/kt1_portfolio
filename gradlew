#!/bin/sh
set -eu

GRADLE_VERSION="8.14.3"
BASE_DIR="${HOME}/.gradle/ktor-task-api-bootstrap"
GRADLE_HOME="${BASE_DIR}/gradle-${GRADLE_VERSION}"
ZIP_FILE="${BASE_DIR}/gradle-${GRADLE_VERSION}-bin.zip"
URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

if [ ! -x "${GRADLE_HOME}/bin/gradle" ]; then
  mkdir -p "${BASE_DIR}"
  if [ ! -f "${ZIP_FILE}" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fL "${URL}" -o "${ZIP_FILE}"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "${ZIP_FILE}" "${URL}"
    else
      echo "Gradle is not installed and neither curl nor wget is available." >&2
      exit 1
    fi
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "${ZIP_FILE}" -d "${BASE_DIR}"
  else
    echo "unzip is required to bootstrap Gradle." >&2
    exit 1
  fi
fi

exec "${GRADLE_HOME}/bin/gradle" "$@"
