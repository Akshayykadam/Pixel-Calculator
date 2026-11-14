package app.morphe.patcher.patch

import app.morphe.patcher.fingerprint.Fingerprint
import org.w3c.dom.Document

abstract class BytecodePatch(
    val fingerprints: Set<Fingerprint> = emptySet()
) {
    abstract fun execute()
}

class PatchContext {
    lateinit var manifest: Document
}

abstract class ResourcePatch {
    val context: PatchContext = PatchContext()
    abstract fun execute()
}
