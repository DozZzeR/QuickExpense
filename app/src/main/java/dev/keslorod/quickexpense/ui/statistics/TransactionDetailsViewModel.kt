package dev.keslorod.quickexpense.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.repository.StatisticsRepository
import dev.keslorod.quickexpense.domain.statistics.TransactionDetailsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface TransactionDetailsUiState {
    object Loading : TransactionDetailsUiState
    data class Success(val data: TransactionDetailsData) : TransactionDetailsUiState
    data class Error(val message: String) : TransactionDetailsUiState
}

class TransactionDetailsViewModel(
    application: Application,
    private val expenseId: String
) : AndroidViewModel(application) {

    private val app = application as App
    private val repository = StatisticsRepository.forApp(app)

    private val _uiState = MutableStateFlow<TransactionDetailsUiState>(TransactionDetailsUiState.Loading)
    val uiState: StateFlow<TransactionDetailsUiState> = _uiState.asStateFlow()

    private val _currency = MutableStateFlow("RSD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    init {
        viewModelScope.launch {
            _currency.value = app.prefs.currencyFlow.first()
        }
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = TransactionDetailsUiState.Loading
            try {
                val data = repository.getTransactionDetails(expenseId)
                if (data != null) {
                    _uiState.value = TransactionDetailsUiState.Success(data)
                } else {
                    _uiState.value = TransactionDetailsUiState.Error("Transaction not found")
                }
            } catch (e: Exception) {
                _uiState.value = TransactionDetailsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

class TransactionDetailsViewModelFactory(
    private val application: android.app.Application,
    private val expenseId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return TransactionDetailsViewModel(application, expenseId) as T
    }
}
