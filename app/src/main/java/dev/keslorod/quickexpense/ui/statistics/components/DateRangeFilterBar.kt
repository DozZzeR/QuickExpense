package dev.keslorod.quickexpense.ui.statistics.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.statistics.StatisticsDatePreset
import dev.keslorod.quickexpense.domain.statistics.StatisticsDateRangeState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DateRangeFilterBar(
    state: StatisticsDateRangeState,
    onPresetSelected: (StatisticsDatePreset) -> Unit,
    // onCustomRangeSelected will be needed later
) {
    var expanded by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(state.preset.toLabel())
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                StatisticsDatePreset.values().forEach { preset ->
                    if (preset != StatisticsDatePreset.CUSTOM) { // Custom handled differently later
                        DropdownMenuItem(
                            text = { Text(preset.toLabel()) },
                            onClick = {
                                onPresetSelected(preset)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        if (state.preset != StatisticsDatePreset.ALL_TIME) {
            Text(
                text = "${state.startDate.format(formatter)} — ${state.endDate.format(formatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.all_time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatisticsDatePreset.toLabel(): String = when (this) {
    StatisticsDatePreset.TODAY -> stringResource(R.string.today)
    StatisticsDatePreset.YESTERDAY -> stringResource(R.string.yesterday)
    StatisticsDatePreset.THIS_WEEK -> stringResource(R.string.this_week)
    StatisticsDatePreset.THIS_MONTH -> stringResource(R.string.this_month)
    StatisticsDatePreset.THIS_YEAR -> stringResource(R.string.this_year)
    StatisticsDatePreset.LAST_7_DAYS -> stringResource(R.string.last_7_days)
    StatisticsDatePreset.LAST_30_DAYS -> stringResource(R.string.last_30_days)
    StatisticsDatePreset.CUSTOM -> stringResource(R.string.custom_range)
    StatisticsDatePreset.ALL_TIME -> stringResource(R.string.all_time)
}
