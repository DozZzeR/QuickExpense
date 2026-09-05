package dev.keslorod.quickexpense

import dev.keslorod.quickexpense.domain.statistics.StatisticsDatePreset
import dev.keslorod.quickexpense.domain.statistics.StatisticsDateRangeState
import dev.keslorod.quickexpense.domain.statistics.StatisticsDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class StatisticsDateUtilsTest {

    private fun state(
        preset: StatisticsDatePreset,
        start: LocalDate,
        end: LocalDate,
        comparison: Boolean = true
    ) = StatisticsDateRangeState(preset, start, end, comparison)

    // Regression: LocalDate.MIN used to overflow Instant.toEpochMilli() and crash "All time".
    @Test
    fun allTimeStartConvertsToMillisWithoutOverflow() {
        val (start, _) = StatisticsDateUtils.getRangeForPreset(StatisticsDatePreset.ALL_TIME)
        // Must not throw ArithmeticException.
        val millis = StatisticsDateUtils.localDateToMillis(start)
        assertEquals(StatisticsDateUtils.ALL_TIME_START, start)
        // Epoch start in the host time zone is close to 0 (offset only).
        assert(kotlin.math.abs(millis) < 24L * 60 * 60 * 1000)
    }

    @Test
    fun thisMonthComparesAgainstFullPreviousMonth() {
        val range = StatisticsDateUtils.getComparisonRange(
            state(StatisticsDatePreset.THIS_MONTH, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        )
        assertEquals(LocalDate.of(2026, 2, 1), range.previousStart)
        assertEquals(LocalDate.of(2026, 2, 28), range.previousEnd)
    }

    @Test
    fun customRangeComparesAgainstSameLengthImmediatelyBefore() {
        // 10-day range 2026-06-10..2026-06-19 -> previous 2026-05-31..2026-06-09
        val range = StatisticsDateUtils.getComparisonRange(
            state(StatisticsDatePreset.CUSTOM, LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 19))
        )
        assertEquals(LocalDate.of(2026, 6, 9), range.previousEnd)
        assertEquals(LocalDate.of(2026, 5, 31), range.previousStart)
    }

    @Test
    fun last7DaysShiftsBackByExactly7Days() {
        val range = StatisticsDateUtils.getComparisonRange(
            state(StatisticsDatePreset.LAST_7_DAYS, LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 16))
        )
        assertEquals(LocalDate.of(2026, 6, 3), range.previousStart)
        assertEquals(LocalDate.of(2026, 6, 9), range.previousEnd)
    }

    @Test
    fun allTimeDisablesComparison() {
        val range = StatisticsDateUtils.getComparisonRange(
            state(StatisticsDatePreset.ALL_TIME, StatisticsDateUtils.ALL_TIME_START, LocalDate.of(2026, 6, 16))
        )
        assertNull(range.previousStart)
        assertNull(range.previousEnd)
    }

    @Test
    fun comparisonDisabledFlagYieldsNoPreviousRange() {
        val range = StatisticsDateUtils.getComparisonRange(
            state(StatisticsDatePreset.THIS_MONTH, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), comparison = false)
        )
        assertNull(range.previousStart)
        assertNull(range.previousEnd)
    }
}
