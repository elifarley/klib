package klib.base.io

import java.io.File
import java.security.DigestInputStream
import java.security.DigestOutputStream
import java.security.MessageDigest

/**
 * Created by elifarley on 02/09/16.
 */

inline fun File.digestInputStream(digest: MessageDigest = MessageDigest.getInstance("MD5")) =
        DigestInputStream(this.inputStream(), digest)

inline fun File.digestOutputStream(digest: MessageDigest = MessageDigest.getInstance("MD5")) =
        DigestOutputStream(this.outputStream(), digest)
