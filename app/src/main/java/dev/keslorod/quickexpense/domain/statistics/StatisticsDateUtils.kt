package dev.keslorod.quickexpense.domain.statistics

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object StatisticsDateUtils {

    fun localDateToMillis(date: LocalDate, endOfDay: Boolean = false): Long {
        val dateTime = if (endOfDay) {
            date.atTime(23, 59, 59, 999_999_999)
        } else {
            date.atStartOfDay()
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getRangeForPreset(preset: StatisticsDatePreset, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
        return when (preset) {
            StatisticsDatePreset.TODAY -> today to today
            StatisticsDatePreset.YESTERDAY -> today.minusDays(1) to today.minusDays(1)
            StatisticsDatePreset.THIS_WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                start to end
            }
            StatisticsDatePreset.THIS_MONTH -> {
                val start = today.with(TemporalAdjusters.firstDayOfMonth())
                val end = today.with(TemporalAdjusters.lastDayOfMonth())
                start to end
            }
            StatisticsDatePreset.THIS_YEAR -> {
                val start = today.with(TemporalAdjusters.firstDayOfYear())
                val end = today.with(TemporalAdjusters.lastDayOfYear())
                start to end
            }
            StatisticsDatePreset.LAST_7_DAYS -> today.minusDays(6) to today
            StatisticsDatePreset.LAST_30_DAYS -> today.minusDays(29) to today
            StatisticsDatePreset.ALL_TIME -> LocalDate.MIN to today
            StatisticsDatePreset.CUSTOM -> today to today // Placeholder, should be handled by caller
        }
    }

    fun getComparisonRange(state: StatisticsDateRangeState): StatisticsComparisonRange {
        if (!state.comparisonEnabled || state.preset == StatisticsDatePreset.ALL_TIME) {
            return StatisticsComparisonRange(state.startDate, state.endDate, null, null)
        }

        val (prevStart, prevEnd) = when (state.preset) {
            StatisticsDatePreset.TODAY -> {
                val yesterday = state.startDate.minusDays(1)
                yesterday to yesterday
            }
            StatisticsDatePreset.YESTERDAY -> {
                val dayBefore = state.startDate.minusDays(1)
                dayBefore to dayBefore
            }
            StatisticsDatePreset.THIS_WEEK -> {
                val start = state.startDate.minusWeeks(1)
                val end = state.endDate.minusWeeks(1)
                start to end
            }
            StatisticsDatePreset.THIS_MONTH -> {
                val start = state.startDate.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth())
                val end = state.startDate.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
                start to end
            }
            StatisticsDatePreset.THIS_YEAR -> {
                val start = state.startDate.minusYears(1).with(TemporalAdjusters.firstDayOfYear())
                val end = state.startDate.minusYears(1).with(TemporalAdjusters.lastDayOfYear())
                start to end
            }
            StatisticsDatePreset.LAST_7_DAYS -> {
                val start = state.startDate.minusDays(7)
                val end = state.endDate.minusDays(7)
                start to end
            }
            StatisticsDatePreset.LAST_30_DAYS -> {
                val start = state.startDate.minusDays(30)
                val end = state.endDate.minusDays(30)
                start to end
            }
            StatisticsDatePreset.CUSTOM -> {
                val periodLength = ChronoUnit.DAYS.between(state.startDate, state.endDate) + 1
                val end = state.startDate.minusDays(1)
                val start = end.minusDays(periodLength - 1)
                start to end
            }
            StatisticsDatePreset.ALL_TIME -> null to null
        }

        return StatisticsComparisonRange(state.startDate, state.endDate, prevStart, prevEnd)
    }
}
