package dev.keslorod.quickexpense.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.domain.Period
import dev.keslorod.quickexpense.domain.getWidgetSubtitle
import dev.keslorod.quickexpense.domain.periodRange
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class WidgetRecomputeAndRedraw(
    private val scope: CoroutineScope,
    context: Context
) {
    private val app = context.applicationContext as App
    private var job: Job? = null

    /** коалесированный пересчёт + запись state + redraw */
    fun schedule(delayMs: Long = 1200) {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            delay(delayMs)

            val periodStr = app.prefs.widgetPeriodFlow.first()
            val currency = app.prefs.currencyFlow.first()
            val limitCents = app.prefs.widgetLimitCentsFlow.first()
            val showRemainder = app.prefs.showRemainderInsteadOfExpenseFlow.first()
            
            val period = when (periodStr) {
                "day" -> Period.DAY
                "week" -> Period.WEEK
                "month" -> Period.MONTH
                "all" -> Period.ALL
                else  -> Period.DAY
            }
            val range = periodRange(period, 1)
            val total = app.db.expenses().sumInRange(range.from, range.to)
            
            // Вычисляем значение для отображения: либо расход, либо остаток
            val displayValue = if (showRemainder && limitCents > 0) {
                limitCents - total
            } else {
                total
            }
            
            val subtitle = period.getWidgetSubtitle(app)

            writeWidgetStateForAll(
                ctx = app,
                sumCents = displayValue,
                currency = currency,
                subtitle = subtitle,
                showRemainder = showRemainder
            )
            QuickExpenseWidget().updateAll(app)
        }
    }
}

