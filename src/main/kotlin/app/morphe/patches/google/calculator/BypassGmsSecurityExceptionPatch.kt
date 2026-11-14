package app.morphe.patches.google.calculator

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.annotation.Patch

@Patch(
    name = "Bypass GMS Security Exception",
    description = "Prevents GMS BaseGmsClient from rethrowing SecurityException on renamed packages or debug signatures.",
    compatibleWith = ["com.google.android.calculator"]
)
class BypassGmsSecurityExceptionPatch : BytecodePatch(
    setOf(Fingerprints.gmsClientGetServiceFingerprint)
) {
    override fun execute() {
        Fingerprints.gmsClientGetServiceFingerprint.result?.let { result ->
            val method = result.mutableMethod
            // Replace throw v0 in SecurityException catch handler with graceful connection failure handling
            method.findCatchBlocks("Ljava/lang/SecurityException;").forEach { catchBlock ->
                catchBlock.rerouteToFallbackHandler()
            }
        }
    }
}
