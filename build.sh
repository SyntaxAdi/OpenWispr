#!/usr/bin/env bash
# build.sh — one-command APK builder for OpenWispr
# Usage: bash build.sh [debug|release]  (default: debug)
#  - Auto-detects JDK 17 / ANDROID_HOME, handles wireless + USB adb
#  - Copies APK to project root as OpenWispr-<version>-<buildType>-<date>.apk + latest symlink
set -euo pipefail

BUILD_TYPE="${1:-debug}"
if [[ "$BUILD_TYPE" != "debug" && "$BUILD_TYPE" != "release" ]]; then
  echo "Usage: bash build.sh [debug|release]"
  exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# --- Resolve JDK 17 (keep system JDK 26 default) ---
CANDIDATES=(
  "/usr/lib/jvm/java-17-openjdk"
  "/usr/lib/jvm/java-21-openjdk"
  "/opt/jdk-17"
  "/opt/jdk17"
)
JDK_HOME=""
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  ver="$("$JAVA_HOME/bin/java" -version 2>&1 | head -n1)"
  if echo "$ver" | grep -qE '"(17|21)\.'; then JDK_HOME="$JAVA_HOME"; fi
fi
if [[ -z "$JDK_HOME" ]]; then
  for c in "${CANDIDATES[@]}"; do
    if [[ -x "$c/bin/java" ]]; then
      ver="$("$c/bin/java" -version 2>&1 | head -n1)"
      if echo "$ver" | grep -qE '"(17|21)\.'; then JDK_HOME="$c"; break; fi
    fi
  done
fi
if [[ -z "$JDK_HOME" ]]; then
  echo "ERROR: JDK 17/21 not found. Install it: sudo pacman -S jdk17-openjdk"
  echo "Tried: ${CANDIDATES[*]} and \$JAVA_HOME=${JAVA_HOME:-<unset>}"
  exit 1
fi
export JAVA_HOME="$JDK_HOME"
echo "→ Using JAVA_HOME=$JAVA_HOME ($("$JAVA_HOME/bin/java" -version 2>&1 | head -n1))"

# --- Resolve Android SDK ---
if [[ ! -f "local.properties" ]]; then
  for sdk in "$HOME/Android/Sdk" "$HOME/Android/Sdk" "/opt/android-sdk" "$ANDROID_HOME" "$ANDROID_SDK_ROOT"; do
    if [[ -n "${sdk:-}" && -d "$sdk/platforms" ]]; then
      echo "sdk.dir=$sdk" > local.properties
      echo "→ Created local.properties (sdk.dir=$sdk)"
      break
    fi
  done
fi
if [[ ! -f "local.properties" ]]; then
  echo "ERROR: local.properties missing and SDK not found."
  echo 'Create it: echo "sdk.dir=/home/akeno/Android/Sdk" > local.properties'
  exit 1
fi
echo "→ $(cat local.properties)"

# --- Fix broken gradlew shim (from 9-line to official 252-line wrapper) ---
if [[ $(wc -l < gradlew 2>/dev/null || echo 0) -lt 50 ]]; then
  echo "→ Fixing broken gradlew shim..."
  if command -v curl >/dev/null 2>&1; then
    curl -sL https://raw.githubusercontent.com/gradle/gradle/v8.11.0/gradlew -o gradlew
    chmod +x gradlew
    echo "→ gradlew replaced with official Gradle 8.11 wrapper"
  else
    echo "ERROR: gradlew is broken and curl not found to fix it"
    exit 1
  fi
fi

# --- Build ---
echo ""
echo "━━━ Building $BUILD_TYPE APK ━━━"
./gradlew "assemble${BUILD_TYPE^}" --console=plain

# --- Locate + copy APK to project root ---
SRC_APK=""
if [[ "$BUILD_TYPE" == "debug" ]]; then
  SRC_APK="app/build/outputs/apk/debug/app-debug.apk"
else
  # release may be unsigned or signed depending on signingConfig
  SRC_APK=$(ls -t app/build/outputs/apk/release/*.apk 2>/dev/null | head -n1 || true)
fi
if [[ -z "${SRC_APK:-}" || ! -f "$SRC_APK" ]]; then
  echo "ERROR: APK not found after build (expected $SRC_APK)"
  ls -R app/build/outputs/apk 2>&1 | head -n 30 || true
  exit 1
fi

VERSION="$(grep -E 'versionName\s*=' app/build.gradle.kts | head -n1 | sed -E 's/.*"([^"]+)".*/\1/')"
VERSION="${VERSION:-1.0.0}"
DATE="$(date +%Y%m%d)"
DEST="OpenWispr-${VERSION}-${BUILD_TYPE}-${DATE}.apk"
cp -f "$SRC_APK" "./$DEST"
LATEST="OpenWispr-latest-${BUILD_TYPE}.apk"
ln -sf "$DEST" "./$LATEST"
SIZE="$(du -h "./$DEST" | cut -f1)"
echo ""
echo "✓ APK built: $SRC_APK ($SIZE)"
echo "✓ Copied to: $SCRIPT_DIR/$DEST"
echo "✓ Latest symlink: $SCRIPT_DIR/$LATEST -> $DEST"
echo ""
ls -lh "./$DEST" "./$LATEST"
echo ""
echo "Install with:"
echo "  adb install -r ./$DEST"
echo "  # or: adb -s <device> install -r ./$DEST  (for wireless: adb devices to list)"
