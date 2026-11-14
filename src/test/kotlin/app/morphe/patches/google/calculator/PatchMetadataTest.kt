package app.morphe.patches.google.calculator

import app.morphe.patcher.patch.annotation.Patch
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PatchMetadataTest {

    @Test
    @DisplayName("Verify UnlockPixelSolverPatch metadata")
    fun testUnlockPixelSolverPatch() {
        val patchAnnotation = UnlockPixelSolverPatch::class.java.getAnnotation(Patch::class.java)
        assertNotNull(patchAnnotation)
        assertEquals("Unlock Pixel Solver (Ask Math)", patchAnnotation.name)
        assertTrue(patchAnnotation.compatibleWith.contains("com.google.android.calculator"))
    }

    @Test
    @DisplayName("Verify UnlockUnitConversionPatch metadata")
    fun testUnlockUnitConversionPatch() {
        val patchAnnotation = UnlockUnitConversionPatch::class.java.getAnnotation(Patch::class.java)
        assertNotNull(patchAnnotation)
        assertEquals("Unlock Unit Conversion", patchAnnotation.name)
        assertTrue(patchAnnotation.compatibleWith.contains("com.google.android.calculator"))
    }

    @Test
    @DisplayName("Verify ChangePackageNamePatch metadata")
    fun testChangePackageNamePatch() {
        val patchAnnotation = ChangePackageNamePatch::class.java.getAnnotation(Patch::class.java)
        assertNotNull(patchAnnotation)
        assertEquals("Change Package Name", patchAnnotation.name)
        assertTrue(patchAnnotation.compatibleWith.contains("com.google.android.calculator"))
        assertEquals("com.google.android.pixelcalculator", ChangePackageNamePatch.NEW_PACKAGE_NAME)
    }

    @Test
    @DisplayName("Verify BypassGmsSecurityExceptionPatch metadata")
    fun testBypassGmsSecurityExceptionPatch() {
        val patchAnnotation = BypassGmsSecurityExceptionPatch::class.java.getAnnotation(Patch::class.java)
        assertNotNull(patchAnnotation)
        assertEquals("Bypass GMS Security Exception", patchAnnotation.name)
        assertTrue(patchAnnotation.compatibleWith.contains("com.google.android.calculator"))
    }

    @Test
    @DisplayName("Verify bytecode fingerprints definition")
    fun testFingerprints() {
        assertNotNull(Fingerprints.pixelExperienceCheckFingerprint)
        assertNotNull(Fingerprints.rosettaFlagFingerprint)
        assertNotNull(Fingerprints.nonPixelOverrideFlagFingerprint)
        assertNotNull(Fingerprints.abacusMasterFlagFingerprint)
        assertNotNull(Fingerprints.gmsClientGetServiceFingerprint)
    }
}
