package dev.keslorod.quickexpense.data.repository

import dev.keslorod.quickexpense.data.db.AppDatabase
import dev.keslorod.quickexpense.domain.statistics.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

class StatisticsRepository(private val db: AppDatabase) {

    suspend fun getDashboardStatistics(state: StatisticsDateRangeState): DashboardStatistics {
        val currentRange = StatisticsDateUtils.getComparisonRange(state)
        
        val currentData = getPeriodData(currentRange.currentStart, currentRange.currentEnd)
        val previousData = currentRange.previousStart?.let { start ->
            currentRange.previousEnd?.let { end ->
                getPeriodData(start, end)
            }
        }

        val totalSpent = calculateTotalSpent(currentData, previousData, state)
        
        val categoryPie = calculateCategoryBreakdown(currentData)
        val merchantPie = calculateMerchantBreakdown(currentData)
        val topTags = calculateTagBreakdown(currentData)

        return DashboardStatistics(
            totalSpent = totalSpent,
            categoryPie = categoryPie,
            merchantPie = merchantPie,
            topTags = topTags
        )
    }

    private suspend fun getPeriodData(start: LocalDate, end: LocalDate): PeriodData {
        val from = StatisticsDateUtils.localDateToMillis(start)
        val to = StatisticsDateUtils.localDateToMillis(end, endOfDay = true)
        
        val expenses = db.expenses().expensesInRange(from, to)
        val expenseIds = expenses.map { it.id }
        
        val allSplitNodes = mutableListOf<dev.keslorod.quickexpense.data.entities.SplitNode>()
        // We might want to optimize this by adding a DAO method to fetch all nodes for a list of expenses
        expenseIds.forEach { id ->
            allSplitNodes.addAll(db.splitNodes().getByExpenseId(id))
        }

        val nodeTags = mutableMapOf<String, List<dev.keslorod.quickexpense.data.entities.Tag>>()
        allSplitNodes.forEach { node ->
            nodeTags[node.id] = db.splitNodeTags().getTagsForSplitNode(node.id)
        }

        val categoryNames = db.categories().all().associate { it.id to it.name }
        val merchantNames = db.merchants().all().associate { it.id to it.name }
        val tagNames = db.tags().all().associate { it.id to it.name }

        return PeriodData(
            expenses = expenses,
            splitNodes = allSplitNodes,
            nodeTags = nodeTags,
            categoryNames = categoryNames,
            merchantNames = merchantNames,
            tagNames = tagNames
        )
    }

    private fun calculateTotalSpent(
        current: PeriodData,
        previous: PeriodData?,
        state: StatisticsDateRangeState
    ): StatsAmountSummary {
        val currentAmount = current.expenses.sumOf { it.amount }
        val previousAmount = previous?.expenses?.sumOf { it.amount }
        
        val absoluteDelta = if (previousAmount != null) currentAmount - previousAmount else null
        val relativeDeltaPercent = if (previousAmount != null && previousAmount != 0L) {
            (currentAmount - previousAmount).toDouble() / previousAmount * 100
        } else if (previousAmount == 0L && currentAmount > 0L) {
            null // "new spending"
        } else {
            0.0
        }

        val direction = when {
            !state.comparisonEnabled || previousAmount == null -> ComparisonDirection.DISABLED
            currentAmount > previousAmount -> ComparisonDirection.UP
            currentAmount < previousAmount -> ComparisonDirection.DOWN
            else -> ComparisonDirection.SAME
        }

        return StatsAmountSummary(
            currentAmount = currentAmount,
            previousAmount = previousAmount,
            currentCount = current.expenses.size,
            previousCount = previous?.expenses?.size,
            comparison = PeriodComparisonSummary(
                currentAmount = currentAmount,
                previousAmount = previousAmount,
                absoluteDelta = absoluteDelta,
                relativeDeltaPercent = relativeDeltaPercent,
                direction = direction,
                label = "" // Will be set in UI or specialized helper
            )
        )
    }

    private fun calculateCategoryBreakdown(data: PeriodData): List<StatsBreakdownItem> {
        val fragments = data.expenses.flatMap { StatisticsAggregation.buildCategoryFragments(it, data.splitNodes) }
        val totals = fragments.groupBy { it.effectiveCategoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        val totalAmount = totals.values.sum()
        if (totalAmount == 0L) return emptyList()

        val items = totals.map { (categoryId, amount) ->
            StatsBreakdownItem(
                id = categoryId,
                label = data.categoryNames[categoryId] ?: "Uncategorized",
                amount = amount,
                count = fragments.count { it.effectiveCategoryId == categoryId },
                shareOfTotalPercent = amount.toDouble() / totalAmount * 100,
                relativeToMaxPercent = 0.0 // Set below
            )
        }.sortedByDescending { it.amount }

        val maxAmount = items.firstOrNull()?.amount ?: 1L
        return items.map { it.copy(relativeToMaxPercent = it.amount.toDouble() / maxAmount * 100) }
    }

    private fun calculateMerchantBreakdown(data: PeriodData): List<StatsBreakdownItem> {
        val totals = data.expenses.groupBy { it.merchantId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        val totalAmount = totals.values.sum()
        if (totalAmount == 0L) return emptyList()

        val items = totals.map { (merchantId, amount) ->
            StatsBreakdownItem(
                id = merchantId ?: "unknown",
                label = merchantId?.let { data.merchantNames[it] } ?: "Unknown merchant",
                amount = amount,
                count = data.expenses.count { it.merchantId == merchantId },
                shareOfTotalPercent = amount.toDouble() / totalAmount * 100,
                relativeToMaxPercent = 0.0 // Set below
            )
        }.sortedByDescending { it.amount }

        val maxAmount = items.firstOrNull()?.amount ?: 1L
        return items.map { it.copy(relativeToMaxPercent = it.amount.toDouble() / maxAmount * 100) }
    }

    private fun calculateTagBreakdown(data: PeriodData): List<StatsBreakdownItem> {
        val fragments = StatisticsAggregation.buildTagFragments(data.splitNodes, data.nodeTags)
        val totals = fragments.groupBy { it.tagId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        val maxAmount = totals.values.maxOrNull() ?: 1L

        return totals.map { (tagId, amount) ->
            StatsBreakdownItem(
                id = tagId,
                label = data.tagNames[tagId] ?: "Tag",
                amount = amount,
                count = fragments.count { it.tagId == tagId },
                shareOfTotalPercent = 0.0, // Not applicable for overlapping tags
                relativeToMaxPercent = amount.toDouble() / maxAmount * 100
            )
        }.sortedByDescending { it.amount }
    }

    private data class PeriodData(
        val expenses: List<dev.keslorod.quickexpense.data.entities.Expense>,
        val splitNodes: List<dev.keslorod.quickexpense.data.entities.SplitNode>,
        val nodeTags: Map<String, List<dev.keslorod.quickexpense.data.entities.Tag>>,
        val categoryNames: Map<String, String>,
        val merchantNames: Map<String, String>,
        val tagNames: Map<String, String>
    )
}
