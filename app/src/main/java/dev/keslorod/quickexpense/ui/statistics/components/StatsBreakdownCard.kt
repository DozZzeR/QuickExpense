package dev.keslorod.quickexpense.ui.statistics.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.formatCents
import dev.keslorod.quickexpense.domain.statistics.StatsBreakdownItem

@Composable
fun StatsBreakdownCard(
    title: String,
    items: List<StatsBreakdownItem>,
    currency: String,
    onViewAll: () -> Unit,
    onItemClick: (StatsBreakdownItem) -> Unit,
    showPercent: Boolean = true,
    note: String? = null
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
            
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.take(5).forEach { item ->
                    StatsRelativeBarRow(
                        item = item,
                        currency = currency,
                        showPercent = showPercent,
                        onClick = { onItemClick(item) },
                        formatAmount = { formatCents(it) }
                    )
                }
            }
        }
    }
}
