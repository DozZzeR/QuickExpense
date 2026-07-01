package dev.keslorod.quickexpense.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.repository.StatisticsRepository
import dev.keslorod.quickexpense.data.entities.Category
import dev.keslorod.quickexpense.data.entities.Merchant
import dev.keslorod.quickexpense.data.entities.Tag
import dev.keslorod.quickexpense.domain.statistics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val data: SearchResultsData) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class AdvancedSearchViewModel(
    application: Application,
    private val filterViewModel: StatisticsFilterViewModel
) : AndroidViewModel(application) {

    private val app = application as App
    private val repository = StatisticsRepository(app.db)

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchFilter = MutableStateFlow(StatisticsSearchFilter(dateRange = filterViewModel.state.value))
    val searchFilter: StateFlow<StatisticsSearchFilter> = _searchFilter.asStateFlow()

    private val _currency = MutableStateFlow("RSD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _allCategories = MutableStateFlow<List<Category>>(emptyList())
    val allCategories: StateFlow<List<Category>> = _allCategories.asStateFlow()

    private val _allMerchants = MutableStateFlow<List<Merchant>>(emptyList())
    val allMerchants: StateFlow<List<Merchant>> = _allMerchants.asStateFlow()

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _currency.value = app.prefs.currencyFlow.first()
            _allCategories.value = app.db.categories().all()
            _allMerchants.value = app.db.merchants().all()
            _allTags.value = app.db.tags().all()
        }

        // Keep date range in search filter synced with the global filter
        filterViewModel.state
            .onEach { dateRange ->
                _searchFilter.update { it.copy(dateRange = dateRange) }
                if (_uiState.value is SearchUiState.Success) {
                    performSearch() // Refresh search if already performed
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateQuery(query: String) {
        _searchFilter.update { it.copy(query = query) }
    }

    fun toggleMerchant(id: String) {
        _searchFilter.update { filter ->
            val newIds = if (filter.merchantIds.contains(id)) filter.merchantIds - id else filter.merchantIds + id
            filter.copy(merchantIds = newIds)
        }
    }

    fun toggleCategory(id: String) {
        _searchFilter.update { filter ->
            val newIds = if (filter.categoryIds.contains(id)) filter.categoryIds - id else filter.categoryIds + id
            filter.copy(categoryIds = newIds)
        }
    }

    fun toggleTag(id: String) {
        _searchFilter.update { filter ->
            val newIds = if (filter.tagIds.contains(id)) filter.tagIds - id else filter.tagIds + id
            filter.copy(tagIds = newIds)
        }
    }

    fun setTagMatchMode(mode: TagMatchMode) {
        _searchFilter.update { it.copy(tagMatchMode = mode) }
    }

    fun performSearch() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = SearchUiState.Loading
            try {
                val results = repository.search(_searchFilter.value)
                _uiState.value = SearchUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

class AdvancedSearchViewModelFactory(
    private val application: android.app.Application,
    private val filterViewModel: StatisticsFilterViewModel
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AdvancedSearchViewModel(application, filterViewModel) as T
    }
}
