package klib.base

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.Closeable
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject

/**
 * See http://stackoverflow.com/questions/34416869/idiomatic-way-of-logging-in-kotlin
 */

// Return logger for Java class, if companion object fix the name
inline fun <T : Any> logger(forClass: Class<T>): Logger = LoggerFactory.getLogger(unwrapCompanionClass(forClass).name)

// Return logger for Kotlin class
inline fun <T : Any> logger(forClass: KClass<T>): Logger = logger(forClass.java)

// unwrap companion class to enclosing class given a Java Class
fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> =
    if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }

// unwrap companion class to enclosing class given a Kotlin Class
inline fun <T : Any> unwrapCompanionClass(ofClass: KClass<T>): KClass<*> = unwrapCompanionClass(ofClass.java).kotlin

// return a lazy logger property delegate for enclosing class
inline fun <R : Any> R.lazyLogger(): Lazy<Logger> = lazy { logger(this.javaClass) }

/**
 * Base class to provide logging.
 * Intended for companion objects more than classes but works for either.
 * Usage example: `companion object: WithLogging() {}`
 */
abstract class WithLogging {
    val log: Logger by lazyLogger()
}

class MDCCloseable : Closeable {

    private val keys = mutableSetOf<String>()

    @JvmOverloads
    fun putAll(map: Map<out String, Any?>?, prefix: String? = null): MDCCloseable {
        map?.forEach {
            MDC.put(it.key.prefixWith(prefix).apply { keys.add(this) }, it.value.toString())
        }
        return this
    }

    fun put(key: String, value: Any?): MDCCloseable {
        MDC.put(key.apply { keys.add(this) }, value.toString())
        return this
    }

    inline fun get(key: String): String? = MDC.get(key)
    fun clear() {
        keys.forEach { MDC.remove(it) }; keys.clear()
    }

    fun remove(key: String): Boolean = key.let { MDC.remove(it); keys.remove(it) }

    override fun close() = clear()

}
