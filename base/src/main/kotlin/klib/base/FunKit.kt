package klib.base

/**
 * Created by elifarley on 22/11/16.
 */

inline fun Any?.nullIf(nullValue: Any) = this.takeUnless { this == nullValue }

/**
 * Calls the specified function [block] with no argument and always returns `Unit`.
 */
inline fun unit(block: () -> Any?) {
    block()
}

/**
 * Calls the specified function [block] with `this` value as its argument and always returns `Unit`.
 */
inline fun <T> T.unit(block: (T) -> Any?) {
    block(this)
}

/**
 * Can be used to transform a `when` receiver into an expression, making it **exhaustive**.
 * @return `Unit`
 */
val Any?.unit inline get() = Unit

inline operator fun <T> Boolean?.rem(block: () -> T): T? = this?.let { if (this) block() else null }

inline infix operator fun <A, B, C> ((A) -> B).rem(crossinline other: (B) -> C): (A) -> C =
    { other(this(it)) }
