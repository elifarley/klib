package klib.base.path

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileAttribute

/**
 * Created by elifarley on 02/09/16.
 */

fun Path.createParentDirectories(vararg attrs: FileAttribute<*>) = this.apply {
    Files.createDirectories(this.parent, *attrs)
}
