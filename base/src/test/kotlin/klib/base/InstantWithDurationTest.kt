package klib.base

import klib.base.InstantWithDuration.Companion.DURATION_MINUTES_MASK
import klib.base.InstantWithDuration.Companion.EPOCH_2020
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

@DisplayName("InstantWithDuration")
class InstantWithDurationTest {

    @Test
    fun `Constructor with startEpochSeconds and durationMinutes creates correct packedValue`() {
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
    fun `Constructor with packedValue creates correct startEpochSeconds and durationMinutes`() {
        val instantWithDuration = InstantWithDuration(0x7ffL or DURATION_MINUTES_MASK)
        assertEquals(EPOCH_2020, instantWithDuration.startEpochSeconds)
        assertEquals(MAX_DURATION_MINUTES, instantWithDuration.durationMinutes)
    }

    @ParameterizedTest
    @CsvSource(
        "2020-01-01T00:00:00Z, 2020-01-01T00:00:00Z, 0",
        "2025-08-01T10:00:00Z, 2025-08-01T10:05:00Z, 5",
        "2025-08-01T10:00:00Z, 2025-08-01T10:30:00Z, 30",
        "2025-08-01T10:00:00Z, 2025-08-01T11:00:00Z, 60",
        "6375-04-08T15:04:31Z, 6502-11-12T00:07:31Z, $DURATION_MINUTES_MASK"
    )
    @DisplayName("Factory methods")
    fun testFromStartFactory(start: String, end: String, expectedDurationMinutes: UInt) {
        InstantWithDuration.fromStartAndEnd(start, end).run {
            assertEquals(start, startFormatted)
            assertEquals(end, endFormatted)
            assertEquals(expectedDurationMinutes, durationMinutes, "fromStartAndEnd")
        }
        InstantWithDuration.fromStartAndDuration(start, expectedDurationMinutes).run {
            assertEquals(start, startFormatted)
            assertEquals(end, endFormatted)
            assertEquals(expectedDurationMinutes, durationMinutes, "fromStartAndDuration")
        }
    }

    @Test
    @DisplayName("'fromStartAndEnd' throws IllegalArgumentException when end is before start")
    fun testFromStartAndEndThrowsException() {
        val start = "2025-08-01T09:00:01Z"
        val end = "2025-08-01T09:00:00Z"

        assertThrows<IllegalArgumentException> {
            InstantWithDuration.fromStartAndEnd(start, end)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "$EPOCH_2020, 0, 000000",
        "$EPOCH_2020, 1, 000001",
        "$EPOCH_2020, $DURATION_MINUTES_MASK, 13ydj3",
        "${EPOCH_2020 + 60}, 0, 1ulajuo",
        "${EPOCH_2020 + INSTANT_SECONDS_MASK}, $DURATION_MINUTES_MASK, 1y2p0ij32e8e7",
    )
    @DisplayName("Roundtrip (asShortString -> fromShortString)")
    fun roundTrip(
        startEpochSeconds: Long,
        durationMinutes: UInt,
        expectedShortString: String
    ) {
        val encodedString = InstantWithDuration.fromStartAndDuration(startEpochSeconds, durationMinutes).asShortString
        InstantWithDuration.fromShortString(encodedString).also { decoded ->
            assertEquals(startEpochSeconds, decoded.startEpochSeconds)
            assertEquals(durationMinutes, decoded.durationMinutes)
        }
        assertEquals(expectedShortString, encodedString)
    }

    @Test
    fun `'endEpochSeconds' - minimum valid values`() {
        val instantWithDuration = InstantWithDuration.fromStartAndDuration(EPOCH_2020)
        assertEquals(
            EPOCH_2020,
            instantWithDuration.endEpochSeconds,
            "endEpochSeconds"
        )
    }

    @Test
    fun `Maximum representable values`() {
        val instantWithDuration = InstantWithDuration.fromStartAndDuration(
            EPOCH_2020 + INSTANT_SECONDS_MASK, MAX_DURATION_MINUTES
        )
        assertEquals("6375-04-08T15:04:31Z", instantWithDuration.startFormatted, "startFormatted")
        assertEquals("6502-11-12T00:07:31Z", instantWithDuration.endFormatted, "endFormatted")
        assertEquals(EPOCH_2020 + INSTANT_SECONDS_MASK, instantWithDuration.startEpochSeconds, "startEpochSeconds")
        assertEquals(143043322051, instantWithDuration.endEpochSeconds, "endEpochSeconds")
        assertEquals(DURATION_MINUTES_MASK.toUInt(), instantWithDuration.durationMinutes, "durationMinutes")
        assertEquals(DURATION_MINUTES_MASK, instantWithDuration.duration.toMinutes(), "duration")
    }

    @Test
    fun `Constructor throws IllegalArgumentException for values exceeding maximum`() {
        assertThrows<IllegalArgumentException>("Start") {
            InstantWithDuration.fromStartAndDuration(
                EPOCH_2020 + INSTANT_SECONDS_MASK + 1,
                0u
            )
        }

        assertThrows<IllegalArgumentException>("Duration") {
            InstantWithDuration.fromStartAndDuration(
                EPOCH_2020,
                MAX_DURATION_MINUTES + 1u
            )
        }
    }

    @ParameterizedTest
    @CsvSource(
        "$EPOCH_2020, 0, '0m @ 2020-01-01T00:00:00Z'",
        "$EPOCH_2020, 1, '1m @ 2020-01-01T00:00:00Z'",
        "$EPOCH_2020, 2047, '2047m @ 2020-01-01T00:00:00Z'",
        "$EPOCH_2020, $DURATION_MINUTES_MASK, '67108863m @ 2020-01-01T00:00:00Z'",
        "${EPOCH_2020 + 60}, 0, '0m @ 2020-01-01T00:01:00Z'",
    )
    @DisplayName("'toShortString()' for various inputs")
    fun `toShortString()`(
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
    @DisplayName("'endFormatted' for various inputs")
    fun endFormatted(
        startEpochSeconds: Long,
        durationMinutes: UInt,
        expectedEnd: String
    ) {
        assertEquals(
            expectedEnd,
            InstantWithDuration.fromStartAndDuration(startEpochSeconds, durationMinutes).endFormatted
        )
    }

    @Test
    fun `'compareTo' works correctly`() {
        val earlier = InstantWithDuration.fromStartAndDuration(EPOCH_2020, 30u)
        val later = InstantWithDuration.fromStartAndDuration(EPOCH_2020, 60u)
        val sameEndAsLater = InstantWithDuration.fromStartAndDuration(EPOCH_2020 + 1800, 30u)

        assertEquals(later.endFormatted, sameEndAsLater.endFormatted)
        assert(earlier < later)
        assert(later > earlier)
        assert(earlier < sameEndAsLater)
        assert(later < sameEndAsLater)
    }
}
