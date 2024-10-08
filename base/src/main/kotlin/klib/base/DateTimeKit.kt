package klib.base

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAccessor
import java.util.*

/**
 * Created by elifarley on 31/08/16.
 */

val ZonedDateTime.asUTC_ISO_ZONED_DATE_TIME: String
    get() = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(this.withZoneSameInstant(ZoneOffset.UTC))

fun compareTruncatedToSeconds(a: LocalDateTime, b: LocalDateTime) = ChronoUnit.SECONDS.between(a, b)

fun compareTruncatedToDays(a: LocalDate, b: LocalDate) = ChronoUnit.DAYS.between(a, b)

object DateTimeKit {

    fun LocalDateTime.toZoneOffset(zoneId: ZoneId? = null): ZoneOffset {

        val lzid = zoneId ?: ZoneId.systemDefault()

        val trans = lzid.rules.getTransition(this) ?:
        // Normal case: only one valid offset
        return lzid.rules.getOffset(this)

        // Gap or Overlap: determine what to do from transition
        logger(javaClass).warn("[toZoneOffset] {}", trans.toString(), RuntimeException("Timezone transition found"))

        return lzid.rules.getOffset(this)

    }

    fun Instant.plusYears(y: Long) = this.toLocalDateTime(ZoneId.of("+0")).plusYears(y).toInstant(ZoneOffset.UTC)

    fun Instant.plusMonths(y: Long) = this.toLocalDateTime(ZoneId.of("+0")).plusMonths(y).toInstant(ZoneOffset.UTC)

    @JvmOverloads
    fun TemporalAccessor.toDate(zoneId: ZoneId? = null) = LocalDateTime.from(this).toDate(zoneId)

    @JvmOverloads
    fun LocalDate.toDate(zoneId: ZoneId? = null) = this.atStartOfDay().toDate(zoneId)

    @JvmOverloads
    fun LocalDateTime.toDate(zoneId: ZoneId? = null): Date {
        return Date.from(this.toInstant(this.toZoneOffset(zoneId)))!!
    }

    @JvmOverloads
    fun LocalDateTime.toInstant(zoneId: ZoneId? = null): Instant {
        return this.toInstant(this.toZoneOffset(zoneId))!!
    }

    @JvmOverloads
    fun Instant.toLocalDateTime(zoneId: ZoneId? = null) =
        LocalDateTime.ofInstant(this, zoneId ?: ZoneId.systemDefault())!!

    @JvmOverloads
    fun Date.toLocalDateTime(zoneId: ZoneId? = null) = when {
        // java.sql.Date doesn't have time information, so we need to create a java.util.Date instance out of it
        this is java.sql.Date -> Date(this.time).toInstant().toLocalDateTime(zoneId) // TODO this.toInstant() ?
        else -> this.toInstant().toLocalDateTime(zoneId)
    }

    val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss").withZone(ZoneId.systemDefault())!!

    /**
     * @param zoneId When null, 'ZoneId.systemDefault()' will be used. When absent, 'America/Sao_Paulo'
     * @return Now formatted as yyyyMMdd
     */
    inline fun nowAsISODate(zoneId: String? = "America/Sao_Paulo") =
        ZonedDateTime.now(zoneId?.let { ZoneId.of(zoneId) } ?: ZoneId.systemDefault()).let {
            DateTimeFormatter.BASIC_ISO_DATE.format(it).takeWhile { it != '-' }
        }

    fun DateTimeFormatter.format(date: Date) = this.format(date.toInstant())!!

}

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import klib.base.ShortString.asShortString
import klib.base.ShortString.shortStringAsLong
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

private val dateTimeFormatterUTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

@Suppress("MagicNumber")
object ShortString {
    val Long.asShortString get() = toString(36).padStart(6, '0')
    val String.shortStringAsLong get() = toLong(36)
}

@Suppress("MagicNumber")
@JvmInline
value class InstantWithDuration(private val packedValue: Long) {

    init {
        require(packedValue >= 0) { "PackedValue must be non-negative" }
        require(packedValue shr (63 - BITS_FOR_DURATION) == 0L) { "StartEpochSeconds out of range" }
        require(durationMinutes <= MAX_DURATION_MINUTES) { "Duration must be at most $MAX_DURATION_MINUTES" }
    }

    val startEpochSeconds: Long
        get() = EPOCH_2020 + (packedValue shr BITS_FOR_DURATION)

    val startInstant
        get() = Instant.ofEpochSecond(startEpochSeconds)

    val durationMinutes: UShort
        get() = (packedValue and DURATION_MASK).toUShort()

    val duration
        get() = Duration.ofMinutes(durationMinutes.toLong())

    val endEpochSeconds: Long
        get() = startEpochSeconds + durationMinutes.toLong() * 60

    val endInstant
        get() = Instant.ofEpochSecond(endEpochSeconds)

    val startFormatted get() = dateTimeFormatterUTC.format(startInstant)
    val endFormatted get() = dateTimeFormatterUTC.format(endInstant)

    override fun toString() = "${durationMinutes}m @ $startFormatted"

    @get:JsonValue
    val asShortString get() = packedValue.asShortString

    companion object {
        const val EPOCH_2020 = 1577836800L
        const val BITS_FOR_DURATION = 11
        val MAX_DURATION_MINUTES: UShort get() = (2.0.pow(BITS_FOR_DURATION).toUInt() - 1u).toUShort()
        const val DURATION_MASK = (1L shl BITS_FOR_DURATION) - 1
        const val MAX_SECONDS = (1L shl (63 - BITS_FOR_DURATION)) - 1

        @JsonCreator
        @JvmStatic
        fun fromShortString(value: String) = InstantWithDuration(value.shortStringAsLong)

        fun fromStartAndEnd(start: Instant, end: Instant): InstantWithDuration {
            require(end >= start) { "End time must not be before start time" }
            val durationMinutes = Duration.between(start, end).toMinutes().toUShort()
            return fromStartAndDuration(start, durationMinutes)
        }

        fun fromStartAndDuration(start: String, durationMinutes: UShort = 0u) =
            fromStartAndDuration(Instant.parse(start), durationMinutes)

        fun fromStartAndDuration(start: Instant, durationMinutes: UShort = 0u) =
            fromStartAndDuration(start.epochSecond, durationMinutes)

        fun fromStartAndDuration(startEpochSeconds: Long, durationMinutes: UShort = 0u): InstantWithDuration {
            return InstantWithDuration(
                ((startEpochSeconds - EPOCH_2020).toInt().toLong() shl BITS_FOR_DURATION) or durationMinutes.toLong()
            )
        }
    }
}
