package klib.db

import com.google.gson.JsonElement
import com.jsoniter.JsonIterator
import com.jsoniter.output.JsonStream
import klib.http4k.RemoteSystemProblem
import klib.http4k.headerLastModified
import klib.http4k.perform
import klib.http4k.performOrNull
import klib.json.SimpleObject
import org.http4k.contract.RouteBinder
import org.http4k.contract.bindContract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.format.Gson.json
import org.http4k.lens.BiDiPathLens
import org.http4k.lens.BodyLens
import org.http4k.lens.Header
import org.http4k.lens.string
import java.time.ZonedDateTime

interface ISimpleObjectRestClient<PK> {
    val objIdPath: BiDiPathLens<PK>
    fun head(id: PK): ZonedDateTime?
    operator fun get(id: PK): SimpleObject<PK>?
    fun post(obj: SimpleObject<PK>): String // TODO Return PK
}

abstract class SimpleObjectRestClient<PK>(objPath: String, private val client: HttpHandler) :
    ISimpleObjectRestClient<PK> {

    private val simpleObjectBody: BodyLens<JsonElement> = Body.json().toLens()
    private val routePOST = objPath meta { body = simpleObjectBody } bindContract Method.POST
    private val routeHEAD: RouteBinder<(PK) -> HttpHandler> by lazy { objPath / objIdPath bindContract Method.HEAD }
    private val routeGET: RouteBinder<(PK) -> HttpHandler> by lazy { objPath / objIdPath bindContract Method.GET }

    override fun head(id: PK): ZonedDateTime? =
        client.performOrNull(
            routeHEAD.newRequest()
                .with(objIdPath of id), headerLastModified
        ).let {

            if (it.first !in listOf(Status.OK, Status.NOT_FOUND)) {
                throw RemoteSystemProblem("Unexpected status", it.first)
            }

            it.second
        }

    override operator fun get(id: PK): SimpleObject<PK>? =
        client.performOrNull(
            routeGET.newRequest()
                .with(objIdPath of id), simpleObjectBody
        ).let {

            if (it.first !in listOf(Status.OK, Status.NOT_FOUND)) {
                throw RemoteSystemProblem("Unexpected status", it.first)
            }

            it.second?.let {
                JsonIterator.deserialize(it.toString(), SimpleObject::class.java) as SimpleObject<PK>
            }
        }

    /**
     * @return ID of inserted instance
     */
    override fun post(obj: SimpleObject<PK>): String = try {

        val cReq = routePOST.newRequest().with(
            Body.string(ContentType.APPLICATION_JSON).toLens() of JsonStream.serialize(obj)
        )
        client.perform(cReq, SimpleObjectPOST.response)

    } catch (e: RemoteSystemProblem) {

        if (e.status == Status.CONFLICT) {
            throw e.asDuplicateKeyException
        }

        throw e

    } catch (e: Exception) {
        throw e

    }.let {

        if (it.first != Status.CREATED) {
            throw RemoteSystemProblem("Unexpected status: ${it.first}", it.first)
        }

        it.second
    }

    companion object {

        object SimpleObjectPOST {
            val response = Header.string().required("id", "ID of this instance")
        }

    }
}

object SimpleRepoExceptions {

    class DuplicateKeyException : Exception {

        val body: String?
        val contentType: String?

        constructor(message: String, body: String? = null, contentType: String? = null) : super(message) {
            this.body = body
            this.contentType = contentType
        }

        constructor(message: String, cause: Throwable, body: String? = null, contentType: String? = null) : super(
            message,
            cause
        ) {
            this.body = body
            this.contentType = contentType
        }

    }

}

val RemoteSystemProblem.asDuplicateKeyException: SimpleRepoExceptions.DuplicateKeyException
    get() = if (this.status != Status.CONFLICT) throw IllegalArgumentException("[asDuplicateKeyException] Invalid status: $status")
    else SimpleRepoExceptions.DuplicateKeyException(this.message.orEmpty(), this, body?.toString(), contentType?.value)
