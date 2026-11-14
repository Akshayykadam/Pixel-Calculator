package app.morphe.patcher.fingerprint

data class Fingerprint(
    val strings: List<String> = emptyList(),
    val returnType: String? = null,
    val opcodes: List<Int> = emptyList()
) {
    var result: FingerprintResult? = null
}

class FingerprintResult(
    val mutableMethod: MutableMethod = MutableMethod()
)

class MutableMethod {
    fun returnEarly(value: Any?) {}
    fun findCatchBlocks(exceptionType: String): List<CatchBlock> = emptyList()
}

class CatchBlock {
    fun rerouteToFallbackHandler() {}
}
