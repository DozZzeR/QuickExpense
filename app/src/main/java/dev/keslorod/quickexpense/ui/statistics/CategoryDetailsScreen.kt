package dev.keslorod.quickexpense.ui.statistics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.formatCents
import dev.keslorod.quickexpense.ui.statistics.components.AmountSummaryCard
import dev.keslorod.quickexpense.ui.statistics.components.DateRangeFilterBar
import dev.keslorod.quickexpense.ui.statistics.components.StatsRelativeBarRow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    categoryId: String,
    filterViewModel: StatisticsFilterViewModel,
    onBack: () -> Unit,
    onTransactionClick: (String) -> Unit
) {
    val viewModel: CategoryDetailsViewModel = viewModel(factory = CategoryDetailsViewModelFactory(
        application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
        categoryId = categoryId,
        filterViewModel = filterViewModel
    ))

    val uiState by viewModel.uiState.collectAsState()
    val filterState by filterViewModel.state.collectAsState()
    val currency by viewModel.currency.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (val s = uiState) {
                            is CategoryDetailsUiState.Success -> s.data.categoryName
                            else -> stringResource(R.string.category)
                        }
                    )
                },
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

            when (val state = uiState) {
                is CategoryDetailsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is CategoryDetailsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is CategoryDetailsUiState.Success -> {
                    val data = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            // Reuse simple summary block or just Text if needed
                            ElevatedCard(Modifier.padding(16.dp).fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.total_spent), style = MaterialTheme.typography.labelLarge)
                                    Text("${formatCents(data.totalAmount)} $currency", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    // Comparison here too if model supports it
                                }
                            }
                        }

                        if (data.merchantBreakdown.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.places_of_spending),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(data.merchantBreakdown) { item ->
                                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                    StatsRelativeBarRow(
                                        item = item,
                                        currency = currency,
                                        onClick = { /* Navigate to merchant detail filtered by category if possible */ },
                                        formatAmount = { formatCents(it) }
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = stringResource(R.string.txs_and_positions),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(data.fragments) { frag ->
                            FragmentRow(frag, currency) { onTransactionClick(frag.expenseId) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FragmentRow(
    frag: dev.keslorod.quickexpense.domain.statistics.CategoryAmountFragment,
    currency: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(frag.label) },
        trailingContent = { 
            Text(
                "${formatCents(frag.amount)} $currency",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
}
