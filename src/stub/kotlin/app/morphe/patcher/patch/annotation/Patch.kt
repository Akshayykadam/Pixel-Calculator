package app.morphe.patcher.patch.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Patch(
    val name: String = "",
    val description: String = "",
    val compatibleWith: Array<String> = []
)
