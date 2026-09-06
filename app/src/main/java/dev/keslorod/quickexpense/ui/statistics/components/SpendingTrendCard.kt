package dev.keslorod.quickexpense.ui.statistics.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.formatCents
import dev.keslorod.quickexpense.domain.statistics.DailyTrendPoint
import dev.keslorod.quickexpense.ui.theme.ChartColors
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.PopupProperties
import java.time.format.DateTimeFormatter

/**
 * Daily spending line for the currently selected period. Hidden by the caller when
 * [dev.keslorod.quickexpense.domain.statistics.DashboardStatistics.dailyTrend] is empty (too
 * short/too long a period — see StatisticsRepository.calculateDailyTrend).
 */
@Composable
fun SpendingTrendCard(
    points: List<DailyTrendPoint>,
    currency: String,
) {
    if (points.size < 2) return // nothing to trend with 0-1 points

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.spending_trend),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            val lineColor = ChartColors.series().first()
            val dayFormatter = remember { DateTimeFormatter.ofPattern("d") }
            // The library lays `labels` out with equal spacing across the chart width,
            // independent of how many data points there are — so thinning to ~6 ticks means
            // passing a short, gap-free list, not one padded with "" placeholders to match
            // points.size. (A mix of real and empty strings breaks its width math: it sizes
            // every label to the *narrowest* one, which is 0 for "", collapsing all labels.)
            val labels = remember(points) {
                val tickCount = points.size.coerceAtMost(6)
                if (tickCount <= 1) {
                    listOf(points.last().date.format(dayFormatter))
                } else {
                    (0 until tickCount).map { tick ->
                        val index = tick * (points.size - 1) / (tickCount - 1)
                        points[index].date.format(dayFormatter)
                    }
                }
            }

            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                data = remember(points, lineColor) {
                    listOf(
                        Line(
                            label = null,
                            values = points.map { it.amount / 100.0 },
                            color = SolidColor(lineColor),
                            dotProperties = DotProperties(
                                enabled = true,
                                radius = 4.dp,
                                color = SolidColor(lineColor)
                            ),
                            popupProperties = PopupProperties(
                                contentBuilder = { popup -> "${formatCents((popup.value * 100).toLong())} $currency" }
                            )
                        )
                    )
                },
                labelProperties = LabelProperties(
                    enabled = true,
                    labels = labels,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                ),
                labelHelperProperties = LabelHelperProperties(enabled = false),
                indicatorProperties = HorizontalIndicatorProperties(enabled = false)
            )
        }
    }
}
