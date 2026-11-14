package app.morphe.patches.google.calculator

import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.annotation.Patch
import org.w3c.dom.Element

@Patch(
    name = "Change Package Name",
    description = "Changes the package name to com.google.android.pixelcalculator to allow installing alongside the stock calculator.",
    compatibleWith = ["com.google.android.calculator"]
)
class ChangePackageNamePatch : ResourcePatch() {
    companion object {
        const val NEW_PACKAGE_NAME = "com.google.android.pixelcalculator"
        const val OLD_PACKAGE_NAME = "com.google.android.calculator"
    }

    override fun execute() {
        val manifest = context.manifest
        val manifestElement = manifest.documentElement

        // Set application display name to Pixel Calculator
        val appNodes = manifest.getElementsByTagName("application")
        for (i in 0 until appNodes.length) {
            val app = appNodes.item(i) as Element
            app.setAttribute("android:label", "Pixel Calculator")
        }

        // Set main activity display name to Pixel Calculator
        val activityNodes = manifest.getElementsByTagName("activity")
        for (i in 0 until activityNodes.length) {
            val act = activityNodes.item(i) as Element
            if (act.getAttribute("android:name") == "com.android.calculator2.Calculator") {
                act.setAttribute("android:label", "Pixel Calculator")
            }
        }

        // Update package name
        manifestElement.setAttribute("package", NEW_PACKAGE_NAME)

        // Remove original-package tag to avoid system package linking
        val originalPkgNodes = manifest.getElementsByTagName("original-package")
        for (i in 0 until originalPkgNodes.length) {
            val node = originalPkgNodes.item(i)
            node.parentNode.removeChild(node)
        }

        // Update MLKit Init Provider authority
        val providerNodes = manifest.getElementsByTagName("provider")
        for (i in 0 until providerNodes.length) {
            val provider = providerNodes.item(i) as Element
            val authority = provider.getAttribute("android:authorities")
            if (authority == "$OLD_PACKAGE_NAME.mlkitinitprovider") {
                provider.setAttribute("android:authorities", "$NEW_PACKAGE_NAME.mlkitinitprovider")
            }
        }

        // Update dynamic receiver permissions
        val permissionNodes = manifest.getElementsByTagName("permission")
        for (i in 0 until permissionNodes.length) {
            val perm = permissionNodes.item(i) as Element
            val name = perm.getAttribute("android:name")
            if (name.startsWith(OLD_PACKAGE_NAME)) {
                perm.setAttribute("android:name", name.replace(OLD_PACKAGE_NAME, NEW_PACKAGE_NAME))
            }
        }

        val usesPermissionNodes = manifest.getElementsByTagName("uses-permission")
        for (i in 0 until usesPermissionNodes.length) {
            val perm = usesPermissionNodes.item(i) as Element
            val name = perm.getAttribute("android:name")
            if (name.startsWith(OLD_PACKAGE_NAME)) {
                perm.setAttribute("android:name", name.replace(OLD_PACKAGE_NAME, NEW_PACKAGE_NAME))
            }
        }

        // Strip split-APK enforcement
        manifestElement.removeAttribute("android:requiredSplitTypes")
        manifestElement.removeAttribute("android:splitTypes")
    }
}
