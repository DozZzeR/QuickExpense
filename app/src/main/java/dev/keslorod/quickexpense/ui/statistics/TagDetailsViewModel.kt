package dev.keslorod.quickexpense.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.repository.StatisticsRepository
import dev.keslorod.quickexpense.domain.statistics.TagDetailsData
import dev.keslorod.quickexpense.domain.statistics.StatisticsDateRangeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface TagDetailsUiState {
    object Loading : TagDetailsUiState
    data class Success(val data: TagDetailsData) : TagDetailsUiState
    data class Error(val message: String) : TagDetailsUiState
}

class TagDetailsViewModel(
    application: Application,
    private val tagId: String,
    private val filterViewModel: StatisticsFilterViewModel
) : AndroidViewModel(application) {

    private val app = application as App
    private val repository = StatisticsRepository.forApp(app)

    private val _uiState = MutableStateFlow<TagDetailsUiState>(TagDetailsUiState.Loading)
    val uiState: StateFlow<TagDetailsUiState> = _uiState.asStateFlow()

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
            _uiState.value = TagDetailsUiState.Loading
            try {
                val data = repository.getTagDetails(tagId, filterState)
                _uiState.value = TagDetailsUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = TagDetailsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

class TagDetailsViewModelFactory(
    private val application: android.app.Application,
    private val tagId: String,
    private val filterViewModel: StatisticsFilterViewModel
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return TagDetailsViewModel(application, tagId, filterViewModel) as T
    }
}
