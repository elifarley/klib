package klib.base

import klib.base.InstantWithDuration.Companion.DURATION_MINUTES_MASK
import klib.base.InstantWithDuration.Companion.EPOCH_2020
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class InstantWithDurationTest {

    @Test
    fun `constructor with startEpochSeconds and durationMinutes creates correct packedValue`() {
        val instantWithDuration = InstantWithDuration
            .fromStartAndDuration(EPOCH_2020, MAX_DURATION_MINUTES)
        assertEquals("2020-01-01T00:00:00Z", instantWithDuration.startFormatted, "startFormatted")
        assertEquals("2147-08-06T09:03:00Z", instantWithDuration.endFormatted, "endFormatted")
        assertEquals(
            60 * DURATION_MINUTES_MASK,
            instantWithDuration.endEpochSeconds - instantWithDuration.startEpochSeconds,
            "Max duration"
        )
        assertEquals(DURATION_MINUTES_MASK, instantWithDuration.packedValue and DURATION_MINUTES_MASK, "packedValue")
        assertEquals(0x3ffffff, instantWithDuration.packedValue, "packedValue should be 67108863")
    }

    @Test
    fun `constructor with packedValue creates correct startEpochSeconds and durationMinutes`() {
        val instantWithDuration = InstantWithDuration(0x7ffL or DURATION_MINUTES_MASK)
        assertEquals(EPOCH_2020, instantWithDuration.startEpochSeconds)
        assertEquals(MAX_DURATION_MINUTES, instantWithDuration.durationMinutes)
    }

    @ParameterizedTest
    @CsvSource(
        "$EPOCH_2020, 0, 000000",
        "$EPOCH_2020, 1, 000001",
        "$EPOCH_2020, $DURATION_MINUTES_MASK, 13ydj3",
        "${EPOCH_2020 + 60}, 0, 1ulajuo",
    )
    fun `roundtrip conversion maintains values`(
        startEpochSeconds: Long,
        durationMinutes: UInt,
        expectedShortString: String
    ) {
        val instantWithDuration1 = InstantWithDuration.fromStartAndDuration(startEpochSeconds, durationMinutes)
        assertEquals(expectedShortString, instantWithDuration1.asShortString)

        val instantWithDuration2 = InstantWithDuration.fromShortString(expectedShortString)
        assertEquals(startEpochSeconds, instantWithDuration2.startEpochSeconds)
        assertEquals(durationMinutes, instantWithDuration2.durationMinutes)
    }

    @Test
    fun `endEpochSeconds - minimum valid values`() {
        val instantWithDuration = InstantWithDuration.fromStartAndDuration(EPOCH_2020)
        assertEquals(
            EPOCH_2020,
            instantWithDuration.endEpochSeconds,
            "endEpochSeconds"
        )
    }

    @Test
    fun `endEpochSeconds - Year 2120, max duration`() {
        val epoch2120 = 4733510400L
        val instantWithDuration = InstantWithDuration.fromStartAndDuration(
            epoch2120,
            MAX_DURATION_MINUTES
        )
        assertEquals("2120-01-01T00:00:00Z", instantWithDuration.startFormatted, "startFormatted")
        assertEquals("2247-08-06T09:03:00Z", instantWithDuration.endFormatted, "endFormatted")
        assertEquals(
            epoch2120,
            instantWithDuration.startEpochSeconds,
            "startEpochSeconds"
        )
        assertEquals(
            epoch2120 + MAX_DURATION_MINUTES.toLong() * 60,
            instantWithDuration.endEpochSeconds,
            "endEpochSeconds"
        )
    }

    @ParameterizedTest
    @CsvSource(
        "$EPOCH_2020, 0, '0m @ 2020-01-01T00:00:00Z'",
        "$EPOCH_2020, 1, '1m @ 2020-01-01T00:00:00Z'",
        "$EPOCH_2020, 2047, '2047m @ 2020-01-01T00:00:00Z'",
        "$EPOCH_2020, $DURATION_MINUTES_MASK, '67108863m @ 2020-01-01T00:00:00Z'",
        "${EPOCH_2020 + 60}, 0, '0m @ 2020-01-01T00:01:00Z'",
    )
    fun `toShortString() for various inputs`(
        startEpochSeconds: Long,
        durationMinutes: UInt,
        expected: String
    ) {
        assertEquals(
            expected,
            InstantWithDuration.fromStartAndDuration(startEpochSeconds, durationMinutes).toString()
        )
    }

    @ParameterizedTest
    @CsvSource(
        "$EPOCH_2020,        0, '2020-01-01T00:00:00Z'",
        "$EPOCH_2020,        1, '2020-01-01T00:01:00Z'",
        "${EPOCH_2020 + 60}, 0, '2020-01-01T00:01:00Z'",
        "$EPOCH_2020,     2047, '2020-01-02T10:07:00Z'",
        "$EPOCH_2020,  $DURATION_MINUTES_MASK,          '2147-08-06T09:03:00Z'",
        "${EPOCH_2020 + DURATION_MINUTES_MASK * 60}, 0, '2147-08-06T09:03:00Z'",
    )
    fun `'endFormatted' for various inputs`(
        startEpochSeconds: Long,
        durationMinutes: UInt,
        expected: String
    ) {
        assertEquals(
            expected,
            InstantWithDuration.fromStartAndDuration(startEpochSeconds, durationMinutes).endFormatted
        )
    }

    @Test
    fun `compareTo works correctly`() {
        val earlier = InstantWithDuration.fromStartAndDuration(EPOCH_2020, 30u)
        val later = InstantWithDuration.fromStartAndDuration(EPOCH_2020, 60u)
        val sameEndAsLater = InstantWithDuration.fromStartAndDuration(EPOCH_2020 + 1800, 30u)

        assertEquals(later.endFormatted , sameEndAsLater.endFormatted)
        assert(earlier < later)
        assert(later > earlier)
        assert(earlier < sameEndAsLater)
        assert(later < sameEndAsLater)
    }
}
