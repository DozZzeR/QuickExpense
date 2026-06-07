package dev.keslorod.quickexpense.ui.statistics.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.formatCents
import dev.keslorod.quickexpense.domain.statistics.ComparisonDirection
import dev.keslorod.quickexpense.domain.statistics.StatsAmountSummary

@Composable
fun AmountSummaryCard(
    summary: StatsAmountSummary,
    currency: String
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.total_spent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${formatCents(summary.currentAmount)} $currency",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            )
            
            summary.comparison.let { comp ->
                if (comp.direction != ComparisonDirection.DISABLED) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val color = when (comp.direction) {
                            ComparisonDirection.UP -> MaterialTheme.colorScheme.error
                            ComparisonDirection.DOWN -> MaterialTheme.colorScheme.primary // Green-ish in default material? Actually primary is usually blue.
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        
                        val prefix = when (comp.direction) {
                            ComparisonDirection.UP -> "+"
                            ComparisonDirection.DOWN -> "-"
                            else -> ""
                        }

                        val deltaText = if (comp.absoluteDelta != null) {
                            "$prefix${formatCents(Math.abs(comp.absoluteDelta))} $currency"
                        } else ""

                        val percentText = if (comp.relativeDeltaPercent != null) {
                            "${String.format("%.1f", Math.abs(comp.relativeDeltaPercent))}%"
                        } else stringResource(R.string.new_spending)

                        Text(
                            text = "$deltaText • $percentText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.vs_previous_period),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = summary.currentCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.transactions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                val avg = if (summary.currentCount != null && summary.currentCount > 0) {
                    summary.currentAmount / summary.currentCount
                } else 0L

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${formatCents(avg)} $currency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.avg_transaction),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
