#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AVD_NAME="kexplore_test"
SYSTEM_IMAGE="system-images;android-35;google_apis;arm64-v8a"
PACKAGE="dev.nutting.kexplore"

# Create AVD if it doesn't exist
if ! emulator -list-avds 2>/dev/null | grep -q "^${AVD_NAME}$"; then
    echo "==> AVD '$AVD_NAME' not found, creating..."
    echo "no" | avdmanager create avd -n "$AVD_NAME" -k "$SYSTEM_IMAGE" -d pixel_6
fi

# Kill any existing emulator
if adb devices 2>/dev/null | grep -q emulator; then
    echo "==> Killing existing emulator..."
    adb emu kill 2>/dev/null || true
    sleep 3
fi

# Launch emulator
echo "==> Starting emulator ($AVD_NAME)..."
emulator -avd "$AVD_NAME" -no-audio -no-snapshot -gpu swiftshader_indirect &
EMULATOR_PID=$!

# Wait for boot
echo "==> Waiting for device..."
adb wait-for-device
echo "==> Waiting for boot to complete..."
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
    sleep 3
done
echo "==> Boot complete."

# Build and install
echo "==> Building debug APK..."
"$SCRIPT_DIR/../gradlew" :app:assembleDebug

echo "==> Installing..."
adb install -r "$SCRIPT_DIR/../app/build/outputs/apk/debug/app-debug.apk"

echo "==> Launching..."
adb shell am start -n "$PACKAGE/.MainActivity"

echo ""
echo "Ready. Emulator PID: $EMULATOR_PID"
echo "To stop: adb emu kill"
