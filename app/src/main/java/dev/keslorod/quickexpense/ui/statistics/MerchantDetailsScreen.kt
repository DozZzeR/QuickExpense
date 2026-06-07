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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.formatCents
import dev.keslorod.quickexpense.ui.statistics.components.DateRangeFilterBar
import dev.keslorod.quickexpense.ui.statistics.components.StatsBreakdownCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDetailsScreen(
    merchantId: String,
    filterViewModel: StatisticsFilterViewModel,
    onBack: () -> Unit,
    onTransactionClick: (String) -> Unit
) {
    // Placeholder implementation
    val filterState by filterViewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали магазина") },
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

            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Детали магазина $merchantId (в разработке)")
            }
        }
    }
}
