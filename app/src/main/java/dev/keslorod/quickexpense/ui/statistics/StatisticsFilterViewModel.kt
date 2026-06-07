package dev.keslorod.quickexpense.ui.statistics

import androidx.lifecycle.ViewModel
import dev.keslorod.quickexpense.domain.statistics.StatisticsDatePreset
import dev.keslorod.quickexpense.domain.statistics.StatisticsDateRangeState
import dev.keslorod.quickexpense.domain.statistics.StatisticsDateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class StatisticsFilterViewModel : ViewModel() {
    private val _state = MutableStateFlow(createDefaultState())
    val state: StateFlow<StatisticsDateRangeState> = _state.asStateFlow()

    private fun createDefaultState(): StatisticsDateRangeState {
        val (start, end) = StatisticsDateUtils.getRangeForPreset(StatisticsDatePreset.THIS_MONTH)
        return StatisticsDateRangeState(
            preset = StatisticsDatePreset.THIS_MONTH,
            startDate = start,
            endDate = end,
            comparisonEnabled = true
        )
    }

    fun setPreset(preset: StatisticsDatePreset) {
        if (preset == StatisticsDatePreset.CUSTOM) return // Use setCustomRange instead
        
        val (start, end) = StatisticsDateUtils.getRangeForPreset(preset)
        _state.update { it.copy(preset = preset, startDate = start, endDate = end) }
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _state.update { it.copy(preset = StatisticsDatePreset.CUSTOM, startDate = start, endDate = end) }
    }

    fun setComparisonEnabled(enabled: Boolean) {
        _state.update { it.copy(comparisonEnabled = enabled) }
    }
}
