package dev.keslorod.quickexpense.ui.statistics.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.formatCents
import dev.keslorod.quickexpense.domain.statistics.StatsBreakdownItem
import dev.keslorod.quickexpense.ui.theme.ChartColors
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.Pie

private const val OTHER_ID = "__other__"
private const val MAX_SLICES = 5

/**
 * Folds items beyond [MAX_SLICES] into one synthetic "Other" bucket. A pie whose slices don't
 * sum to the period total (silently dropping the tail) is misleading, so unlike the plain
 * [StatsBreakdownCard] list this never just truncates.
 */
private fun foldForPie(items: List<StatsBreakdownItem>, otherLabel: String): List<StatsBreakdownItem> {
    if (items.size <= MAX_SLICES) return items
    val head = items.take(MAX_SLICES)
    val tail = items.drop(MAX_SLICES)
    val maxAmount = head.firstOrNull()?.amount ?: 1L
    val otherAmount = tail.sumOf { it.amount }
    return head + StatsBreakdownItem(
        id = OTHER_ID,
        label = otherLabel,
        amount = otherAmount,
        count = tail.sumOf { it.count },
        shareOfTotalPercent = tail.sumOf { it.shareOfTotalPercent },
        relativeToMaxPercent = if (maxAmount > 0) otherAmount.toDouble() / maxAmount * 100 else 0.0
    )
}

/**
 * Dashboard card for categories/merchants: a donut chart above the existing ranked bar list.
 * Not used for tags — tag totals overlap and must not be shown as part-to-whole (see
 * Statistics_Dashboard_Flow.md §2.2–2.3); tags keep the plain [StatsBreakdownCard].
 */
@Composable
fun StatsPieBreakdownCard(
    title: String,
    items: List<StatsBreakdownItem>,
    currency: String,
    onViewAll: () -> Unit,
    onItemClick: (StatsBreakdownItem) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onViewAll) {
                    Text(stringResource(R.string.view_all))
                }
            }

            if (items.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_data_period),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val otherLabel = stringResource(R.string.chart_other)
            val folded = remember(items, otherLabel) { foldForPie(items, otherLabel) }
            val hasOther = folded.lastOrNull()?.id == OTHER_ID
            val realSliceCount = if (hasOther) folded.size - 1 else folded.size
            val palette = ChartColors.forCount(realSliceCount)
            val otherColor = ChartColors.other()

            // The Pie model has no id field, so slices are correlated back to StatsBreakdownItem
            // by list position (pieData[i] always corresponds to folded[i]).
            val pieData = folded.mapIndexed { index, item ->
                Pie(
                    label = item.label,
                    data = item.amount.toDouble(),
                    color = if (item.id == OTHER_ID) otherColor else palette[index]
                )
            }

            Spacer(Modifier.height(12.dp))

            PieChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                data = pieData,
                style = Pie.Style.Stroke(width = 28.dp),
                spaceDegree = 2f,
                labelHelperProperties = LabelHelperProperties(enabled = false),
                onPieClick = { pie ->
                    val index = pieData.indexOfFirst { it === pie }
                    folded.getOrNull(index)?.let { item ->
                        if (item.id != OTHER_ID) onItemClick(item)
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                folded.forEach { item ->
                    StatsRelativeBarRow(
                        item = item,
                        currency = currency,
                        onClick = { if (item.id != OTHER_ID) onItemClick(item) },
                        formatAmount = { formatCents(it) }
                    )
                }
            }
        }
    }
}
