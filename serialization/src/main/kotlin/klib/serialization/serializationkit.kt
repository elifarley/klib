package klib

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.math.BigInteger

object SerializationKit {
    val jacksonObjectMapper = jacksonObjectMapper().apply {
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
    }

    inline fun <reified T> decodeValue(map: Map<String, String>): T =
        jacksonObjectMapper.convertValue(map.decodeMap(), T::class.java)

    fun <T> decodeValue(map: Map<String, String>, clazz: Class<T>): T = map.decodeMap().let {
        jacksonObjectMapper.convertValue(it, clazz)
    }

    fun encodeToMap(obj: Any): Map<String, String> =
        jacksonObjectMapper.convertValue(obj, object : TypeReference<Map<String, Any>>() {}).encodeMap()
    /* TODO Use native Kotlin serialization
            Json.encodeToString(obj).let{
                Json.parseToJsonElement(it).jsonObject.toMap().encodeMap()
            }
    */

}

private fun Map<String, Any>.encodeMap(): Map<String, String> = encode("", this).toMap()

fun Map<String, String>.decodeMap(): Map<String, Any> = mutableMapOf<String, Any>().also { result ->
    forEach { (key, value) ->
        buildStructure(key.split('.'), decodeValue(value), result)
    }
}

@Suppress("UNCHECKED_CAST")
private fun encode(prefix: String, value: Any): Sequence<Pair<String, String>> = sequence {
    when (value) {
        is Map<*, *> -> {
            for ((key, v) in value as Map<String, Any>) {
                val newPrefix = if (prefix.isEmpty()) key else "$prefix.$key"
                yieldAll(encode(newPrefix, v))
            }
        }

        is Iterable<*> -> {
            value.forEachIndexed { index, v ->
                if (v != null) {
                    val newPrefix = if (prefix.isEmpty()) index.toString() else "$prefix.$index"
                    yieldAll(encode(newPrefix, v))
                }
            }
        }

        else -> yield(prefix to encodeValue(value))
    }
}

@Suppress("UNCHECKED_CAST")
private fun buildStructure(keys: List<String>, value: Any, current: MutableMap<String, Any>) {
    if (keys.size == 1) {
        current[keys[0]] = value
        return
    }
    val key = keys[0]
    val next = current.getOrPut(key) {
        if (keys[1].toIntOrNull() != null) mutableListOf<Any?>() else mutableMapOf<String, Any>()
    }
    when (next) {
        is MutableList<*> -> {
            val index = keys[1].toInt()
            val list = next as MutableList<Any?>
            while (list.size <= index) list.add(null)
            if (keys.size == 2) {
                list[index] = value
            } else {
                if (list[index] !is MutableMap<*, *>) {
                    list[index] = mutableMapOf<String, Any>()
                }
                buildStructure(keys.drop(2), value, list[index] as MutableMap<String, Any>)
            }
        }

        is MutableMap<*, *> -> buildStructure(keys.drop(1), value, next as MutableMap<String, Any>)
        else -> throw IllegalStateException("Unexpected type: ${next::class.simpleName}")
    }
}

@Suppress("CyclomaticComplexMethod")
private fun encodeValue(value: Any): String = when (value) {
    is String -> when {
        value == "true" || value == "false" || value.firstOrNull()?.run {
            isDigit() || this == '-' || this == '+' || this == '!'
        } == true -> "!s$value"

        else -> value
    }

    is Boolean -> value.toString()
    is Byte -> "!y$value"
    is UByte -> "!Y$value"
    is Int -> "!i$value"
    is UInt -> "!I$value"
    is Long -> "!l$value"
    is ULong -> "!L$value"
    is Float -> "!f$value"
    is Double -> "!d$value"
    is BigInteger -> "!+I$value"
    is BigDecimal -> "!+D$value"
    else -> throw IllegalArgumentException("Unsupported type: ${value::class.simpleName}")
}

@Suppress("CyclomaticComplexMethod", "MagicNumber")
private fun decodeValue(value: String): Any = when {
    value.isEmpty() -> value
    value.length == 1 -> value
    value == "true" -> true
    value == "false" -> false
    value.first() == '!' -> when (value[1]) {
        's' -> value.substring(2)
        'y' -> value.substring(2).toByte()
        'Y' -> value.substring(2).toUByte()
        'i' -> value.substring(2).toInt()
        'I' -> value.substring(2).toUInt()
        'l' -> value.substring(2).toLong()
        'L' -> value.substring(2).toULong()
        'f' -> value.substring(2).toFloat()
        'd' -> value.substring(2).toDouble()
        '+' -> when {
            value.length < 4 -> value
            else -> when (value[2]) {
                'I' -> value.substring(3).toBigInteger()
                'D' -> value.substring(3).toBigDecimal()
            }
        }
        else -> throw IllegalArgumentException("Unknown type prefix in: $value")
    }

    value.first().run { this.isDigit() || this in setOf('-', '+') } -> when {
        value.toByteOrNull() != null -> value.toByte()
        value.toUByteOrNull() != null -> value.toUByte()
        value.toShortOrNull() != null -> value.toShort()
        value.toUShortOrNull() != null -> value.toUShort()
        value.toIntOrNull() != null -> value.toInt()
        value.toUIntOrNull() != null -> value.toUInt()
        value.toLongOrNull() != null -> value.toLong()
        value.toDoubleOrNull() != null -> value.toDouble()
        value.toBigIntegerOrNull() != null -> value.toBigInteger()
        value.toBigDecimalOrNull() != null -> value.toBigDecimal()
        else -> value // It's a number-like string without prefix
    }

    else -> value // It's a string without prefix
}
