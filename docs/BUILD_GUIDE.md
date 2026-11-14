# Build & Installation Guide

This guide covers all methods for compiling, patching, and installing Pixel Calculator Mod.

---

## Prerequisites

- **Java Development Kit (JDK 17 or newer)**
- **Android SDK Build-Tools** (containing `zipalign` and `apksigner`)
- **Apktool** (v2.9.0 or higher)
- **Base Google Calculator v10.0+ Package** (available from APKMirror as `.apkm` or extracted APK splits)

---

## Method 1: Automated Pipeline (Recommended)

The automated pipeline handles decompilation, native library extraction, patch application, 16KB alignment, and APK signing in a single command.

```bash
# Provide the path to the downloaded .apkm or base.apk
./scripts/pipeline.sh /path/to/GoogleCalculator_10.0.apkm
```

Output APK will be available at: `dist/Pixel_Calculator_Modded.apk`.

---

## Method 2: Fast Recompile via `build.sh`

If the package has already been decompiled and patched in `decoded_apk/`:

```bash
./build.sh
```

This will:
1. Recompile `decoded_apk/` using `apktool`.
2. Perform 4-byte / 16KB page boundary alignment using `zipalign`.
3. Sign using Android debug credentials (`~/.android/debug.keystore`, auto-generated if missing).
4. Verify the v1/v2/v3 signatures.

---

## Method 3: Step-by-Step Manual Patching

```bash
# 1. Decompile base APK
apktool d base.apk -o decoded_apk -f

# 2. Extract 64-bit native libraries
unzip -q split_config.arm64_v8a.apk "lib/*" -d decoded_apk/

# 3. Apply smali patches sequentially
./scripts/apply-patches.sh decoded_apk

# 4. Rebuild and sign
./scripts/build.sh decoded_apk dist
```

---

## Method 4: Compiling Java & Kotlin Sources with Gradle

To compile the Kotlin Morphe patches and Java Solver Engine, or run the test suite:

```bash
# Run unit tests
./gradlew test

# Compile jar artifact
./gradlew jar
```

---

## Installation via ADB

Connect your Android 12+ device with USB debugging enabled, then run:

```bash
adb install -r dist/Pixel_Calculator_Modded.apk
```
