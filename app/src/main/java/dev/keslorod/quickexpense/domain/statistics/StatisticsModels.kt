package dev.keslorod.quickexpense.domain.statistics

import java.time.LocalDate

enum class StatisticsDatePreset {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    LAST_7_DAYS,
    LAST_30_DAYS,
    CUSTOM,
    ALL_TIME,
}

data class StatisticsDateRangeState(
    val preset: StatisticsDatePreset,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val comparisonEnabled: Boolean = true,
)

data class StatisticsComparisonRange(
    val currentStart: LocalDate,
    val currentEnd: LocalDate,
    val previousStart: LocalDate?,
    val previousEnd: LocalDate?,
)

enum class ComparisonDirection {
    UP,
    DOWN,
    SAME,
    NO_PREVIOUS_DATA,
    DISABLED,
}

data class PeriodComparisonSummary(
    val currentAmount: Long, // in cents
    val previousAmount: Long?,
    val absoluteDelta: Long?,
    val relativeDeltaPercent: Double?,
    val direction: ComparisonDirection,
    val label: String,
)

data class StatsAmountSummary(
    val currentAmount: Long,
    val previousAmount: Long?,
    val currentCount: Int? = null,
    val previousCount: Int? = null,
    val comparison: PeriodComparisonSummary,
)

data class StatsBreakdownItem(
    val id: String,
    val label: String,
    val amount: Long,
    val count: Int,
    val shareOfTotalPercent: Double,
    val relativeToMaxPercent: Double,
)

data class DashboardStatistics(
    val totalSpent: StatsAmountSummary,
    val categoryPie: List<StatsBreakdownItem>,
    val merchantPie: List<StatsBreakdownItem>,
    val topTags: List<StatsBreakdownItem>,
)
