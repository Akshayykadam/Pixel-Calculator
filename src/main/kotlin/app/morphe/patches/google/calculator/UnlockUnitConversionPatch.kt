package app.morphe.patches.google.calculator

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.annotation.Patch
import app.morphe.patcher.util.smali.SmaliHelper

@Patch(
    name = "Unlock Unit Conversion",
    description = "Enables Rosetta Unit Conversion mode on non-Pixel Android devices.",
    compatibleWith = ["com.google.android.calculator"]
)
class UnlockUnitConversionPatch : BytecodePatch(
    setOf(
        Fingerprints.pixelExperienceCheckFingerprint,
        Fingerprints.rosettaFlagFingerprint,
        Fingerprints.nonPixelOverrideFlagFingerprint
    )
) {
    override fun execute() {
        // Force the PIXEL_EXPERIENCE check to return true
        Fingerprints.pixelExperienceCheckFingerprint.result?.let { result ->
            val method = result.mutableMethod
            method.returnEarly(1)
        }

        // Force Flag 45810769 (Rosetta master switch) to return true
        Fingerprints.rosettaFlagFingerprint.result?.let { result ->
            val method = result.mutableMethod
            method.returnEarly(1)
        }

        // Force Flag 45814182 (Non-Pixel fallback check) to return true
        Fingerprints.nonPixelOverrideFlagFingerprint.result?.let { result ->
            val method = result.mutableMethod
            method.returnEarly(1)
        }
    }
}
