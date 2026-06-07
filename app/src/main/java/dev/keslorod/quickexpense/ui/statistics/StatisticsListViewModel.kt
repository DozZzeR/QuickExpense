package dev.keslorod.quickexpense.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.repository.StatisticsRepository
import dev.keslorod.quickexpense.domain.statistics.StatsBreakdownItem
import dev.keslorod.quickexpense.domain.statistics.StatisticsDateRangeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class StatsListType { MERCHANTS, CATEGORIES, TAGS }

class StatisticsListViewModel(
    application: Application,
    private val filterViewModel: StatisticsFilterViewModel,
    private val type: StatsListType
) : AndroidViewModel(application) {

    private val app = application as App
    private val repository = StatisticsRepository(app.db)

    private val _items = MutableStateFlow<List<StatsBreakdownItem>>(emptyList())
    val items: StateFlow<List<StatsBreakdownItem>> = _items.asStateFlow()

    private val _currency = MutableStateFlow("RSD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _currency.value = app.prefs.currencyFlow.first()
        }

        filterViewModel.state
            .onEach { loadData(it) }
            .launchIn(viewModelScope)
    }

    private fun loadData(filterState: StatisticsDateRangeState) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // This is a bit inefficient as we fetch everything just for one list, 
                // but okay for MVP with repo-level Kotlin aggregation.
                val stats = repository.getDashboardStatistics(filterState)
                _items.value = when (type) {
                    StatsListType.MERCHANTS -> stats.merchantPie
                    StatsListType.CATEGORIES -> stats.categoryPie
                    StatsListType.TAGS -> stats.topTags
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class StatisticsListViewModelFactory(
    private val application: android.app.Application,
    private val filterViewModel: StatisticsFilterViewModel,
    private val type: StatsListType
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return StatisticsListViewModel(application, filterViewModel, type) as T
    }
}
