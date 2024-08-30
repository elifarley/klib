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

import klib.base.ShortString.asShortString
import klib.base.ShortString.shortStringAsLong

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


@Suppress("MagicNumber")
object ShortString {
    val Long.asShortString get() = toString(36).padStart(6, '0')
    val String.shortStringAsLong get() = toLong(36)
}

@Suppress("MagicNumber")
@JvmInline
value class InstantWithDuration(internal val packedValue: Long) : Comparable<InstantWithDuration> {

    init {
        require(packedValue shr (63 - BITS_FOR_DURATION) == 0L) { "StartEpochSeconds out of range" }
        require(durationMinutes <= MAX_DURATION_MINUTES) { "Duration must be at most $MAX_DURATION_MINUTES" }
    }

    val startEpochSeconds: Long
        get() = EPOCH_2020 + (packedValue shr BITS_FOR_DURATION)

    val durationMinutes: UShort
        get() = (packedValue and DURATION_MINUTES_MASK).toUShort()

    @get:JsonValue
    val asShortString get() = packedValue.asShortString

    override fun toString() = "${durationMinutes}m @ $startFormatted"

    override fun compareTo(other: InstantWithDuration): Int = packedValue.compareTo(other.packedValue)

    companion object {
        const val EPOCH_2020 = 1577836800L
        const val BITS_FOR_DURATION = 11
        const val DURATION_MINUTES_MASK: Long = (1L shl BITS_FOR_DURATION) - 1
        val MAX_DURATION_MINUTES: UShort = DURATION_MINUTES_MASK.toUShort()
        const val INSTANT_SECONDS_MASK = (1L shl (63 - BITS_FOR_DURATION)) - 1

        @JsonCreator
        @JvmStatic
        fun fromShortString(value: String) = InstantWithDuration(value.shortStringAsLong)

        fun fromStartAndEnd(start: String, end: String) =
            fromStartAndEnd(Instant.parse(start), Instant.parse(end))

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
            require(durationMinutes <= MAX_DURATION_MINUTES) {
                "Max duration minutes exceeded by ${durationMinutes - MAX_DURATION_MINUTES}"
            }
            require(startEpochSeconds >= EPOCH_2020) {
                "Min startEpochSeconds should be increased by ${EPOCH_2020 - startEpochSeconds}"
            }
            return InstantWithDuration(
                ((startEpochSeconds - EPOCH_2020) shl BITS_FOR_DURATION) or durationMinutes.toLong()
            )
        }
    }
}

val InstantWithDuration.startInstant
    get() = Instant.ofEpochSecond(startEpochSeconds)

val InstantWithDuration.duration
    get() = Duration.ofMinutes(durationMinutes.toLong())

@Suppress("MagicNumber")
val InstantWithDuration.endEpochSeconds: Long
    get() = startEpochSeconds + durationMinutes.toLong() * 60

val InstantWithDuration.endInstant
    get() = Instant.ofEpochSecond(endEpochSeconds)

private val dateTimeFormatterUTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    .withZone(ZoneOffset.UTC)

val InstantWithDuration.startFormatted get() = dateTimeFormatterUTC.format(startInstant)
val InstantWithDuration.endFormatted get() = dateTimeFormatterUTC.format(endInstant)
