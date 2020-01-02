package klib.base

//import org.apache.commons.lang3.time.StopWatch
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by elifarley on 22/11/16.
 */

val Throwable.simpleMessage get() = "(${javaClass.simpleName}) ${message.trimToDefault(toString())}"

//fun StopWatch.stopIfRunning(): StopWatch { if (!isStopped) stop(); return this }

fun Random.nextInt(range: IntRange): Int = if (range.first == range.last) range.first
else range.first + nextInt(range.last - range.first)

inline fun rnd(range: IntRange = 0..Int.MAX_VALUE) = ThreadLocalRandom.current().nextInt(range)

/**
 * true, false, false -> 100(binary) = 4
 */
fun boolsToInt(vararg bs: Boolean): Int {
    var sum = 0
    var i = bs.size - 1
    bs.forEach { b ->
        if (b) sum += if (i == 0) 1 else 1 shl i
        i--
    }
    return sum
}

fun Int.toBools(size: Int) = BooleanArray(size) { i ->
    (1 shl (size - 1 - i) and this) != 0
}

private val HEX_CHARS = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

/**
 *  Returns the string of two characters representing the HEX value of the byte.
 */
fun Byte.toHexString(): String {
    val i = this.toInt()
    val char2 = HEX_CHARS[i and 0x0f]
    val char1 = HEX_CHARS[i shr 4 and 0x0f]
    return "$char1$char2"
}

fun ByteArray.toHexString(): String {
    val builder = StringBuilder(this.size * 2)
    for (b in this) {
        builder.append(b.toHexString())
    }
    return builder.toString()
}

val Array<*>.asByteArray
    get() = StringBuilder().let {
        for (item in this) {
            it.append(item).append('\u0000')
        }
        it.toString().toByteArray(Charset.forName("ISO-8859-1"))
    }

inline fun Array<*>.asNameUUID() = UUID.nameUUIDFromBytes(asByteArray).toString()

val Array<*>.md5Sum
    get() = MessageDigest.getInstance("MD5").let {
        it.digest(this.asByteArray).toHexString()
    }

val Pair<String, String>.md5Sum
    get() = MessageDigest.getInstance("MD5").let {
        it.digest(arrayOf(this.first, this.second).asByteArray).toHexString()
    }