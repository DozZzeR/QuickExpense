package dev.keslorod.quickexpense.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.repository.StatisticsRepository
import dev.keslorod.quickexpense.domain.statistics.CategoryDetailsData
import dev.keslorod.quickexpense.domain.statistics.StatisticsDateRangeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface CategoryDetailsUiState {
    object Loading : CategoryDetailsUiState
    data class Success(val data: CategoryDetailsData) : CategoryDetailsUiState
    data class Error(val message: String) : CategoryDetailsUiState
}

class CategoryDetailsViewModel(
    application: Application,
    private val categoryId: String,
    private val filterViewModel: StatisticsFilterViewModel
) : AndroidViewModel(application) {

    private val app = application as App
    private val repository = StatisticsRepository.forApp(app)

    private val _uiState = MutableStateFlow<CategoryDetailsUiState>(CategoryDetailsUiState.Loading)
    val uiState: StateFlow<CategoryDetailsUiState> = _uiState.asStateFlow()

    private val _currency = MutableStateFlow("RSD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    init {
        viewModelScope.launch {
            _currency.value = app.prefs.currencyFlow.first()
        }

        filterViewModel.state
            .onEach { loadDetails(it) }
            .launchIn(viewModelScope)
    }

    private fun loadDetails(filterState: StatisticsDateRangeState) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = CategoryDetailsUiState.Loading
            try {
                val data = repository.getCategoryDetails(categoryId, filterState)
                _uiState.value = CategoryDetailsUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = CategoryDetailsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

class CategoryDetailsViewModelFactory(
    private val application: android.app.Application,
    private val categoryId: String,
    private val filterViewModel: StatisticsFilterViewModel
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return CategoryDetailsViewModel(application, categoryId, filterViewModel) as T
    }
}
