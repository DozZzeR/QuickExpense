package dev.keslorod.quickexpense.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.domain.Period
import dev.keslorod.quickexpense.domain.Range
import dev.keslorod.quickexpense.domain.periodRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExpenseItemUi(
    val id: String,
    val title: String,      // "Еда" и т.п.
    val subtitle: String,   // "Источник: Карта • 2025-01-01 12:34"
    val amount: Long
)

data class MainUiState(
    val currency: String = "RSD",
    val subtitle: String = "",
    val totalCents: Long = 0L,
    val last: List<Expense> = emptyList(),
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val appRef get() = getApplication<App>()

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val currency = appRef.prefs.currencyFlow.first()
            val periodStr = appRef.prefs.widgetPeriodFlow.first()
            val anchor = appRef.prefs.monthAnchorDayFlow.first()

            val period = when (periodStr) {
                "day" -> Period.DAY
                "week" -> Period.WEEK
                "month" -> Period.MONTH
                "all" -> Period.ALL
                else  -> Period.DAY
            }
            val subtitlePeriod = when (periodStr) {
                "day" -> "за день"
                "week" -> "за неделю"
                "month" -> "за месяц"
                "all" -> "за всё время"
                else -> "за день"
            }

            val range = periodRange(period, anchor)
            val total = appRef.db.expenses().sumInRange(range.from, range.to)

            val list = appRef.db.expenses()
                .expensesInRangeWithNames(range.from, range.to, limit = 200)
                .map { e ->
                    ExpenseItemUi(
                        id = e.id,
                        title = e.categoryName ?: "Без категории",
                        subtitle = buildString {
                            append("Источник: ")
                            append(e.sourceName ?: "—")
                            append(" • ")
                            append(formatTs(e.createdAt))
                        },
                        amount = e.amount
                    )
                }

            _state.update {
                it.copy(
                    currency = currency,
                    subtitle = subtitlePeriod,
                    totalCents = total
                )
            }
            _items.value = list
        }
    }

    private val _items = MutableStateFlow<List<ExpenseItemUi>>(emptyList())
    val items: StateFlow<List<ExpenseItemUi>> = _items.asStateFlow()

    private fun formatTs(ts: Long): String {
        val fmt = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        return fmt.format(java.util.Date(ts))
    }

    suspend fun currentRange(): Range {
        val periodStr = appRef.prefs.widgetPeriodFlow.first()
        val anchor = appRef.prefs.monthAnchorDayFlow.first()

        val period = when (periodStr) {
            "day" -> Period.DAY
            "week" -> Period.WEEK
            "month" -> Period.MONTH
            "all" -> Period.ALL
            else  -> Period.DAY
        }
        return periodRange(period, anchor)
    }
}