package dev.keslorod.quickexpense.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.statistics.StatsBreakdownItem
import dev.keslorod.quickexpense.ui.statistics.components.DateRangeFilterBar
import dev.keslorod.quickexpense.ui.statistics.components.StatsRelativeBarRow
import dev.keslorod.quickexpense.domain.formatCents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsListScreen(
    title: String,
    filterViewModel: StatisticsFilterViewModel,
    items: List<StatsBreakdownItem>,
    currency: String,
    showPercent: Boolean = true,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val filterState by filterViewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            DateRangeFilterBar(
                state = filterState,
                onPresetSelected = { filterViewModel.setPreset(it) }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    StatsRelativeBarRow(
                        item = item,
                        currency = currency,
                        showPercent = showPercent,
                        onClick = { onItemClick(item.id) },
                        formatAmount = { formatCents(it) }
                    )
                }
            }
        }
    }
}
