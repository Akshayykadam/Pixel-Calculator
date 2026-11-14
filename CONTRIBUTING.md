# Contributing to Pixel Calculator Mod

Thank you for your interest in contributing to Pixel Calculator Mod! This guide provides instructions on how to set up your environment, contribute patches, write tests, and submit pull requests.

---

## Code of Conduct

Please maintain a welcoming, respectful, and constructive environment for everyone.

---

## Development Environment Setup

### Prerequisites

- **JDK 17+**
- **Android SDK Build-Tools** (`zipalign`, `apksigner`)
- **Apktool** (v2.9.0 or higher)
- **Git**
- Target test device or Android Virtual Device running **Android 12 (API 32)** or higher (ARM64 architecture).

### Cloning the Repository

```bash
git clone https://github.com/Akshayykadam/Pixel-Calculator.git
cd Pixel-Calculator
```

---

## Project Structure Overview

- `patches/smali/`: Standalone `.patch` diffs applied sequentially to the decompiled APK.
- `patches/smali/solver/`: Injected `PixelSolverEngine` bytecode files.
- `src/main/kotlin/`: Morphe Kotlin patch definitions for the Morphe / ReVanced patcher ecosystem.
- `src/main/java/`: Java source code for the offline/online Pixel Solver engine.
- `src/test/`: JUnit 5 unit tests for solver engine logic and patch metadata.
- `scripts/`: Modular build and packaging automation:
  - `scripts/build.sh`: Recompiles, 4-byte/16KB aligns, and signs the APK.
  - `scripts/apply-patches.sh`: Applies smali diffs and injects the solver engine.
  - `scripts/extract-splits.sh`: Extracts native 64-bit libraries from split APKs.
  - `scripts/pipeline.sh`: Full automated end-to-end pipeline from base APK or APKM bundle.
- `build.sh`: Convenient top-level entry point that runs the build pipeline.

---

## Modifying and Adding Patches

### Smali Patches

When updating or adding a smali patch:
1. Ensure the patch file is numbered sequentially (e.g., `07-my-feature.patch`) in `patches/smali/`.
2. Generate standard unified diffs using:
   ```bash
   diff -u original/smali_classes2/target.smali modified/smali_classes2/target.smali > patches/smali/07-my-feature.patch
   ```
3. Test applying the patch cleanly:
   ```bash
   ./scripts/apply-patches.sh decoded_apk
   ```

### Java / Kotlin Solver Engine

1. Modify or add functionality in `src/main/java/com/android/calculator2/abacus/solver/PixelSolverEngine.java`.
2. Add corresponding tests in `src/test/java/com/android/calculator2/abacus/solver/PixelSolverEngineTest.java`.
3. Verify all tests pass with Gradle:
   ```bash
   ./gradlew test
   ```

---

## Building and Testing Locally

1. Place your base Google Calculator package (v10.0+ `.apkm` or `base.apk`) in the repository or run:
   ```bash
   ./scripts/pipeline.sh /path/to/GoogleCalculator.apkm
   ```
2. The final signed APK is output to `dist/Pixel_Calculator_Modded.apk`.
3. Sideload and test on your device via ADB:
   ```bash
   adb install -r dist/Pixel_Calculator_Modded.apk
   ```

---

## Pull Request Guidelines

1. Create a descriptive feature branch from `master`:
   ```bash
   git checkout -b feature/my-enhancement
   ```
2. Ensure `./gradlew test` passes cleanly.
3. Ensure shell scripts pass syntax checks (`bash -n scripts/*.sh build.sh`).
4. Commit changes with clear, semantic commit messages (e.g., `feat:`, `fix:`, `docs:`, `refactor:`).
5. Open a Pull Request referencing any related issues.
