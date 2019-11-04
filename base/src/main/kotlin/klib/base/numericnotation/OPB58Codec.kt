package klib.base.numericnotation


import klib.base.math.BitKit.threeBytes2Int
import klib.base.math.BitKit.toByteArray
import klib.base.math.BitKit.twoBytes2Int
import klib.base.math.RandomKit
import java.math.BigInteger
import java.util.*


// See https://youtrack.jetbrains.com/issue/KT-10477
object OPB58Codec {

    fun Long.toOPB58() = encode(this)
    fun Int.toOPB58() = encode(this.toLong())
    fun Long.Companion.fromOPB58(v: String) = decodeToLong(v)
    fun Int.Companion.fromOPB58(v: String) = decodeToInt(v)

    private val BIGINT_BYTE_MAXVAL = BigInteger.valueOf(255)

    val BIGINT_LONG_MAXVAL = BigInteger.valueOf(java.lang.Long.MAX_VALUE)

    val BIGINT_INTEGER_MAXVAL = BigInteger.valueOf(Integer.MAX_VALUE.toLong())

    val INTEGER_MAX_VALUE = "3FkQ47"

    val LONG_MAX_VALUE = "MPk5mJp7qEB"

    private val ALPHABET = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz"

    private val ALPHABET_CHARS = ALPHABET.toCharArray()

    private val ALPHABET_SIZE = ALPHABET_CHARS.size

    private val ALPHABET_SIZE_AS_BIGINT = BigInteger.valueOf(ALPHABET_SIZE.toLong())

    private val BIGINT_0 = BigInteger.valueOf(0)

    private val ALPHABET_MIN_CHAR = ALPHABET[0]

    private val OPB58_DIGIT_2_INT = IntArray(ALPHABET[ALPHABET_SIZE - 1] - ALPHABET_MIN_CHAR + 1)

    private val ENCODER_CACHE = arrayOfNulls<String>(256)

    private val OPB58_DIGIT_2_BIGINT = arrayOfNulls<BigInteger>(OPB58_DIGIT_2_INT.size)

    init {

        for (i in 0 until ALPHABET_SIZE) {
            val c = ALPHABET[i]
            val pos = c - ALPHABET_MIN_CHAR
            OPB58_DIGIT_2_INT[pos] = i
            OPB58_DIGIT_2_BIGINT[pos] = BigInteger.valueOf(i.toLong())
        }

        val b = ByteArray(1)
        for (i in 0..255) {
            b[0] = (i and 0xFF).toByte()
            ENCODER_CACHE[i] = internalEncode(b)
        }

    }

    @JvmStatic
    fun main(args: Array<String>) {

        val XSRNG = RandomKit.threadLocalXORShiftRNG
//        val XSRNG = ThreadLocalRandom.current()
        val bcount = 2
        var b = ByteArray(bcount)

        var st = System.currentTimeMillis()
        XSRNG.nextBytes(b)
        val rend = System.currentTimeMillis()
        println(
            "rnd: "
                    + (rend - st) * .001 / (bcount.toDouble() / 1024.0 / 1024.0 / 1024.0) + " s/GB"
        )

        val iter = 10000000.0

        var i = 0
        while (i < iter) {
            XSRNG.nextBytes(b)

            val ex = BigInteger(1, b)

            st = System.currentTimeMillis()
            var ee: String? = null
            val v = java.lang.Long.MAX_VALUE
            b = BigInteger.valueOf(255).toByteArray()
            run {
                var j = 0
                while (j < iter) {
                    ee = encode(b)
                    j++

                }
            }

            val etime = System.currentTimeMillis() - st
            println("to encode " + BigInteger(1, b) + " (" + ee + ")")
            println("encode: " + iter / etime)

            var bdecoded: ByteArray? = null

            st = System.currentTimeMillis()
            var j = 0
            while (j < iter) {
                bdecoded = decode(ee!!)
                j++
            }
            val dtime = System.currentTimeMillis() - st
            println("decode: " + iter / dtime)
            println("decoded: " + BigInteger(1, bdecoded!!))
            i++

        }

        System.exit(0)

    }

    fun encode(num: Long): String {

        if (num < 0) return num.toString()

        var num = num

        if (num < 256) {
            return ENCODER_CACHE[num.toInt()]!!
        }

        val result = LinkedList<Char>()

        while (num >= ALPHABET_SIZE) {
            val div = num / ALPHABET_SIZE
            val remainder = (num - ALPHABET_SIZE * div).toInt()
            result.addFirst(ALPHABET_CHARS[remainder])
            num = div
        }

        if (num > 0) {
            result.addFirst(ALPHABET_CHARS[num.toInt()])
        }

        val sb = StringBuilder(result.size)
        for (c in result) {
            sb.append(c)
        }
        result.clear()

        return sb.toString()

    }

    fun encode(data: ByteArray, offset: Int, len: Int): String {

        if (offset == 0 && len == data.size) {
            return encode(data)
        }

        val to = offset + len
        if (to > data.size) {
            throw ArrayIndexOutOfBoundsException()
        }

        return encode(Arrays.copyOfRange(data, offset, to))

    }

    fun encode(value: BigInteger): String {

        if (value.compareTo(BIGINT_BYTE_MAXVAL) <= 0) {
            return ENCODER_CACHE[value.toInt()]!!
        }

        if (value.compareTo(BIGINT_INTEGER_MAXVAL) <= 0) {
            return encode(value.toInt().toLong())
        }

        return if (value.compareTo(BIGINT_LONG_MAXVAL) <= 0) {
            encode(value.toLong())
        } else internalEncode(value)

    }

    fun encode(value: ByteArray?): String {

        if (value == null || value.isEmpty()) {
            return ""

        }

        if (value.size == 1) {
            return ENCODER_CACHE[(value[0].toInt() and 0xFF)]!!
        }

        if (value[0].toInt() == 0) {
            return encode(value, 1, value.size - 1)
        }

        if (value.size < 4) {

            if (value.size == 2) {
                return encode(value.twoBytes2Int().toLong())
            }
            if (value.size == 3) {
                return encode(value.threeBytes2Int().toLong())
            }

        }

        return if (value.size < 8) {
            encode(BigInteger(1, value).toLong())

        } else internalEncode(value)

    }

    private fun internalEncode(value: ByteArray): String {

        if (value.size == 1) {
            val digit = (value[0].toInt() and 0xFF).toChar()
            if (digit < ALPHABET_SIZE.toChar()) {
                return ALPHABET_CHARS[digit.toInt()].toString()
            }
        }

        return internalEncode(BigInteger(1, value))

    }

    private fun internalEncode(value: BigInteger): String {
        var value = value

        val result = LinkedList<Char>()

        while (value.compareTo(BIGINT_0) > 0) {
            val remainder = value.mod(ALPHABET_SIZE_AS_BIGINT).toInt()
            value = value.divide(ALPHABET_SIZE_AS_BIGINT)
            result.addFirst(ALPHABET_CHARS[remainder])
        }

        val sb = StringBuilder(result.size)
        for (c in result) {
            sb.append(c)
        }
        result.clear()

        return sb.toString()
    }

    fun fitsSignedInt(opb58String: String): Boolean {
        val slen = opb58String.length
        return slen < INTEGER_MAX_VALUE.length || slen == INTEGER_MAX_VALUE.length && opb58String.compareTo(
            INTEGER_MAX_VALUE
        ) <= 0

    }

    fun fitsSignedLong(opb58String: String): Boolean {
        val slen = opb58String.length
        return slen < LONG_MAX_VALUE.length || slen == LONG_MAX_VALUE.length && opb58String.compareTo(LONG_MAX_VALUE) <= 0

    }

    fun decodeToInt(opb58Number: String): Int {
        require(fitsSignedInt(opb58Number)) { "$opb58Number is grater than the maximum signed int value: $INTEGER_MAX_VALUE" }
        return unsafeDecodeToInt(opb58Number)
    }

    private fun unsafeDecodeToInt(opb58String: String): Int {
        var result = 0

        for (i in 0 until opb58String.length) {
            result = result * ALPHABET_SIZE + OPB58_DIGIT_2_INT[opb58String[i] - ALPHABET_MIN_CHAR]
        }

        return result
    }

    fun decodeToLong(opb58String: String): Long {
        require(fitsSignedLong(opb58String)) { "$opb58String is grater than the maximum signed long value: $LONG_MAX_VALUE" }
        return unsafeDecodeToLong(opb58String)
    }

    private fun unsafeDecodeToLong(opb58String: String): Long {
        var result: Long = 0

        for (i in 0 until opb58String.length) {
            result = result * ALPHABET_SIZE + OPB58_DIGIT_2_INT[opb58String[i] - ALPHABET_MIN_CHAR]
        }

        return result
    }

    // See http://www1.cs.columbia.edu/~zeph/Spring2001/3139/lecture4/FastBigInteger.java
    fun decode(opb58String: String): ByteArray {

        if (fitsSignedInt(opb58String)) {
            return unsafeDecodeToInt(opb58String).toByteArray()
        }

        if (fitsSignedLong(opb58String)) {
            return unsafeDecodeToLong(opb58String).toByteArray()
        }

        var intData = BIGINT_0

        for (i in 0 until opb58String.length) {
            intData = intData.multiply(ALPHABET_SIZE_AS_BIGINT).add(
                OPB58_DIGIT_2_BIGINT[opb58String[i] - ALPHABET_MIN_CHAR]
            )
        }

        return intData.toByteArray()

    }
}
