package dev.keslorod.quickexpense.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.repository.StatisticsRepository
import dev.keslorod.quickexpense.domain.statistics.MerchantDetailsData
import dev.keslorod.quickexpense.domain.statistics.StatisticsDateRangeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface MerchantDetailsUiState {
    object Loading : MerchantDetailsUiState
    data class Success(val data: MerchantDetailsData) : MerchantDetailsUiState
    data class Error(val message: String) : MerchantDetailsUiState
}

class MerchantDetailsViewModel(
    application: Application,
    private val merchantId: String,
    private val filterViewModel: StatisticsFilterViewModel
) : AndroidViewModel(application) {

    private val app = application as App
    private val repository = StatisticsRepository(app.db)

    private val _uiState = MutableStateFlow<MerchantDetailsUiState>(MerchantDetailsUiState.Loading)
    val uiState: StateFlow<MerchantDetailsUiState> = _uiState.asStateFlow()

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
            _uiState.value = MerchantDetailsUiState.Loading
            try {
                val data = repository.getMerchantDetails(merchantId, filterState)
                _uiState.value = MerchantDetailsUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = MerchantDetailsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

class MerchantDetailsViewModelFactory(
    private val application: android.app.Application,
    private val merchantId: String,
    private val filterViewModel: StatisticsFilterViewModel
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MerchantDetailsViewModel(application, merchantId, filterViewModel) as T
    }
}
