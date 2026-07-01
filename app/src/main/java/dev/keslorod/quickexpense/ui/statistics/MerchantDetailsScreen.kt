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
fun MerchantDetailsScreen(
    merchantId: String,
    filterViewModel: StatisticsFilterViewModel,
    onBack: () -> Unit,
    onTransactionClick: (String) -> Unit
) {
    val viewModel: MerchantDetailsViewModel = viewModel(factory = MerchantDetailsViewModelFactory(
        application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
        merchantId = merchantId,
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
                            is MerchantDetailsUiState.Success -> s.data.merchantName
                            else -> stringResource(R.string.merchant)
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
                is MerchantDetailsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MerchantDetailsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is MerchantDetailsUiState.Success -> {
                    val data = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            AmountSummaryCard(summary = data.totalSummary, currency = currency)
                        }

                        if (data.categoryBreakdown.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.categories_in_merchant),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(data.categoryBreakdown) { item ->
                                PaddingBox {
                                    StatsRelativeBarRow(
                                        item = item,
                                        currency = currency,
                                        onClick = { /* Could navigate to category details filtered by merchant */ },
                                        formatAmount = { formatCents(it) }
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = stringResource(R.string.transactions),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(data.transactions) { tx ->
                            TransactionRow(tx, currency) { onTransactionClick(tx.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaddingBox(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        content()
    }
}

@Composable
private fun TransactionRow(
    tx: dev.keslorod.quickexpense.data.entities.Expense,
    currency: String,
    onClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    ListItem(
        headlineContent = { Text(sdf.format(Date(tx.createdAt))) },
        trailingContent = { 
            Text(
                "${formatCents(tx.amount)} $currency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            tx.note?.let { Text(it) }
        },
        modifier = Modifier.clickable { onClick() }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
}
