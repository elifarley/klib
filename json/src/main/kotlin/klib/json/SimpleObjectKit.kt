package klib.json

import com.jsoniter.annotation.JsonObject
import com.jsoniter.annotation.JsonProperty
import klib.base.fmt
import java.time.ZonedDateTime

interface ISimpleObject<T> {
    val id: T?
    val attrs: Map<String, JsonAny?>?
    val created: ZonedDateTime?
    val updated: ZonedDateTime?

    operator fun get(key: String): JsonAny? = attrs!![key]
    infix operator fun plus(p: Pair<String, JsonAny?>): ISimpleObject<T>
//    override fun toString(): String
}

@JsonObject(asExtraForUnknownProperties = true)
data class SimpleObject<T>(
    @JsonProperty("id") override val id: T,
    override val attrs: Map<String, JsonAny?>,
    @JsonProperty("created") override val created: ZonedDateTime? = null,
    @JsonProperty("updated") override val updated: ZonedDateTime? = null
) : ISimpleObject<T> {

    companion object {
        val dummy get() = SimpleObject("", mapOf(), ZonedDateTime.now(), ZonedDateTime.now())
    }

    override infix operator fun plus(p: Pair<String, JsonAny?>) = copy(attrs = attrs.plus(p))

    override fun toString(): String {
        return "SimpleObject(id='$id', attrs=$attrs" +
                "${created.fmt(", created=%s")}${if (created == updated) "" else ", updated=$updated"})"
    }
}
