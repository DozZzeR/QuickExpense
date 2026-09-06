package dev.keslorod.quickexpense.ui.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.domain.statistics.StatsBreakdownItem

@Composable
fun StatsRelativeBarRow(
    item: StatsBreakdownItem,
    currency: String,
    showPercent: Boolean = true,
    onClick: () -> Unit,
    formatAmount: (Long) -> String,
    // Ties this row's background back to its chart series (e.g. the matching donut slice) so a
    // reader can tell which legend row belongs to which mark without cross-checking labels.
    // Defaults to the generic theme tint for callers with no per-item chart color (e.g. tags,
    // which aren't charted as a pie — see Statistics_Dashboard_Flow.md §2.3).
    seriesColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
    ) {
        // Bar background
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = (item.relativeToMaxPercent / 100.0).toFloat().coerceAtLeast(0.03f))
                .matchParentSize()
                .background(seriesColor.copy(alpha = 0.28f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (showPercent && item.shareOfTotalPercent > 0) {
                    Text(
                        text = "${String.format("%.1f", item.shareOfTotalPercent)}% • ${item.count} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "${item.count} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${formatAmount(item.amount)} $currency",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
