package dev.keslorod.quickexpense.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.repository.StatisticsRepository
import dev.keslorod.quickexpense.domain.statistics.DashboardStatistics
import dev.keslorod.quickexpense.domain.statistics.StatisticsDateRangeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(val data: DashboardStatistics) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class StatisticsDashboardViewModel(
    application: Application,
    private val filterViewModel: StatisticsFilterViewModel
) : AndroidViewModel(application) {
    
    private val app = application as App
    private val repository = StatisticsRepository(app.db)

    private val _currency = MutableStateFlow("RSD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _currency.value = app.prefs.currencyFlow.first()
        }

        // Automatically reload when filter changes
        filterViewModel.state
            .onEach { loadStatistics(it) }
            .launchIn(viewModelScope)
    }

    private fun loadStatistics(filterState: StatisticsDateRangeState) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = DashboardUiState.Loading
            try {
                val stats = repository.getDashboardStatistics(filterState)
                _uiState.value = DashboardUiState.Success(stats)
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
