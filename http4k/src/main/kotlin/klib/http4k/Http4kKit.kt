package klib.http4k

import klib.base.fmt
import klib.base.simpleMessage
import org.http4k.core.*
import org.http4k.filter.ServerFilters
import org.http4k.lens.Failure
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import org.http4k.lens.string
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.SocketException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// TODO Remove this line once the PR gets released
fun Header.zonedDateTime(formatter: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME) =
    this.map({ ZonedDateTime.parse(it, formatter) }, formatter::format)

// TODO Remove this line once the PR gets released: https://github.com/http4k/http4k/pull/163
object SetHostFrom {
    operator fun invoke(uri: Uri): Filter = Filter { next ->
        {
            next(it.uri(it.uri.scheme(uri.scheme).host(uri.host).port(uri.port)).replaceHeader("Host", "${uri.host}${
            uri.port?.let { port -> ":$port" } ?: ""
            }"))
        }
    }
}

/* HeaderLens<ZonedDateTime> */
val headerLastModified = Header
    .zonedDateTime(DateTimeFormatter.RFC_1123_DATE_TIME)
    .required("last-modified", "Last modification date")

val headerLocation = Header
    .string().required("location", "Location")

val Pair<String, HttpHandler>.toAutoSetHost
    get() = SetHostFrom(Uri.of(this.first)).then(this.second)

fun <T> HttpHandler.performOrNull(request: Request, responseLens: (HttpMessage) -> T): Pair<Status, T?> =
    performInternal(request, true, responseLens)

fun <T> HttpHandler.perform(request: Request, responseLens: (HttpMessage) -> T): Pair<Status, T> =
    this.performInternal(request, false, responseLens).let { it.first to it.second!! }

private fun <T> HttpHandler.performInternal(
    request: Request,
    nullWhenNotFound: Boolean = false,
    responseLens: (HttpMessage) -> T
)
        : Pair<Status, T?> = try {
    this(request)
        .let {

            if (nullWhenNotFound && it.status == Status.NOT_FOUND) {
                return it.status to null
            }

            if (it.status.code >= 400) {
                throw RemoteSystemProblem(
                    "${request.method} ${request.uri} (${request.body})",
                    it.status, it.body, Header.CONTENT_TYPE(it)
                )
            }

            try {
                it.status to responseLens(it)

            } catch (e: LensFailure) {
                throw RemoteSystemProblem(
                    "Unable to parse response", request.uri.toString(), e, Status.INTERNAL_SERVER_ERROR,
                    it.body, Header.CONTENT_TYPE(it)
                )
            }

        }
} catch (e: SocketException) {
    throw RemoteSystemProblem(
        "SocketException on '${request.method}': ${e.message}",
        request.uri.toString(),
        e,
        Status.BAD_GATEWAY
    )

}

class RemoteSystemProblem : Exception {
    val status: Status
    val body: Body?
    val contentType: ContentType?

    constructor(
        msg: String, remoteURI: String, cause: Throwable, localStatus: Status = Status.INTERNAL_SERVER_ERROR,
        body: Body? = null, contentType: ContentType? = null
    )
            : super("$msg ($remoteURI)", cause) {
        status = localStatus
        this.body = body
        this.contentType = contentType
    }

    constructor(remoteURI: String, remoteStatus: Status, body: Body? = null, contentType: ContentType? = null)
            : super(
        "$remoteStatus${
        body?.let { " (Body: $body);" }.orEmpty()
        } $remoteURI"
    ) {
        this.status = remoteStatus
        this.body = body
        this.contentType = contentType
    }

}

fun RemoteSystemProblem.asResponse(description: String? = null) = Response(
    description?.let { status.description(it + status.description.fmt(" [%s]")) } ?: status).let { res ->
    body?.let { res.body(it) } ?: res.with(
        Body.string(contentType ?: ContentType.TEXT_PLAIN, "Exception").toLens() of toString()
    )
}

fun Status.withNested(nested: Status, e: Exception?) =
    this.description("${e?.javaClass?.simpleName ?: ""} (Nested: $nested)")

fun Exception.toStackTraceResponse(status: Status = Status.INTERNAL_SERVER_ERROR): Response {
    val exceptionBodyMod: (Response) -> Response = (Body.string(ContentType.TEXT_PLAIN, "Exception").toLens()
            of "$simpleMessage\n\n" +
            ByteArrayOutputStream().also { baos ->
                PrintStream(baos).also {
                    printStackTrace(it)
                    it.close()
                }
            }.toString())
    return Response(status.description(javaClass.simpleName)).with(exceptionBodyMod)
}

val CatchLensFailureWithCause: Filter = ServerFilters.CatchLensFailure {
    Response(
        Status.BAD_REQUEST.description(
            it.failures.joinToString("; ")
                    + it.cause.fmt(" (${it.javaClass.simpleName})")
        )
    ).with(
        Body.string(ContentType.TEXT_PLAIN).toLens() of it.cause?.toString().orEmpty()
    )
}

val LensFailure.asDetailedLensFailure
    get() = DetailedLensFailure(failures, cause, target)

open class DetailedLensFailure(
    val failures: List<Failure>,
    override val cause: Exception? = null,
    val target: Any? = null
) : Exception(failures.joinToString { "$it" } + (cause?.let { " ($it)" }.orEmpty()), cause) {

    constructor(vararg failures: Failure, cause: Exception? = null, target: Any? = null)
            : this(failures.asList(), cause, target)

    fun overall(): Failure.Type =
        with(failures.map { it.type }) {
            when {
                contains(Failure.Type.Unsupported) -> Failure.Type.Unsupported
                isEmpty() || contains(Failure.Type.Invalid) -> Failure.Type.Invalid
                else -> Failure.Type.Missing
            }
        }
}
