package klib.base

inline fun String.prefixWith(prefix: String?) = if (prefix == null) this else prefix + this

fun Any?.fmt(format: String, defaultWhenNull: String = ""): String = this?.let {
    format.format(this)
} ?: defaultWhenNull

fun String?.trimToDefault(default: String = "") =
        this?.trim()?.run { if (isEmpty()) null else this } ?: default

fun String?.trimToNull() =
        this?.trim()?.run { if (isEmpty()) null else this }

private val illegalNameCharsLowerCase: Regex by lazy { """[^-.a-záéíóúàèìòùâêîôûäëïöüãẽĩõũñç]+""".toRegex() }
private val twoOrMoreSpaces: Regex by lazy { """\s{2,}""".toRegex() }

fun String?.toNormalizedSpaces() = this.trimToNull()
        ?.replace(twoOrMoreSpaces, " ")

fun String?.toNormalizedLowerCase() = this.trimToNull()
        ?.toLowerCase()?.replace(illegalNameCharsLowerCase, " ")
        ?.toNormalizedSpaces()

private val noDigits: Regex by lazy { "[^0-9]+".toRegex() }
val String?.tryLong get() = this?.replace(noDigits, "")?.toLongOrNull()

