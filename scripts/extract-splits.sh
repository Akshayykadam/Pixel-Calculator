#!/usr/bin/env bash
# ==============================================================================
# Pixel Calculator Mod - Native Libraries Extraction Script
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

SPLIT_APK="${1:-}"
TARGET_DIR="${2:-$ROOT_DIR/decoded_apk}"

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ -z "$SPLIT_APK" ]; then
    # Auto-detect split_config.arm64_v8a.apk in standard locations
    if [ -f "$ROOT_DIR/extracted/split_config.arm64_v8a.apk" ]; then
        SPLIT_APK="$ROOT_DIR/extracted/split_config.arm64_v8a.apk"
    elif [ -f "$ROOT_DIR/split_config.arm64_v8a.apk" ]; then
        SPLIT_APK="$ROOT_DIR/split_config.arm64_v8a.apk"
    else
        echo -e "${RED}Error:${NC} Usage: $0 <path-to-split_config.arm64_v8a.apk> [target-decoded-apk-dir]" >&2
        exit 1
    fi
fi

if [ ! -f "$SPLIT_APK" ]; then
    echo -e "${RED}Error:${NC} Split APK '$SPLIT_APK' not found." >&2
    exit 1
fi

if [ ! -d "$TARGET_DIR" ]; then
    echo -e "${RED}Error:${NC} Target directory '$TARGET_DIR' does not exist." >&2
    exit 1
fi

echo -e "${BLUE}==>${NC} Extracting native libraries from: ${YELLOW}$SPLIT_APK${NC}"
echo -e "${BLUE}==>${NC} Destination: ${YELLOW}$TARGET_DIR/lib/${NC}"

mkdir -p "$TARGET_DIR/lib"
unzip -q -o "$SPLIT_APK" "lib/*" -d "$TARGET_DIR/" || true

if [ -d "$TARGET_DIR/lib/arm64-v8a" ]; then
    LIB_COUNT=$(find "$TARGET_DIR/lib/arm64-v8a" -name "*.so" | wc -l | tr -d ' ')
    echo -e "${GREEN}==>${NC} Extracted $LIB_COUNT native shared libraries (.so) into $TARGET_DIR/lib/arm64-v8a"
else
    echo -e "${YELLOW}Warning:${NC} No arm64-v8a libraries found in $SPLIT_APK."
fi
