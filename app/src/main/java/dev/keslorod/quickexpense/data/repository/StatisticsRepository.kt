package dev.keslorod.quickexpense.data.repository

import dev.keslorod.quickexpense.data.db.AppDatabase
import dev.keslorod.quickexpense.domain.statistics.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class StatisticsRepository(private val db: AppDatabase) {

    suspend fun getMerchantStats(state: StatisticsDateRangeState): List<StatsBreakdownItem> {
        val currentRange = StatisticsDateUtils.getComparisonRange(state)
        val data = getPeriodData(currentRange.currentStart, currentRange.currentEnd)
        return calculateMerchantBreakdown(data)
    }

    suspend fun getCategoryStats(state: StatisticsDateRangeState): List<StatsBreakdownItem> {
        val currentRange = StatisticsDateUtils.getComparisonRange(state)
        val data = getPeriodData(currentRange.currentStart, currentRange.currentEnd)
        return calculateCategoryBreakdown(data)
    }

    suspend fun getTagStats(state: StatisticsDateRangeState): List<StatsBreakdownItem> {
        val currentRange = StatisticsDateUtils.getComparisonRange(state)
        val data = getPeriodData(currentRange.currentStart, currentRange.currentEnd)
        return calculateTagBreakdown(data)
    }

    suspend fun getDashboardStatistics(state: StatisticsDateRangeState): DashboardStatistics {
        val currentRange = StatisticsDateUtils.getComparisonRange(state)

        val currentData = getPeriodData(currentRange.currentStart, currentRange.currentEnd)
        val previousData = if (currentRange.previousStart != null && currentRange.previousEnd != null) {
            getPeriodData(currentRange.previousStart, currentRange.previousEnd)
        } else null

        val totalSpent = calculateTotalSpent(currentData.expenses, previousData?.expenses, state)

        val categoryPie = calculateCategoryBreakdown(currentData)
        val merchantPie = calculateMerchantBreakdown(currentData)
        val topTags = calculateTagBreakdown(currentData)
        val dailyTrend = calculateDailyTrend(currentData, currentRange.currentStart, currentRange.currentEnd)

        return DashboardStatistics(
            totalSpent = totalSpent,
            categoryPie = categoryPie,
            merchantPie = merchantPie,
            topTags = topTags,
            dailyTrend = dailyTrend
        )
    }

    suspend fun getMerchantDetails(merchantId: String, state: StatisticsDateRangeState): MerchantDetailsData {
        val currentRange = StatisticsDateUtils.getComparisonRange(state)
        val currentData = getPeriodData(currentRange.currentStart, currentRange.currentEnd)
        val previousData = if (currentRange.previousStart != null && currentRange.previousEnd != null) {
            getPeriodData(currentRange.previousStart, currentRange.previousEnd)
        } else null

        // "unknown" is a synthetic id the merchant breakdown list uses to represent
        // expenses with no merchant (see calculateMerchantBreakdown) — there's no real row
        // for it, the actual expenses just have merchantId == null. Map it back before
        // filtering, otherwise `it.merchantId == merchantId` compares null to "unknown"
        // and never matches, leaving the total right (grouped elsewhere) but the detail
        // screen's breakdown/transaction list empty.
        val actualMerchantId = merchantId.takeIf { it != "unknown" }
        val merchantExpenses = currentData.expenses.filter { it.merchantId == actualMerchantId }
        val prevMerchantExpenses = previousData?.expenses?.filter { it.merchantId == actualMerchantId }

        val totalSummary = calculateTotalSpent(merchantExpenses, prevMerchantExpenses, state)

        val fragments = merchantExpenses.flatMap { StatisticsAggregation.buildCategoryFragments(it, currentData.splitNodes) }
        val categoryBreakdown = aggregateFragmentsByCategory(fragments, currentData.categoryNames)

        return MerchantDetailsData(
            merchantName = currentData.merchantNames[actualMerchantId] ?: "Unknown",
            totalSummary = totalSummary,
            categoryBreakdown = categoryBreakdown,
            transactions = merchantExpenses.sortedByDescending { it.createdAt }
        )
    }

    suspend fun getCategoryDetails(categoryId: String, state: StatisticsDateRangeState): CategoryDetailsData {
        val currentRange = StatisticsDateUtils.getComparisonRange(state)
        val currentData = getPeriodData(currentRange.currentStart, currentRange.currentEnd)
        val previousData = if (currentRange.previousStart != null && currentRange.previousEnd != null) {
            getPeriodData(currentRange.previousStart, currentRange.previousEnd)
        } else null

        val fragments = currentData.expenses.flatMap { StatisticsAggregation.buildCategoryFragments(it, currentData.splitNodes) }
            .filter { it.effectiveCategoryId == categoryId }
        
        val prevFragments = previousData?.expenses?.flatMap { StatisticsAggregation.buildCategoryFragments(it, previousData.splitNodes) }
            ?.filter { it.effectiveCategoryId == categoryId }

        val currentAmount = fragments.sumOf { it.amount }
        val previousAmount = prevFragments?.sumOf { it.amount }

        val merchantBreakdown = fragments.groupBy { frag ->
            currentData.expensesById[frag.expenseId]?.merchantId
        }.map { (mId, frags) ->
            val amount = frags.sumOf { it.amount }
            StatsBreakdownItem(
                id = mId ?: "unknown",
                label = currentData.merchantNames[mId] ?: "Unknown merchant",
                amount = amount,
                count = frags.size,
                shareOfTotalPercent = if (currentAmount > 0) amount.toDouble() / currentAmount * 100 else 0.0,
                relativeToMaxPercent = 0.0 // Set later
            )
        }.sortedByDescending { it.amount }

        val maxMerchant = merchantBreakdown.firstOrNull()?.amount ?: 1L
        val merchantBreakdownWithMax = merchantBreakdown.map { it.copy(relativeToMaxPercent = it.amount.toDouble() / maxMerchant * 100) }

        return CategoryDetailsData(
            categoryName = currentData.categoryNames[categoryId] ?: "Uncategorized",
            totalAmount = currentAmount,
            comparison = calculateComparison(currentAmount, previousAmount, state),
            merchantBreakdown = merchantBreakdownWithMax,
            fragments = fragments.sortedByDescending { frag -> currentData.expensesById[frag.expenseId]?.createdAt ?: 0L }
        )
    }

    suspend fun getTagDetails(tagId: String, state: StatisticsDateRangeState): TagDetailsData {
        val currentRange = StatisticsDateUtils.getComparisonRange(state)
        val currentData = getPeriodData(currentRange.currentStart, currentRange.currentEnd)
        val previousData = if (currentRange.previousStart != null && currentRange.previousEnd != null) {
            getPeriodData(currentRange.previousStart, currentRange.previousEnd)
        } else null

        val fragments = StatisticsAggregation.buildTagFragments(currentData.splitNodes, currentData.nodeTags)
            .filter { it.tagId == tagId }
        val prevFragments = previousData?.let { 
            StatisticsAggregation.buildTagFragments(it.splitNodes, it.nodeTags).filter { it.tagId == tagId }
        }

        val currentAmount = fragments.sumOf { it.amount }
        val previousAmount = prevFragments?.sumOf { it.amount }

        return TagDetailsData(
            tagName = currentData.tagNames[tagId] ?: "Tag",
            totalAmount = currentAmount,
            comparison = calculateComparison(currentAmount, previousAmount, state),
            fragments = run {
                val nodesById = currentData.splitNodes.associateBy { it.id }
                fragments.map { frag ->
                    val expense = currentData.expensesById[frag.expenseId]
                    TagResultItem(
                        expenseId = frag.expenseId,
                        label = nodesById[frag.splitNodeId]?.label ?: "Item",
                        amount = frag.amount,
                        date = expense?.createdAt ?: 0L,
                        merchantName = currentData.merchantNames[expense?.merchantId]
                    )
                }.sortedByDescending { it.date }
            }
        )
    }

    suspend fun search(filter: StatisticsSearchFilter): SearchResultsData {
        val currentRange = StatisticsDateUtils.getComparisonRange(filter.dateRange)
        val currentData = getPeriodData(currentRange.currentStart, currentRange.currentEnd)

        val filteredFragments = matchingFragments(currentData, filter)
        val totalAmount = filteredFragments.sumOf { it.amount }

        val previousData = if (currentRange.previousStart != null && currentRange.previousEnd != null) {
            getPeriodData(currentRange.previousStart, currentRange.previousEnd)
        } else null
        val prevAmount = previousData?.let { pData ->
            matchingFragments(pData, filter).sumOf { it.amount }
        }

        val results = filteredFragments.map { frag ->
            val expense = currentData.expensesById.getValue(frag.expenseId)
            SearchResultItem(
                expenseId = frag.expenseId,
                title = frag.label,
                amount = frag.amount,
                date = expense.createdAt,
                merchantName = currentData.merchantNames[expense.merchantId],
                categoryName = currentData.categoryNames[frag.effectiveCategoryId],
                tags = currentData.nodeTags[frag.splitNodeId].orEmpty().map { it.name }
            )
        }.sortedByDescending { it.date }

        return SearchResultsData(
            totalAmount = totalAmount,
            comparison = calculateComparison(totalAmount, prevAmount, filter.dateRange),
            results = results
        )
    }

    /** Applies [filter] to the category fragments of a single period. Shared by the
     *  current-period result set and the previous-period comparison total. */
    private fun matchingFragments(
        data: PeriodData,
        filter: StatisticsSearchFilter
    ): List<CategoryAmountFragment> {
        return data.expenses
            .flatMap { StatisticsAggregation.buildCategoryFragments(it, data.splitNodes) }
            .filter { frag ->
                val expense = data.expensesById[frag.expenseId] ?: return@filter false

                if (filter.merchantIds.isNotEmpty() &&
                    !filter.merchantIds.contains(expense.merchantId ?: "unknown")
                ) return@filter false

                if (filter.categoryIds.isNotEmpty() &&
                    !filter.categoryIds.contains(frag.effectiveCategoryId)
                ) return@filter false

                if (filter.query.isNotBlank()) {
                    val matchesLabel = frag.label.contains(filter.query, ignoreCase = true)
                    val matchesNote = expense.note?.contains(filter.query, ignoreCase = true) == true
                    if (!matchesLabel && !matchesNote) return@filter false
                }

                if (filter.tagIds.isNotEmpty()) {
                    val nodeTags = data.nodeTags[frag.splitNodeId].orEmpty().map { it.id }.toSet()
                    val matches = if (filter.tagMatchMode == TagMatchMode.ALL) {
                        nodeTags.containsAll(filter.tagIds)
                    } else {
                        filter.tagIds.any { nodeTags.contains(it) }
                    }
                    if (!matches) return@filter false
                }

                true
            }
    }

    suspend fun getTransactionDetails(expenseId: String): TransactionDetailsData? {
        val expense = db.expenses().getById(expenseId) ?: return null
        val nodes = db.splitNodes().getByExpenseId(expenseId)
        val nodeTags = nodes.associate { it.id to db.splitNodeTags().getTagsForSplitNode(it.id) }
        
        val categoryName = db.categories().all().find { it.id == expense.categoryId }?.name
        val merchantName = expense.merchantId?.let { mId -> db.merchants().all().find { it.id == mId }?.name }
        val sourceName = db.sources().all().find { it.id == expense.sourceId }?.name

        return TransactionDetailsData(
            expense = expense,
            categoryName = categoryName,
            merchantName = merchantName,
            sourceName = sourceName,
            splitNodes = nodes,
            nodeTags = nodeTags
        )
    }

    private suspend fun getPeriodData(start: LocalDate, end: LocalDate): PeriodData {
        val from = StatisticsDateUtils.localDateToMillis(start)
        val to = StatisticsDateUtils.localDateToMillis(end, endOfDay = true)
        
        val expenses = db.expenses().expensesInRange(from, to)
        val expenseIds = expenses.map { it.id }
        
        val allSplitNodes = if (expenseIds.isNotEmpty()) {
            db.splitNodes().getByExpenseIds(expenseIds)
        } else emptyList()

        val splitNodeIds = allSplitNodes.map { it.id }
        val nodeTags = if (splitNodeIds.isNotEmpty()) {
            db.splitNodeTags().getTagsForSplitNodes(splitNodeIds)
                .groupBy { it.splitNodeId }
                .mapValues { entry -> entry.value.map { it.tag } }
        } else emptyMap()

        val categoryNames = db.categories().all().associate { it.id to it.name }
        val merchantNames = db.merchants().all().associate { it.id to it.name }
        val tagNames = db.tags().all().associate { it.id to it.name }

        return PeriodData(
            expenses = expenses,
            expensesById = expenses.associateBy { it.id },
            splitNodes = allSplitNodes,
            nodeTags = nodeTags,
            categoryNames = categoryNames,
            merchantNames = merchantNames,
            tagNames = tagNames
        )
    }

    private fun calculateTotalSpent(
        currentExpenses: List<dev.keslorod.quickexpense.data.entities.Expense>,
        previousExpenses: List<dev.keslorod.quickexpense.data.entities.Expense>?,
        state: StatisticsDateRangeState
    ): StatsAmountSummary {
        val currentAmount = currentExpenses.sumOf { it.amount }
        val previousAmount = previousExpenses?.sumOf { it.amount }
        
        return StatsAmountSummary(
            currentAmount = currentAmount,
            previousAmount = previousAmount,
            currentCount = currentExpenses.size,
            previousCount = previousExpenses?.size,
            comparison = calculateComparison(currentAmount, previousAmount, state)
        )
    }

    private fun calculateComparison(
        currentAmount: Long,
        previousAmount: Long?,
        state: StatisticsDateRangeState
    ): PeriodComparisonSummary {
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

        return PeriodComparisonSummary(
            currentAmount = currentAmount,
            previousAmount = previousAmount,
            absoluteDelta = absoluteDelta,
            relativeDeltaPercent = relativeDeltaPercent,
            direction = direction,
            label = ""
        )
    }

    private fun calculateCategoryBreakdown(data: PeriodData): List<StatsBreakdownItem> {
        val fragments = data.expenses.flatMap { StatisticsAggregation.buildCategoryFragments(it, data.splitNodes) }
        return aggregateFragmentsByCategory(fragments, data.categoryNames)
    }

    private fun aggregateFragmentsByCategory(
        fragments: List<CategoryAmountFragment>,
        categoryNames: Map<String, String>
    ): List<StatsBreakdownItem> {
        val totals = fragments.groupBy { it.effectiveCategoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        val totalAmount = totals.values.sum()
        if (totalAmount == 0L) return emptyList()

        val items = totals.map { (categoryId, amount) ->
            StatsBreakdownItem(
                id = categoryId,
                label = categoryNames[categoryId] ?: "Uncategorized",
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

    /** Dense per-day totals for the dashboard trend line. Returns empty for periods longer
     *  than ~2 months — a 200+ point daily line stops being legible and needs a coarser
     *  (weekly/monthly) bucket that isn't built yet, so the UI hides the trend card instead
     *  of rendering an unreadable one. */
    private fun calculateDailyTrend(
        data: PeriodData,
        start: LocalDate,
        end: LocalDate
    ): List<DailyTrendPoint> {
        val dayCount = ChronoUnit.DAYS.between(start, end) + 1
        if (dayCount !in 1..62) return emptyList()

        val totalsByDay = data.expenses
            .groupBy { StatisticsDateUtils.millisToLocalDate(it.createdAt) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        return (0 until dayCount).map { offset ->
            val day = start.plusDays(offset)
            DailyTrendPoint(date = day, amount = totalsByDay[day] ?: 0L)
        }
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
        val expensesById: Map<String, dev.keslorod.quickexpense.data.entities.Expense>,
        val splitNodes: List<dev.keslorod.quickexpense.data.entities.SplitNode>,
        val nodeTags: Map<String, List<dev.keslorod.quickexpense.data.entities.Tag>>,
        val categoryNames: Map<String, String>,
        val merchantNames: Map<String, String>,
        val tagNames: Map<String, String>
    )
}
