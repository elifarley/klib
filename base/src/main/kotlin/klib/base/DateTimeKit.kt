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
    fun Instant.toLocalDateTime(zoneId: ZoneId? = null) = LocalDateTime.ofInstant(this, zoneId ?: ZoneId.systemDefault())!!

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
    inline fun nowAsISODate(zoneId: String? = "America/Sao_Paulo") = ZonedDateTime.now(zoneId?.let {ZoneId.of(zoneId)} ?: ZoneId.systemDefault()).let {
        DateTimeFormatter.BASIC_ISO_DATE.format(it).takeWhile { it != '-' }
    }

    fun DateTimeFormatter.format(date: Date) = this.format(date.toInstant())!!

}
