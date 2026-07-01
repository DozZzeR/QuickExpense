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

data class MerchantDetailsData(
    val merchantName: String,
    val totalSummary: StatsAmountSummary,
    val categoryBreakdown: List<StatsBreakdownItem>,
    val transactions: List<dev.keslorod.quickexpense.data.entities.Expense>
)

data class CategoryDetailsData(
    val categoryName: String,
    val totalAmount: Long,
    val comparison: PeriodComparisonSummary,
    val merchantBreakdown: List<StatsBreakdownItem>,
    val fragments: List<CategoryAmountFragment>
)

data class TagDetailsData(
    val tagName: String,
    val totalAmount: Long,
    val comparison: PeriodComparisonSummary,
    val fragments: List<TagResultItem>
)

data class TagResultItem(
    val expenseId: String,
    val label: String,
    val amount: Long,
    val date: Long,
    val merchantName: String?
)

data class StatisticsSearchFilter(
    val dateRange: StatisticsDateRangeState,
    val merchantIds: Set<String> = emptySet(),
    val categoryIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
    val tagMatchMode: TagMatchMode = TagMatchMode.ALL,
    val query: String = ""
)

enum class TagMatchMode { ALL, ANY }

data class SearchResultsData(
    val totalAmount: Long,
    val comparison: PeriodComparisonSummary,
    val results: List<SearchResultItem>
)

data class SearchResultItem(
    val expenseId: String,
    val title: String,
    val amount: Long,
    val date: Long,
    val merchantName: String?,
    val categoryName: String?,
    val tags: List<String>
)

data class TransactionDetailsData(
    val expense: dev.keslorod.quickexpense.data.entities.Expense,
    val categoryName: String?,
    val merchantName: String?,
    val sourceName: String?,
    val splitNodes: List<dev.keslorod.quickexpense.data.entities.SplitNode>,
    val nodeTags: Map<String, List<dev.keslorod.quickexpense.data.entities.Tag>>
)
