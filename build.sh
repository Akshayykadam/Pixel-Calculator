#!/usr/bin/env bash
# ==============================================================================
# Pixel Calculator Mod - Build Entry Point
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# If decoded_apk directory exists, run the build script directly
if [ -d "$SCRIPT_DIR/decoded_apk" ]; then
    exec "$SCRIPT_DIR/scripts/build.sh" "$@"
fi

# If decoded_apk does not exist, check if an APK or APKM bundle is available to run full pipeline
FOUND_APKM=$(find "$SCRIPT_DIR" -maxdepth 2 -name "*.apkm" | head -n 1 || true)
FOUND_BASE=$(find "$SCRIPT_DIR" -maxdepth 2 -name "base.apk" | head -n 1 || true)

if [ -n "$FOUND_APKM" ]; then
    echo "Notice: 'decoded_apk' directory not found, but found package bundle: $FOUND_APKM"
    echo "Running end-to-end pipeline automatically..."
    exec "$SCRIPT_DIR/scripts/pipeline.sh" "$FOUND_APKM"
elif [ -n "$FOUND_BASE" ]; then
    echo "Notice: 'decoded_apk' directory not found, but found base.apk: $FOUND_BASE"
    echo "Running end-to-end pipeline automatically..."
    exec "$SCRIPT_DIR/scripts/pipeline.sh" "$FOUND_BASE"
else
    echo "Error: Neither 'decoded_apk' nor base APK package found." >&2
    echo "Please provide a Google Calculator v10.0+ APK or APKM bundle:" >&2
    echo "  ./scripts/pipeline.sh <path-to-calculator.apkm>" >&2
    exit 1
fi
