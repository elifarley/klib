package klib.base.io

import java.io.Closeable
import java.security.MessageDigest

/**
 * Created by elifarley on 29/11/16.
 */

abstract class IOWithDigest(private val md: MessageDigest) : Closeable {

    val digest: ByteArray get() = md.digest()
    val digestLength: Int get() = md.digestLength
    val algorithm: String get() = md.algorithm

    abstract class MDReader(md: MessageDigest) : IOWithDigest(md) {
        abstract fun read(): Any?
    }

    abstract class MDWriter(md: MessageDigest) : IOWithDigest(md) {
        abstract fun write(o: Any)
        abstract fun flush()
    }

}
