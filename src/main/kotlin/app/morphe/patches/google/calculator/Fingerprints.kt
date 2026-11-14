package app.morphe.patches.google.calculator

import app.morphe.patcher.fingerprint.Fingerprint

object Fingerprints {
    val pixelExperienceCheckFingerprint = Fingerprint(
        strings = listOf("com.google.android.feature.PIXEL_EXPERIENCE"),
        returnType = "Z"
    )

    val rosettaFlagFingerprint = Fingerprint(
        strings = listOf("45810769"),
        returnType = "Lgwq;"
    )

    val nonPixelOverrideFlagFingerprint = Fingerprint(
        strings = listOf("45814182"),
        returnType = "Lgwq;"
    )

    val abacusMasterFlagFingerprint = Fingerprint(
        strings = listOf("45814179"),
        returnType = "Lgwq;"
    )

    val gmsClientGetServiceFingerprint = Fingerprint(
        strings = listOf("IGmsServiceBroker.getService failed"),
        returnType = "V"
    )
}
