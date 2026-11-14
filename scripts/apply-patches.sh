#!/usr/bin/env bash
# ==============================================================================
# Pixel Calculator Mod - Automated Patch Application Script
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PATCHES_DIR="$ROOT_DIR/patches/smali"
TARGET_DIR="${1:-$ROOT_DIR/decoded_apk}"

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}==>${NC} Target directory: ${YELLOW}$TARGET_DIR${NC}"
echo -e "${BLUE}==>${NC} Patches directory: ${YELLOW}$PATCHES_DIR${NC}"

if [ ! -d "$TARGET_DIR" ]; then
    echo -e "${RED}Error:${NC} Target directory '$TARGET_DIR' does not exist." >&2
    echo "Please decompile the base Google Calculator APK with apktool first." >&2
    exit 1
fi

if ! command -v patch >/dev/null 2>&1; then
    echo -e "${RED}Error:${NC} 'patch' utility not found. Please install patch via your package manager." >&2
    exit 1
fi

# Apply smali patches sequentially
echo -e "\n${BLUE}==>${NC} Applying smali patches..."
PATCH_FILES=$(find "$PATCHES_DIR" -maxdepth 1 -name "*.patch" | sort)

if [ -z "$PATCH_FILES" ]; then
    echo -e "${RED}Error:${NC} No patch files found in $PATCHES_DIR" >&2
    exit 1
fi

cd "$TARGET_DIR"

for patch_file in $PATCH_FILES; do
    patch_name=$(basename "$patch_file")
    echo -e "  ${BLUE}-->${NC} Applying ${YELLOW}$patch_name${NC}..."
    if patch -p1 --forward -r - < "$patch_file" >/dev/null 2>&1; then
        echo -e "      ${GREEN}[OK]${NC} Successfully applied $patch_name"
    else
        # Check if already applied
        if patch -p1 -R --dry-run < "$patch_file" >/dev/null 2>&1; then
            echo -e "      ${YELLOW}[SKIP]${NC} $patch_name is already applied."
        else
            echo -e "      ${RED}[FAIL]${NC} Failed to apply $patch_name" >&2
            exit 1
        fi
    fi
done

# Copy PixelSolverEngine smali classes
SOLVER_SRC="$PATCHES_DIR/solver"
SOLVER_DEST="$TARGET_DIR/smali_classes2/com/android/calculator2/abacus/solver"

if [ -d "$SOLVER_SRC" ]; then
    echo -e "\n${BLUE}==>${NC} Injecting PixelSolverEngine smali classes..."
    mkdir -p "$SOLVER_DEST"
    cp -r "$SOLVER_SRC/"*.smali "$SOLVER_DEST/"
    echo -e "      ${GREEN}[OK]${NC} Injected $(ls "$SOLVER_SRC"/*.smali | wc -l | tr -d ' ') solver bytecode classes into $SOLVER_DEST"
else
    echo -e "${YELLOW}Warning:${NC} Solver directory $SOLVER_SRC not found, skipping smali injection."
fi

cd "$ROOT_DIR"
echo -e "\n${GREEN}==>${NC} All patches applied successfully!"
