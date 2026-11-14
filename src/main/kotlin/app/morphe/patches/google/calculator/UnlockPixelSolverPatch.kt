package app.morphe.patches.google.calculator

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.annotation.Patch

@Patch(
    name = "Unlock Pixel Solver (Ask Math)",
    description = "Abacus (Ask Math) interface flag (disabled).",
    compatibleWith = ["com.google.android.calculator"]
)
class UnlockPixelSolverPatch : BytecodePatch(
    setOf(
        Fingerprints.abacusMasterFlagFingerprint
    )
) {
    override fun execute() {
        // Flag 45814179 (Abacus master switch) disabled (returns false)
        Fingerprints.abacusMasterFlagFingerprint.result?.let { result ->
            val method = result.mutableMethod
            method.returnEarly(0)
        }
    }
}
