# Architecture & Reverse Engineering Details

This document outlines the reverse engineering findings, bytecode modifications, and structural architecture of the **Pixel Calculator Mod**.

---

## 1. Overview & Constraints

Starting with Google Calculator v10.0+, advanced features were gated behind:
1. **Hardware Checks**: Calling `PackageManager.hasSystemFeature("com.google.android.feature.PIXEL_EXPERIENCE")`.
2. **Phenotype Flags**: Remote configuration values queried from Google Play Services.
3. **Attestation & DroidGuard**: Remote gRPC calls to `pixelmerlin-pa.googleapis.com` secured by hardware-backed attestation tokens.
4. **Package Signature Verification**: Internal GMS service binding failing when package name or signing certificate changes.

---

## 2. Bytecode & Smali Modification Breakdown

### 2.1 Hardware Check Bypass (`patches/smali/01-unlock-unit-conversion.patch`)
- **Target Class**: `gww` (method `c()Z`)
- **Original Logic**: Evaluates `Optional<Boolean>` returned from system feature query.
- **Patched Logic**: Directly loads boolean literal `const/4 v0, 0x1` and returns `true`.

### 2.2 Phenotype Configuration Flag Overrides
- **Rosetta Unit Converter Switch**:
  - Class `jtw` (method `p()Z`): Forces flag `45810769` to return `true`.
  - Class `jtt` (method `b()Z`): Forces fallback flag `45814182` to return `true`.
- **Abacus (Pixel Solver) Switch (Currently Disabled)**:
  - Class `jtn` (method `d()Z`): Evaluates flag `45814179` (returns `false` / `0` to keep feature hidden).
  - Class `jtq` (method `i()Z`): Evaluates flag `45814179` (returns `false` / `0`).

### 2.3 GMS DroidGuard Security Bypass (`patches/smali/05-bypass-gms-security-exception.patch`)
- **Target Class**: `fuf` (method `r(Lfve;Ljava/util/Set;)V`)
- **Issue**: Google Play Services caller verification fails when the package is renamed or signed with a custom keystore, throwing `java.lang.SecurityException: PackageVerificationRslt: not allowed`.
- **Patched Logic**: Replaces `throw v0` in the exception catch handler with a jump to the graceful disconnect handler (`:goto_4`), avoiding process termination.

### 2.4 Package Coexistence & Manifest Adjustments (`patches/smali/03-change-package-name.patch`)
- Renames package from `com.google.android.calculator` to `com.google.android.pixelcalculator`.
- Strips `original-package` to prevent installation conflicts with pre-installed system apps.
- Remaps ContentProvider authorities (`mlkitinitprovider`).
- Scopes dynamic broadcast receivers to prevent permission collisions.

### 2.5 Standalone Single-APK Conversion (`patches/smali/04-standalone-splits-fix.patch`)
- Extracts native libraries from `split_config.arm64_v8a.apk` and embeds them under `lib/arm64-v8a/`.
- Removes `com.android.vending.splits.required` and split dependencies from `AndroidManifest.xml` and `apktool.yml`.
- Configures 4-byte and 16KB page alignment for Android 15+ kernel compatibility.

### 2.6 Local & Online Solver Engine (`src/main/java/.../PixelSolverEngine.java`)
- **Hook Point**: `erz.smali` reroutes queries from the remote gRPC endpoint to `PixelSolverEngine.solve(query)`.
- **Components**:
  - Offline mathematical evaluator (algebra, quadratic equations, linear expressions).
  - Unit and temperature converters.
  - Interactive GenUI card builders (Bill splitter, BMI calculator, EMI/loan estimator).
  - Real-time Forex currency converter with caching.
  - Optional Gemini AI Gateway support.

---

## 3. Morphe / ReVanced Patch Implementation

In addition to Smali unified diffs, the repository contains declarative Kotlin bytecode patch classes under `src/main/kotlin/app/morphe/patches/google/calculator/`:
- `Fingerprints.kt`: Bytecode opcode and string pattern definitions.
- `UnlockUnitConversionPatch.kt`: Runtime bytecode modification for Rosetta.
- `UnlockPixelSolverPatch.kt`: Runtime bytecode modification for Abacus.
- `ChangePackageNamePatch.kt`: XML DOM transformation for dual installation.
- `BypassGmsSecurityExceptionPatch.kt`: Catch block rerouting for DroidGuard.
