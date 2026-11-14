#!/usr/bin/env bash
# ==============================================================================
# Pixel Calculator Mod - Full Automated Pipeline
# (Decompile -> Extract Splits -> Patch Smali -> Recompile -> Align -> Sign)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

INPUT_FILE="${1:-}"

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}    Pixel Calculator Mod - End-to-End Pipeline        ${NC}"
echo -e "${BLUE}======================================================${NC}"

# Find input if not provided
if [ -z "$INPUT_FILE" ]; then
    # Check for .apkm or base.apk in current directory or base/
    FOUND_APKM=$(find "$ROOT_DIR" -maxdepth 2 -name "*.apkm" | head -n 1 || true)
    FOUND_BASE=$(find "$ROOT_DIR" -maxdepth 2 -name "base.apk" | head -n 1 || true)
    if [ -n "$FOUND_APKM" ]; then
        INPUT_FILE="$FOUND_APKM"
    elif [ -n "$FOUND_BASE" ]; then
        INPUT_FILE="$FOUND_BASE"
    fi
fi

if [ -z "$INPUT_FILE" ] || [ ! -f "$INPUT_FILE" ]; then
    echo -e "${RED}Error:${NC} No base APK or APKM bundle specified or found." >&2
    echo "Usage: $0 <path-to-base.apk or .apkm>" >&2
    exit 1
fi

echo -e "${BLUE}==>${NC} Input artifact: ${YELLOW}$INPUT_FILE${NC}"

EXTRACT_DIR="$ROOT_DIR/build/extracted_input"
DECODED_DIR="$ROOT_DIR/decoded_apk"
BASE_APK=""
SPLIT_ARM64=""

# Handle .apkm bundle (zip archive of splits)
if [[ "$INPUT_FILE" == *.apkm ]] || [[ "$INPUT_FILE" == *.zip ]]; then
    echo -e "${BLUE}==>${NC} Unpacking APK bundle..."
    rm -rf "$EXTRACT_DIR"
    mkdir -p "$EXTRACT_DIR"
    unzip -q -o "$INPUT_FILE" -d "$EXTRACT_DIR"
    
    BASE_APK=$(find "$EXTRACT_DIR" -name "base.apk" | head -n 1 || true)
    SPLIT_ARM64=$(find "$EXTRACT_DIR" -name "*arm64*.apk" | head -n 1 || true)
else
    BASE_APK="$INPUT_FILE"
    # Try finding split arm64 alongside base.apk
    INPUT_PARENT=$(dirname "$INPUT_FILE")
    SPLIT_ARM64=$(find "$INPUT_PARENT" -name "*arm64*.apk" 2>/dev/null | head -n 1 || true)
fi

if [ -z "$BASE_APK" ] || [ ! -f "$BASE_APK" ]; then
    echo -e "${RED}Error:${NC} Failed to locate base.apk in $INPUT_FILE" >&2
    exit 1
fi

echo -e "${GREEN}[OK]${NC} Base APK: $BASE_APK"
if [ -n "$SPLIT_ARM64" ]; then
    echo -e "${GREEN}[OK]${NC} Split arm64 APK: $SPLIT_ARM64"
else
    echo -e "${YELLOW}[INFO]${NC} No separate arm64 split found (standalone APK assumed)."
fi

# 1. Decompile base APK
echo -e "\n${BLUE}==>${NC} Decompiling base APK with apktool into ${YELLOW}$DECODED_DIR${NC}..."
apktool d -f "$BASE_APK" -o "$DECODED_DIR"

# 2. Extract arm64 native libraries if split is available
if [ -n "$SPLIT_ARM64" ] && [ -f "$SPLIT_ARM64" ]; then
    echo -e "\n${BLUE}==>${NC} Extracting native libraries from split APK..."
    "$SCRIPT_DIR/extract-splits.sh" "$SPLIT_ARM64" "$DECODED_DIR"
fi

# 3. Apply Smali Patches and inject Solver Engine
echo -e "\n${BLUE}==>${NC} Applying patches and injecting solver engine..."
"$SCRIPT_DIR/apply-patches.sh" "$DECODED_DIR"

# 4. Recompile, Align, and Sign
echo -e "\n${BLUE}==>${NC} Recompiling and signing final APK..."
"$SCRIPT_DIR/build.sh" "$DECODED_DIR" "$ROOT_DIR/dist"

echo -e "\n${GREEN}Pipeline completed successfully!${NC}"
