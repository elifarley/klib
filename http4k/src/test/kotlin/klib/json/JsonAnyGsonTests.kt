package klib.json

import com.jsoniter.JsonIterator
import klib.base.asUTC_ISO_ZONED_DATE_TIME
import klib.http4k.JsonAnyGson
import klib.http4k.ZonedDateTimeGson
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.time.ZonedDateTime
import klib.http4k.ZonedDateTimeGson.auto as zdtAuto

/**
 * -Djson-unit.libraries=gson
 */
class JsonAnyGsonTests {

    val jsonStr = """
                {"i": 0, "s": "str", "a": [1, 2, 3], "o": {"n1": 5}}
                """

    val dataJsonIter = JsonIterator.deserialize(jsonStr).asMap()

    @Before
    fun before() {
    }

    @After
    fun after() {
    }

    @Test
    fun `Zoned Date Time TO String`() {
        val zdt = ZonedDateTime.now()
        val zdtStrExpected = zdt.asUTC_ISO_ZONED_DATE_TIME
        val zdtStr = ZonedDateTimeGson.compact(ZonedDateTimeGson.asJsonObject(zdt))
        println(zdtStrExpected)
        println(zdt.toString())
        JsonFluentAssert.assertThatJson(zdtStr)
            .isEqualTo(zdtStrExpected)
    }

    @Test
    @Ignore
    fun `Zoned Date Time FROM String`() {
        val zdtBody = Body.zdtAuto<ZonedDateTime>().toLens()
        val zdtExpected = ZonedDateTime.now()
        val zdtStr = "\"${zdtExpected.asUTC_ISO_ZONED_DATE_TIME}\""
        val zdt = Response(Status.OK).with(zdtBody of zdtExpected)
        zdtBody(zdt)
        println("body str: " + zdt.bodyString())
        println("zdtStr: " + zdtStr)
        JsonFluentAssert.assertThatJson(zdt)
            .isEqualTo(zdtExpected)
    }

    @Test
    @Ignore
    fun `Long`() {
        val transf = JsonAnyGson.compact(JsonAnyGson.asJsonObject(JsonAny.wrap(4)))
        JsonFluentAssert.assertThatJson(transf)
            .isEqualTo("4")
    }

    @Test
    @Ignore
    fun `dry run`() {
        val transf = JsonAnyGson.compact(JsonAnyGson.asJsonObject(dataJsonIter))
        JsonFluentAssert.assertThatJson(transf)
            .isEqualTo(jsonStr)
    }

}
