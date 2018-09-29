package klib.json

import com.jsoniter.JsonIterator
import com.jsoniter.output.JsonStream
import klib.json.jsoniter.codec.JsonIterCodec
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * -Djson-unit.libraries=gson
 */
class SimpleObjectTests {

    companion object {
        @BeforeClass @JvmStatic fun beforeClass() {
            JsonIterCodec.registerUtcIsoZonedDateTime()
            JsonIterCodec.registerSimpleObject()
        }
    }

    private val now = ZonedDateTime.now()

    private val expectedJsonStr = """
                {"id": ${Long.MIN_VALUE}, "created": "${DateTimeFormatter.ISO_ZONED_DATE_TIME.format(now)}", "updated": "${DateTimeFormatter.ISO_ZONED_DATE_TIME.format(now)}",
                   "misc": {"num1": 5, "ar": [1, "2", {}], "ob": {"o1": "o1val"}},
                   "email": "em@il.com"
                 }
                """
    private val expectedSimpleObject = SimpleObject(Long.MIN_VALUE,
            mapOf(
            "misc" to JsonIterator.deserialize("""
                {"num1": 5, "ar": [1, "2", {}], "ob": {"o1": "o1val"}}
                """),
            "email" to JsonIterator.deserialize("\"em@il.com\"")
    ),
            now, now)

    @Before
    fun before(){
    }

    @After
    fun after(){
    }

    @Test
    fun `Deserialize`() {
//        val clazz = SimpleObject::class.java  - Also works
        val clazz = ISimpleObject::class.java
        val actual = JsonIterator.deserialize(expectedJsonStr, clazz)

        println("FROM String:\n${expectedJsonStr.trim()}")
        println("TO object  : $actual (${actual.id!!.javaClass.simpleName})")
        assertThat(actual).isEqualToComparingFieldByFieldRecursively(expectedSimpleObject)
    }

    @Test
    fun `Serialize (TO String)`() { // TODO Unwrap attrs
        val actual = JsonStream.serialize(expectedSimpleObject)

        println("TO string: $actual")
        JsonFluentAssert.assertThatJson(actual)
                .isEqualTo(expectedJsonStr)

    }

    @Test
    @Ignore
    fun `Serialize (Wrap)`() { // FIXME Unwrap attrs
//        Expected :<[created, email, id, misc, updated]>
//        Actual   :<[attrs, created, id, updated]>. Missing: "email","misc" Extra: "attrs"

        val actual = JsonStream.serialize(JsonAny.wrap(expectedSimpleObject))

        println("TO Wrapped: $actual")
        JsonFluentAssert.assertThatJson(actual)
                .isEqualTo(expectedJsonStr)

    }

}
