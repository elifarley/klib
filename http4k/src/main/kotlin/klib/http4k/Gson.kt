package klib.http4k
// TODO rename package to klib.http4k.gson ?

import com.google.gson.GsonBuilder
import klib.json.*
import org.http4k.format.ConfigurableGson
import java.time.ZonedDateTime

object ZonedDateTimeGson : ConfigurableGson(GsonBuilder()
//        .registerTypeAdapter(ZonedDateTime::class.java, zonedDateTimeSerializer)
//        .registerTypeAdapter(ZonedDateTime::class.java, zonedDateTimeDeserializer)
        .registerTypeAdapter(ZonedDateTime::class.java, zonedDateTimeTypeAdapter)
        .enableComplexMapKeySerialization().serializeNulls()
)

object JsonAnyGson : ConfigurableGson(GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, zonedDateTimeSerializer)
        .registerTypeAdapter(ZonedDateTime::class.java, zonedDateTimeDeserializer)
        .registerTypeAdapter(com.jsoniter.any.Any::class.java, JsonAnySerializer)
        .registerTypeAdapter(com.jsoniter.any.Any::class.java, JsonAnyDeserializer)
        .enableComplexMapKeySerialization().serializeNulls()
)
