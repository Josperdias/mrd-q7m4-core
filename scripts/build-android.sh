#!/usr/bin/env bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_DIR" ]]; then echo 'Set ANDROID_SDK_ROOT to an SDK with platform 35 and build-tools 35.0.0.' >&2; exit 1; fi
BT="${MERIDIAN_BUILD_TOOLS:-$SDK_DIR/build-tools/35.0.0}"
PLATFORM="${MERIDIAN_ANDROID_JAR:-$SDK_DIR/platforms/android-35/android.jar}"
BUILD="$PROJECT_ROOT/android/build"
mkdir -p "$BUILD/classes" "$BUILD/dex" "$BUILD/assets/web" "$BUILD/res/drawable"
cp -R "$PROJECT_ROOT/dist/." "$BUILD/assets/web/"
cp "$PROJECT_ROOT/dist/icon-192.png" "$BUILD/res/drawable/ic_launcher.png"
javac -source 8 -target 8 -bootclasspath "$PLATFORM:$BT/core-lambda-stubs.jar" -d "$BUILD/classes" "$PROJECT_ROOT/android/src/app/mrd/q7m4/MainActivity.java"
mapfile -t CLASS_FILES < <(find "$BUILD/classes" -name '*.class')
"$BT/d8" --lib "$PLATFORM" --min-api 26 --output "$BUILD/dex" "${CLASS_FILES[@]}"
"$BT/aapt" package -f -M "$PROJECT_ROOT/android/AndroidManifest.xml" -I "$PLATFORM" -S "$BUILD/res" -A "$BUILD/assets" -F "$BUILD/unsigned.apk"
(cd "$BUILD/dex" && zip -q -j "$BUILD/unsigned.apk" classes.dex)
"$BT/zipalign" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
# Development signing only. This generated key is ignored and never committed.
KEYSTORE="$BUILD/development.keystore"
if [[ ! -f "$KEYSTORE" ]]; then keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Meridian Development,O=Meridian,C=BR' -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1; fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android --out "$BUILD/Meridian-1.0.0.apk" "$BUILD/aligned.apk"
"$BT/apksigner" verify --verbose "$BUILD/Meridian-1.0.0.apk"
echo "$BUILD/Meridian-1.0.0.apk"
