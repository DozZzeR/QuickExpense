package dev.keslorod.quickexpense.ui.statistics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.data.entities.Category
import dev.keslorod.quickexpense.data.entities.Merchant
import dev.keslorod.quickexpense.data.entities.Tag
import dev.keslorod.quickexpense.domain.formatCents
import dev.keslorod.quickexpense.domain.statistics.TagMatchMode
import dev.keslorod.quickexpense.ui.statistics.components.DateRangeFilterBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedSearchScreen(
    filterViewModel: StatisticsFilterViewModel,
    onBack: () -> Unit,
    onTransactionClick: (String) -> Unit
) {
    val viewModel: AdvancedSearchViewModel = viewModel(factory = AdvancedSearchViewModelFactory(
        application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
        filterViewModel = filterViewModel
    ))

    val uiState by viewModel.uiState.collectAsState()
    val filterState by filterViewModel.state.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val currency by viewModel.currency.collectAsState()
    
    val allCategories by viewModel.allCategories.collectAsState()
    val allMerchants by viewModel.allMerchants.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    var showFilters by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.advanced_search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize()) {
            DateRangeFilterBar(
                state = filterState,
                onPresetSelected = { filterViewModel.setPreset(it) }
            )

            if (showFilters) {
                SearchFiltersBlock(
                    filter = searchFilter,
                    allCategories = allCategories,
                    allMerchants = allMerchants,
                    allTags = allTags,
                    onQueryChange = { viewModel.updateQuery(it) },
                    onToggleMerchant = { viewModel.toggleMerchant(it) },
                    onToggleCategory = { viewModel.toggleCategory(it) },
                    onToggleTag = { viewModel.toggleTag(it) },
                    onSetTagMatchMode = { viewModel.setTagMatchMode(it) },
                    onSearchClick = { 
                        viewModel.performSearch()
                        showFilters = false
                    }
                )
            } else {
                TextButton(
                    onClick = { showFilters = true },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.FilterAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.change_filters))
                }
            }

            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    if (!showFilters) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.set_filters_and_search))
                        }
                    }
                }
                is SearchUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SearchUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is SearchUiState.Success -> {
                    SearchResultsList(
                        data = state.data,
                        currency = currency,
                        onTransactionClick = onTransactionClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFiltersBlock(
    filter: dev.keslorod.quickexpense.domain.statistics.StatisticsSearchFilter,
    allCategories: List<Category>,
    allMerchants: List<Merchant>,
    allTags: List<Tag>,
    onQueryChange: (String) -> Unit,
    onToggleMerchant: (String) -> Unit,
    onToggleCategory: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onSetTagMatchMode: (TagMatchMode) -> Unit,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = filter.query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.search_by_name_or_note)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (filter.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true
        )

        // Categories
        FilterSection(title = stringResource(R.string.categories)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allCategories.forEach { category ->
                    FilterChip(
                        selected = filter.categoryIds.contains(category.id),
                        onClick = { onToggleCategory(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }
        }

        // Merchants
        FilterSection(title = stringResource(R.string.merchants)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allMerchants.forEach { merchant ->
                    FilterChip(
                        selected = filter.merchantIds.contains(merchant.id),
                        onClick = { onToggleMerchant(merchant.id) },
                        label = { Text(merchant.name) }
                    )
                }
            }
        }

        // Tags
        FilterSection(title = stringResource(R.string.tags)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tag_match_mode_label), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = filter.tagMatchMode == TagMatchMode.ALL,
                    onClick = { onSetTagMatchMode(TagMatchMode.ALL) },
                    label = { Text(stringResource(R.string.tag_match_all)) }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = filter.tagMatchMode == TagMatchMode.ANY,
                    onClick = { onSetTagMatchMode(TagMatchMode.ANY) },
                    label = { Text(stringResource(R.string.tag_match_any)) }
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allTags.forEach { tag ->
                    FilterChip(
                        selected = filter.tagIds.contains(tag.id),
                        onClick = { onToggleTag(tag.id) },
                        label = { Text("#${tag.name}") }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        
        Button(
            onClick = onSearchClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.find_btn))
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun SearchResultsList(
    data: dev.keslorod.quickexpense.domain.statistics.SearchResultsData,
    currency: String,
    onTransactionClick: (String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("${stringResource(R.string.found)}: ${data.results.size}", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${formatCents(data.totalAmount)} $currency",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (data.results.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.nothing_found))
                }
            }
        }

        items(data.results) { item ->
            ListItem(
                headlineContent = { Text(item.title) },
                supportingContent = {
                    Column {
                        Text("${item.merchantName ?: "—"} • ${sdf.format(Date(item.date))}")
                        if (item.tags.isNotEmpty()) {
                            Text(
                                item.tags.joinToString(" ") { "#$it" },
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                trailingContent = {
                    Text(
                        "${formatCents(item.amount)} $currency",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.clickable { onTransactionClick(item.expenseId) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        }
    }
}
