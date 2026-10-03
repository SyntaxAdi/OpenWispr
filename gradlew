#!/bin/sh
# Simple wrapper - downloads Gradle if needed, otherwise uses local gradle
set -e
GRADLE_VERSION=8.6
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
if [ -f "./gradle/wrapper/gradle-wrapper.jar" ]; then exec java -jar ./gradle/wrapper/gradle-wrapper.jar "$@"; fi
echo "Gradle not found. Install Gradle 8.6 or Android Studio, or run:"
echo "  wget $GRADLE_URL && unzip gradle-${GRADLE_VERSION}-bin.zip"
