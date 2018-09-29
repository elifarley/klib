package klib.base.math

import kotlin.experimental.and

object BitKit {

    fun Int.toByteArray(): ByteArray =
            byteArrayOf(this.ushr(24).toByte(), this.ushr(16).toByte(), this.ushr(8).toByte(), this.toByte())

    fun Long.toByteArray(): ByteArray =
            byteArrayOf(this.ushr(56).toByte(), this.ushr(48).toByte(), this.ushr(40).toByte(), this.ushr(32).toByte(), this.ushr(24).toByte(), this.ushr(16).toByte(), this.ushr(8).toByte(), this.toByte())

    fun ByteArray.twoBytes2Int(): Int {
        return this[0].and(0xFF.toByte()).toInt() shl (8) or (this[1] and 0xFF.toByte()).toInt()
    }

    fun ByteArray.threeBytes2Int(): Int =
            (this[0] and 0xFF.toByte()).toInt() shl 16 or ((this[1] and 0xFF.toByte()).toInt() shl 8) or (this[2] and 0xFF.toByte()).toInt()

    fun ByteArray.fourBytes2Long(): Long =
            ((this[0] and 0xFF.toByte()).toInt() shl 24 or ((this[1] and 0xFF.toByte()).toInt() shl 16)
                    or ((this[2] and 0xFF.toByte()).toInt() shl 8) or (this[3] and 0xFF.toByte()).toInt()).toLong()

}
