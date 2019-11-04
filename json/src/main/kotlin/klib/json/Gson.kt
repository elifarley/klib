package klib.json

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.jsoniter.ValueType
import klib.base.asUTC_ISO_ZONED_DATE_TIME
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

typealias JsonAny = com.jsoniter.any.Any

val JsonAnySerializer = JsonSerializer<JsonAny> { value, type, context ->
    println("[JsonSerializer] Type: '$type'")
    when (value.valueType()!!) {
        ValueType.NULL -> JsonNull.INSTANCE
        ValueType.NUMBER -> JsonPrimitive(value.toLong())
        ValueType.BOOLEAN -> JsonPrimitive(value.toBoolean())
        ValueType.STRING -> JsonPrimitive(value.toString())
        ValueType.ARRAY -> context.serialize(value.`as`(List::class.java))
        ValueType.OBJECT -> context.serialize(value.`as`(Map::class.java))
        ValueType.INVALID -> throw JsonSyntaxException("Invalid JSON: '$value'")
    }
}

val JsonAnyDeserializer = JsonDeserializer<JsonAny> { value, type, context ->
    println("[JsonDeserializer] Type: '$type'")
    when {
        value.isJsonNull -> JsonAny.wrap(null)
        value.isJsonPrimitive -> value.asJsonPrimitive.let {
            when {
                it.isNumber -> JsonAny.wrap(it.asNumber)
                it.isBoolean -> JsonAny.wrap(it.asBoolean)
                else -> JsonAny.wrap(it.asString)
            }
        }
        value.isJsonArray -> context.deserialize(value.asJsonArray, type) // TODO
        value.isJsonObject -> context.deserialize(value.asJsonObject, type) // TODO
        else -> throw IllegalArgumentException("Unable to deserialize '$value'")
    }
}

val zonedDateTimeSerializer = JsonSerializer<ZonedDateTime?> { value, type, context ->
    context.serialize(value?.asUTC_ISO_ZONED_DATE_TIME)
}

val zonedDateTimeDeserializer = JsonDeserializer<ZonedDateTime?> { value, type, context ->
    when {
        value.isJsonNull -> null
        value.isJsonPrimitive && value.asJsonPrimitive.isString ->
            ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(
                    value.asJsonPrimitive.asString
            ))
        else -> throw IllegalArgumentException(value.toString())
    }
}

val zonedDateTimeTypeAdapter: TypeAdapter<ZonedDateTime?> = object : TypeAdapter<ZonedDateTime?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: ZonedDateTime?) {
        out.value(value?.asUTC_ISO_ZONED_DATE_TIME)
    }

    @Throws(IOException::class)
    override fun read(jsonReader: JsonReader): ZonedDateTime? {
        return when (jsonReader.peek()) {
            JsonToken.NULL -> {
                jsonReader.nextNull(); null
            }
            JsonToken.STRING -> ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(jsonReader.nextString()))
            else -> throw IllegalArgumentException(jsonReader.peek().toString())
        }
    }
}
