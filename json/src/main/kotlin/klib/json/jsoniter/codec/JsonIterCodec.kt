package klib.json.jsoniter.codec

import com.jsoniter.JsonIterator
import com.jsoniter.ValueType
import com.jsoniter.output.JsonStream
import com.jsoniter.spi.Encoder
import com.jsoniter.spi.JsonException
import com.jsoniter.spi.JsoniterSpi
import klib.base.asUTC_ISO_ZONED_DATE_TIME
import klib.json.ISimpleObject
import klib.json.JsonAny
import klib.json.SimpleObject
import java.io.IOException
import java.text.ParseException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object JsonIterCodec {

    fun registerUtcIsoZonedDateTime() {

        JsoniterSpi.registerTypeEncoder(ZonedDateTime::class.java, object : Encoder.ReflectionEncoder {
            @Throws(IOException::class)
            override fun encode(obj: kotlin.Any?, stream: JsonStream) {
                stream.writeVal(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(obj as ZonedDateTime))
            }

            override fun wrap(obj: kotlin.Any?) = JsonAny.wrap(
                (obj as ZonedDateTime).asUTC_ISO_ZONED_DATE_TIME
            )
        })

        JsoniterSpi.registerTypeDecoder(ZonedDateTime::class.java) { iter ->
            try {
                ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(iter.readString()))
            } catch (e: ParseException) {
                throw JsonException(e)
            }
        }
    }

    fun registerSimpleObject() {
        JsoniterSpi.registerTypeImplementation(ISimpleObject::class.java, SimpleObject::class.java)
        JsoniterSpi.registerTypeEncoder(ISimpleObject::class.java, simpleObjectEncoder)
        JsoniterSpi.registerTypeEncoder(SimpleObject::class.java, simpleObjectEncoder)
        JsoniterSpi.registerTypeDecoder(ISimpleObject::class.java, simpleOjbectDecoder)
        JsoniterSpi.registerTypeDecoder(SimpleObject::class.java, simpleOjbectDecoder)
    }

    // Unwrapper / Deserializer
    private val simpleOjbectDecoder: (JsonIterator) -> SimpleObject<Any?> = { iter ->
        try {
            iter.readAny().asMap().let { root ->
                val id = root["id"]
                val created = root["created"]?.`as`(ZonedDateTime::class.java)
                val updated = root["updated"]?.`as`(ZonedDateTime::class.java)
                root.remove("id"); root.remove("created"); root.remove("updated")
                SimpleObject(
                    when (id?.valueType()) {
                        null, ValueType.NULL -> null
                        ValueType.NUMBER -> id.toLong()
                        else -> id.toString()
                    }, root, created, updated
                )
            }
        } catch (e: Exception) {
            throw JsonException(e)
        }
    }

    // Wrapper / Serializer
    private val simpleObjectEncoder = object : Encoder.ReflectionEncoder {
        @Throws(IOException::class)
        override fun encode(obj: Any?, stream: JsonStream) = (obj as ISimpleObject<*>).let { obj ->
            stream.writeObjectStart()
            stream.writeObjectField("id")
            obj.id.let {
                when (it) {
                    is String -> stream.writeVal(it)
                    is Long -> stream.writeVal(it)
                    is Int -> stream.writeVal(it)
                    else -> stream.writeVal(it)
                }
            }

            stream.writeMore()
            stream.writeObjectField("created")
            stream.writeVal(obj.created)

            stream.writeMore()
            stream.writeObjectField("updated")
            stream.writeVal(obj.updated)

            for (e in obj.attrs?.entries ?: emptySet()) {
                stream.writeMore()
                stream.writeObjectField(e.key)
                stream.writeVal(e.value)
            }
            stream.writeObjectEnd()
        }

        override fun wrap(obj: Any?) = (obj as ISimpleObject<*>).let { obj ->
            val base = mapOf<String, JsonAny>(
                "id" to obj.id.let {
                    when (it) {
                        is String -> JsonAny.wrap(it)
                        is Long -> JsonAny.wrap(it)
                        is Int -> JsonAny.wrap(it)
                        else -> JsonAny.wrap(it)
                    }
                },
                "created" to JsonAny.wrap(obj.created),
                "updated" to JsonAny.wrap(obj.updated)
            )

            JsonAny.wrap(obj.attrs?.plus(base) ?: base)
        }
    }
}