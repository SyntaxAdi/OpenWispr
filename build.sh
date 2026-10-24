#!/usr/bin/env bash
# build.sh — interactive & one-command APK builder for OpenWispr
# Options:
#   1) Build release APK
#   2) Build debug APK
#   3) Build debug APK + install via ADB + stream logs
#   4) Exit
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

resolve_jdk() {
  local candidates=(
    "/usr/lib/jvm/java-17-openjdk"
    "/usr/lib/jvm/java-21-openjdk"
    "/opt/jdk-17"
    "/opt/jdk17"
  )
  local jdk_home=""
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    local ver
    ver="$("$JAVA_HOME/bin/java" -version 2>&1 | head -n1)"
    if echo "$ver" | grep -qE '"(17|21)\.'; then jdk_home="$JAVA_HOME"; fi
  fi
  if [[ -z "$jdk_home" ]]; then
    for c in "${candidates[@]}"; do
      if [[ -x "$c/bin/java" ]]; then
        local ver
        ver="$("$c/bin/java" -version 2>&1 | head -n1)"
        if echo "$ver" | grep -qE '"(17|21)\.'; then jdk_home="$c"; break; fi
      fi
    done
  fi
  if [[ -z "$jdk_home" ]]; then
    echo "ERROR: JDK 17/21 not found. Install it: sudo pacman -S jdk17-openjdk"
    echo "Tried: ${candidates[*]} and \$JAVA_HOME=${JAVA_HOME:-<unset>}"
    exit 1
  fi
  export JAVA_HOME="$jdk_home"
  echo "→ Using JAVA_HOME=$JAVA_HOME ($("$JAVA_HOME/bin/java" -version 2>&1 | head -n1))"
}

resolve_sdk() {
  if [[ ! -f "local.properties" ]]; then
    for sdk in "$HOME/Android/Sdk" "/opt/android-sdk" "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
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
}

ensure_gradlew() {
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
}

build_apk() {
  local build_type="$1"
  resolve_jdk
  resolve_sdk
  ensure_gradlew

  echo ""
  echo "━━━ Building ${build_type^} APK ━━━"
  ./gradlew "assemble${build_type^}" --console=plain

  local src_apk=""
  if [[ "$build_type" == "debug" ]]; then
    src_apk="app/build/outputs/apk/debug/app-debug.apk"
  else
    src_apk=$(ls -t app/build/outputs/apk/release/*.apk 2>/dev/null | head -n1 || true)
  fi

  if [[ -z "${src_apk:-}" || ! -f "$src_apk" ]]; then
    echo "ERROR: APK not found after build (expected $src_apk)"
    ls -R app/build/outputs/apk 2>&1 | head -n 30 || true
    exit 1
  fi

  local git_count
  git_count=$(git rev-list --count HEAD 2>/dev/null || echo 1)
  local major=$((git_count / 100))
  local minor=$(((git_count / 10) % 10))
  local patch=$((git_count % 10))
  local version="${major}.${minor}.${patch}"
  local date_str
  date_str="$(date +%Y%m%d)"
  local dest="OpenWispr-${version}-${build_type}-${date_str}.apk"

  echo "→ Removing previous APKs (rm -rf *.apk)..."
  rm -rf ./*.apk

  cp -f "$src_apk" "./$dest"
  local latest="OpenWispr-latest-${build_type}.apk"
  ln -sf "$dest" "./$latest"
  local size
  size="$(du -h "./$dest" | cut -f1)"

  echo ""
  echo "✓ APK built: $src_apk ($size)"
  echo "✓ Copied to: $SCRIPT_DIR/$dest"
  echo "✓ Latest symlink: $SCRIPT_DIR/$latest -> $dest"
  echo ""
  ls -lh "./$dest" "./$latest"
  echo ""

  # Export result path for downstream callers
  BUILT_APK_PATH="./$dest"
}

take_screenshot() {
  local n=1
  while [[ -f "$SCRIPT_DIR/${n}.png" ]]; do
    ((n++))
  done
  local target_file="$SCRIPT_DIR/${n}.png"

  echo ""
  echo "📸 Taking screenshot..."
  if adb -s "$TARGET_DEVICE" exec-out screencap -p > "$target_file" 2>/dev/null && [[ -s "$target_file" ]]; then
    echo "✓ Screenshot saved: ${n}.png ($target_file)"
  else
    rm -f "$target_file" 2>/dev/null || true
    echo "✗ Screenshot capture failed. Ensure device is unlocked and authorized."
  fi
}

install_and_stream_logs() {
  local apk_path="$1"
  if ! command -v adb >/dev/null 2>&1; then
    echo "ERROR: adb command not found on PATH."
    exit 1
  fi

  echo "━━━ Connecting to Android Device ━━━"
  while true; do
    local devices
    mapfile -t devices < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
    if [[ ${#devices[@]} -gt 0 ]]; then
      TARGET_DEVICE="${devices[0]}"
      echo "✓ Device detected: $TARGET_DEVICE"
      if [[ ${#devices[@]} -gt 1 ]]; then
        echo "  (Multiple devices detected: ${devices[*]}. Using first: $TARGET_DEVICE)"
      fi
      break
    fi
    echo "⚠ No active ADB devices detected."
    echo "  Please plug in your device via USB or connect via wireless ADB (adb connect <ip>:<port>)."
    read -r -p "Press [Enter] to scan again, or Ctrl+C to abort... " _
  done

  echo ""
  echo "━━━ Installing APK via ADB ━━━"
  adb -s "$TARGET_DEVICE" install -r "$apk_path"

  echo ""
  echo "━━━ Launching OpenWispr ━━━"
  adb -s "$TARGET_DEVICE" shell am start -n com.openwispr/.MainActivity

  local log_file="$SCRIPT_DIR/openwispr-logs.txt"
  echo ""
  echo "━━━ Streaming Logcat ━━━"
  echo "Logs will be saved to: $log_file"

  adb -s "$TARGET_DEVICE" logcat -c || true

  local pid=""
  for _ in {1..8}; do
    pid="$(adb -s "$TARGET_DEVICE" shell pidof -s com.openwispr 2>/dev/null || true)"
    if [[ -n "$pid" ]]; then break; fi
    sleep 0.25
  done

  echo "Streaming logs for com.openwispr (PID: ${pid:-detecting})."
  echo "Controls: Press 's' to take screenshot, 'q' or Ctrl+C to exit."
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  echo "=== OpenWispr Logcat Session: $(date '+%Y-%m-%d %H:%M:%S') (PID: ${pid:-detecting}) ===" > "$log_file"

  local adb_pid=""
  local tail_pid=""

  cleanup_streaming() {
    trap - INT TERM EXIT
    if [[ -n "$adb_pid" ]] && kill -0 "$adb_pid" 2>/dev/null; then
      kill "$adb_pid" 2>/dev/null || true
      wait "$adb_pid" 2>/dev/null || true
    fi
    if [[ -n "$tail_pid" ]] && kill -0 "$tail_pid" 2>/dev/null; then
      kill "$tail_pid" 2>/dev/null || true
      wait "$tail_pid" 2>/dev/null || true
    fi
    echo -e "\n→ Logcat streaming stopped. Saved to: $log_file"
    exit 0
  }
  trap cleanup_streaming INT TERM EXIT

  if [[ -n "$pid" ]]; then
    adb -s "$TARGET_DEVICE" logcat -v threadtime --pid="$pid" >> "$log_file" 2>&1 &
  else
    adb -s "$TARGET_DEVICE" logcat -v threadtime -e "com.openwispr|OpenWispr|AndroidRuntime|FATAL" >> "$log_file" 2>&1 &
  fi
  adb_pid=$!

  tail -n +1 -f "$log_file" &
  tail_pid=$!

  if [[ -t 0 ]]; then
    while true; do
      if read -r -s -n 1 key; then
        case "$key" in
          s|S)
            take_screenshot
            ;;
          q|Q)
            cleanup_streaming
            ;;
        esac
      else
        break
      fi
    done
  else
    wait "$adb_pid" 2>/dev/null || true
  fi
}

show_menu() {
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "          OpenWispr Build Console               "
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  1) Build release APK"
  echo "  2) Build debug APK"
  echo "  3) Build debug APK + install via ADB + stream logs (press 's' for screenshot)"
  echo "  4) Exit"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# --- Dispatch ---
ACTION=""
if [[ $# -gt 0 ]]; then
  case "$1" in
    1|release|--release)
      ACTION="1"
      ;;
    2|debug|--debug)
      ACTION="2"
      ;;
    3|install|--install|stream|--stream)
      ACTION="3"
      ;;
    4|exit|--exit)
      ACTION="4"
      ;;
    -h|--help)
      echo "Usage: ./build.sh [1|2|3|4|release|debug|install|exit]"
      echo "  1, release: Build release APK"
      echo "  2, debug:   Build debug APK"
      echo "  3, install: Build debug APK, install via ADB, stream logs ('s' for screenshot)"
      echo "  4, exit:    Exit"
      exit 0
      ;;
    *)
      echo "ERROR: Unknown option '$1'"
      echo "Usage: ./build.sh [1|2|3|4|release|debug|install|exit]"
      exit 1
      ;;
  esac
fi

if [[ -z "$ACTION" ]]; then
  while true; do
    show_menu
    read -r -p "Select option [1-4]: " choice
    case "$choice" in
      1)
        ACTION="1"
        break
        ;;
      2)
        ACTION="2"
        break
        ;;
      3)
        ACTION="3"
        break
        ;;
      4)
        ACTION="4"
        break
        ;;
      *)
        echo ""
        echo "⚠ Invalid option '$choice'. Please select 1, 2, 3, or 4."
        echo ""
        ;;
    esac
  done
fi

case "$ACTION" in
  1)
    build_apk "release"
    ;;
  2)
    build_apk "debug"
    ;;
  3)
    build_apk "debug"
    install_and_stream_logs "$BUILT_APK_PATH"
    ;;
  4)
    echo "Exiting."
    exit 0
    ;;
esac
