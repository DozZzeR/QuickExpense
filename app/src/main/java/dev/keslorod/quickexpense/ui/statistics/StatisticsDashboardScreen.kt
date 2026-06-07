package dev.keslorod.quickexpense.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.ui.statistics.components.AmountSummaryCard
import dev.keslorod.quickexpense.ui.statistics.components.DateRangeFilterBar
import dev.keslorod.quickexpense.ui.statistics.components.StatsBreakdownCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsDashboardScreen(
    filterViewModel: StatisticsFilterViewModel,
    onBack: () -> Unit,
    onViewMerchants: () -> Unit,
    onViewCategories: () -> Unit,
    onViewTags: () -> Unit,
    onAdvancedSearch: () -> Unit,
    onMerchantClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    val dashboardViewModel: StatisticsDashboardViewModel = viewModel(factory = StatisticsDashboardViewModelFactory(
        application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
        filterViewModel = filterViewModel
    ))
    
    val uiState by dashboardViewModel.uiState.collectAsState()
    val filterState by filterViewModel.state.collectAsState()
    val currency by dashboardViewModel.currency.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onAdvancedSearch) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.advanced_search))
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            DateRangeFilterBar(
                state = filterState,
                onPresetSelected = { filterViewModel.setPreset(it) }
            )

            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DashboardUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is DashboardUiState.Success -> {
                    DashboardContent(
                        data = state.data,
                        currency = currency,
                        onViewMerchants = onViewMerchants,
                        onViewCategories = onViewCategories,
                        onViewTags = onViewTags,
                        onMerchantClick = onMerchantClick,
                        onCategoryClick = onCategoryClick,
                        onTagClick = onTagClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: dev.keslorod.quickexpense.domain.statistics.DashboardStatistics,
    currency: String,
    onViewMerchants: () -> Unit,
    onViewCategories: () -> Unit,
    onViewTags: () -> Unit,
    onMerchantClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AmountSummaryCard(summary = data.totalSpent, currency = currency)

        StatsBreakdownCard(
            title = stringResource(R.string.categories),
            items = data.categoryPie,
            currency = currency,
            onViewAll = onViewCategories,
            onItemClick = { onCategoryClick(it.id) }
        )

        StatsBreakdownCard(
            title = stringResource(R.string.places_of_spending),
            items = data.merchantPie,
            currency = currency,
            onViewAll = onViewMerchants,
            onItemClick = { onMerchantClick(it.id) }
        )

        StatsBreakdownCard(
            title = stringResource(R.string.tags),
            items = data.topTags,
            currency = currency,
            onViewAll = onViewTags,
            onItemClick = { onTagClick(it.id) },
            showPercent = false,
            note = stringResource(R.string.tag_totals_overlap_note)
        )
        
        Spacer(Modifier.height(32.dp))
    }
}

class StatisticsDashboardViewModelFactory(
    private val application: android.app.Application,
    private val filterViewModel: StatisticsFilterViewModel
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return StatisticsDashboardViewModel(application, filterViewModel) as T
    }
}
