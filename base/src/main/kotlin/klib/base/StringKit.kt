package klib.base

import java.security.SecureRandom
import kotlin.experimental.and
import kotlin.random.Random

val ALPHA_CHARS = ('a'..'z') + ('A'..'Z')
val ALPHANUM_CHARS = ALPHA_CHARS + ('0'..'9')
val COMMON_CHARS = ALPHANUM_CHARS + ".- ".toList()

// TODO Remove once it gets out of experimental state in Kotlin stdlib
fun CharArray.concatToString() = String(this)

fun Random.string(len: Int = 10, chars: Array<Char> = ALPHANUM_CHARS.toTypedArray()) = ByteArray(len).let { bytes ->
    SecureRandom().nextBytes(bytes)
    bytes.indices.asSequence()
        .map { i ->
            ALPHANUM_CHARS[(bytes[i] and 0xFF.toByte() and (ALPHANUM_CHARS.size - 1).toByte()).toInt()]
        }.toList().toCharArray().concatToString()

}

inline fun String.prefixWith(prefix: String?) = prefix?.plus(this) ?: this

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

private val notAlpha: Regex by lazy { "[^0-9a-zA-Z]+".toRegex() }
fun String?.toNormalizedAlpha() = this
    ?.replace(notAlpha, " ")
    ?.toNormalizedSpaces()

private val noDigits: Regex by lazy { "[^0-9]+".toRegex() }
val String?.tryLong get() = this?.replace(noDigits, "")?.toLongOrNull()

