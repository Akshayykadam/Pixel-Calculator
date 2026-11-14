#!/usr/bin/env bash
# ==============================================================================
# Pixel Calculator Mod - Recompile, Align, and Sign Script
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

SOURCE_DIR="${1:-$ROOT_DIR/decoded_apk}"
OUTPUT_DIR="${2:-$ROOT_DIR/dist}"

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}       Pixel Calculator Mod - APK Builder             ${NC}"
echo -e "${BLUE}======================================================${NC}"

# 1. Check Apktool
APKTOOL=$(command -v apktool || true)
if [ -z "$APKTOOL" ]; then
    echo -e "${RED}Error:${NC} apktool is not found in PATH." >&2
    echo "Install apktool via: 'brew install apktool' (macOS) or package manager (Linux)." >&2
    exit 1
fi
echo -e "${GREEN}[OK]${NC} Using apktool: $APKTOOL"

# 2. Find Android SDK Build-Tools (zipalign & apksigner)
ZIPALIGN=""
APKSIGNER=""

SDK_SEARCH_PATHS=(
    "${ANDROID_HOME:-}/build-tools"
    "${ANDROID_SDK_ROOT:-}/build-tools"
    "$HOME/Library/Android/sdk/build-tools"
    "$HOME/Android/Sdk/build-tools"
    "/usr/lib/android-sdk/build-tools"
)

for sdk_path in "${SDK_SEARCH_PATHS[@]}"; do
    if [ -d "$sdk_path" ]; then
        LATEST_DIR=$(find "$sdk_path" -maxdepth 1 -type d 2>/dev/null | sort -V | tail -n 1 || true)
        if [ -n "$LATEST_DIR" ] && [ -f "$LATEST_DIR/zipalign" ] && [ -f "$LATEST_DIR/apksigner" ]; then
            ZIPALIGN="$LATEST_DIR/zipalign"
            APKSIGNER="$LATEST_DIR/apksigner"
            break
        fi
    fi
done

# Fallback to PATH
if [ -z "$ZIPALIGN" ]; then
    ZIPALIGN=$(command -v zipalign || true)
fi
if [ -z "$APKSIGNER" ]; then
    APKSIGNER=$(command -v apksigner || true)
fi

if [ -z "$ZIPALIGN" ] || [ -z "$APKSIGNER" ]; then
    echo -e "${RED}Error:${NC} zipalign or apksigner not found." >&2
    echo "Please set ANDROID_HOME or ensure Android SDK build-tools are installed and in your PATH." >&2
    exit 1
fi

echo -e "${GREEN}[OK]${NC} Using zipalign:  $ZIPALIGN"
echo -e "${GREEN}[OK]${NC} Using apksigner: $APKSIGNER"

# 3. Verify Source Directory
if [ ! -d "$SOURCE_DIR" ]; then
    echo -e "${RED}Error:${NC} Source directory '$SOURCE_DIR' does not exist." >&2
    echo "Run the patching pipeline first using: ./scripts/pipeline.sh <base.apk or .apkm>" >&2
    exit 1
fi

# 4. Keystore Setup
KEYSTORE="$HOME/.android/debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
    mkdir -p "$(dirname "$KEYSTORE")"
    echo -e "${YELLOW}==>${NC} Debug keystore not found. Generating new debug keystore at $KEYSTORE..."
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -alias androiddebugkey \
        -dname "CN=Android Debug,O=Android,C=US" \
        -validity 10000 \
        -keyalg RSA \
        -keysize 2048 \
        -storepass android \
        -keypass android
    echo -e "${GREEN}[OK]${NC} Debug keystore generated."
else
    echo -e "${GREEN}[OK]${NC} Using debug keystore: $KEYSTORE"
fi

# 5. Build Paths
mkdir -p "$OUTPUT_DIR"
UNALIGNED_APK="$OUTPUT_DIR/app-unaligned.apk"
ALIGNED_APK="$OUTPUT_DIR/app-aligned.apk"
FINAL_APK="$OUTPUT_DIR/Pixel_Calculator_Modded.apk"

# 6. Recompile with Apktool
echo -e "\n${BLUE}==>${NC} Recompiling APK with apktool from: ${YELLOW}$SOURCE_DIR${NC}..."
"$APKTOOL" b "$SOURCE_DIR" -o "$UNALIGNED_APK"

# 7. Zipalign (4-byte / 16KB page alignment)
echo -e "\n${BLUE}==>${NC} Aligning APK (4-byte / 16KB page boundary alignment)..."
"$ZIPALIGN" -f -p 4 "$UNALIGNED_APK" "$ALIGNED_APK"
rm -f "$UNALIGNED_APK"

# 8. Sign APK (v1, v2, v3 schemes)
echo -e "\n${BLUE}==>${NC} Signing APK with debug certificate (v1, v2, v3 schemes)..."
"$APKSIGNER" sign \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --ks-key-alias androiddebugkey \
    --key-pass pass:android \
    --out "$FINAL_APK" \
    "$ALIGNED_APK"
rm -f "$ALIGNED_APK"

# 9. Verify Signature
echo -e "\n${BLUE}==>${NC} Verifying APK signature..."
"$APKSIGNER" verify --verbose "$FINAL_APK"

echo -e "\n${GREEN}======================================================${NC}"
echo -e "${GREEN}  BUILD COMPLETE: $FINAL_APK${NC}"
echo -e "${GREEN}======================================================${NC}"
echo -e "Install using ADB: ${YELLOW}adb install -r \"$FINAL_APK\"${NC}\n"
