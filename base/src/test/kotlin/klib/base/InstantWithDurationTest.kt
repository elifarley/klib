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
            .fromStartAndDuration(EPOCH_2020, InstantWithDuration.MAX_DURATION_MINUTES)
        assertEquals("2020-01-01T00:00:00Z", instantWithDuration.startFormatted, "startFormatted")
        assertEquals("2020-01-02T10:07:00Z", instantWithDuration.endFormatted, "endFormatted")
        assertEquals(
            60 * DURATION_MINUTES_MASK,
            instantWithDuration.endEpochSeconds - instantWithDuration.startEpochSeconds,
            "Max duration"
        )
        assertEquals(DURATION_MINUTES_MASK, instantWithDuration.packedValue and DURATION_MINUTES_MASK, "packedValue")
        assertEquals(0x7FF, instantWithDuration.packedValue, "packedValue should be 2047")
    }

    @Test
    fun `constructor with packedValue creates correct startEpochSeconds and durationMinutes`() {
        val instantWithDuration = InstantWithDuration(0x7ffL or DURATION_MINUTES_MASK)
        assertEquals(EPOCH_2020, instantWithDuration.startEpochSeconds)
        assertEquals(InstantWithDuration.MAX_DURATION_MINUTES, instantWithDuration.durationMinutes)
    }

    @ParameterizedTest
    @CsvSource(
        "1630444800, 60, 1dhubhoc",
        "1630448400, 120, 1dhypim0",
        "1630448400, 121, 1dhypim1",
        "1630448401, 120, 1dhypk6w",
        "1630452000, 180, 1di33jjo"
    )
    fun `roundtrip conversion maintains values`(
        startEpochSeconds: Long,
        durationMinutes: UShort,
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
        val epoch2120 = 4733510400
        val instantWithDuration = InstantWithDuration.fromStartAndDuration(
            epoch2120,
            InstantWithDuration.MAX_DURATION_MINUTES
        )
        assertEquals("2120-01-01T00:00:00Z", instantWithDuration.startFormatted, "startFormatted")
        assertEquals("2120-01-02T10:07:00Z", instantWithDuration.endFormatted, "endFormatted")
        assertEquals(
            epoch2120,
            instantWithDuration.startEpochSeconds,
            "startEpochSeconds"
        )
        assertEquals(
            epoch2120 + InstantWithDuration.MAX_DURATION_MINUTES.toLong() * 60,
            instantWithDuration.endEpochSeconds,
            "endEpochSeconds"
        )
    }

    @ParameterizedTest
    @CsvSource(
        "$EPOCH_2020, 0, '0m @ 2020-01-01T00:00:00Z'",
        "$EPOCH_2020, 1, '1m @ 2020-01-01T00:00:00Z'",
        "$EPOCH_2020, 2047, '2047m @ 2020-01-01T00:00:00Z'",
        "${EPOCH_2020 + 60}, 0, '0m @ 2020-01-01T00:01:00Z'",
    )
    fun `toShortString() for various inputs`(
        startEpochSeconds: Long,
        durationMinutes: UShort,
        expected: String
    ) {
        assertEquals(
            expected,
            InstantWithDuration.fromStartAndDuration(startEpochSeconds, durationMinutes).toString()
        )
    }

    @ParameterizedTest
    @CsvSource(
        "$EPOCH_2020, 0, '2020-01-01T00:00:00Z'",
        "$EPOCH_2020, 1, '2020-01-01T00:01:00Z'",
        "${EPOCH_2020 + 60}, 0, '2020-01-01T00:01:00Z'",
        "$EPOCH_2020, 2047, '2020-01-02T10:07:00Z'",
    )
    fun `'endFormatted' for various inputs`(
        startEpochSeconds: Long,
        durationMinutes: UShort,
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
