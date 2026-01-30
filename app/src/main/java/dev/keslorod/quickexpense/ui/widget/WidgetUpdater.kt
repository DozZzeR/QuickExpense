package dev.keslorod.quickexpense.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dev.keslorod.quickexpense.BuildConfig

/**
 * Записать готовые данные в state всех инстансов виджета.
 * Частый вызов допустим: это просто запись prefs, без перерисовки.
 */
suspend fun writeWidgetStateForAll(
    ctx: Context,
    sumCents: Long,
    currency: String,
    subtitle: String,
    showRemainder: Boolean = false
) {
    val mgr = GlanceAppWidgetManager(ctx)
    val ids = mgr.getGlanceIds(QuickExpenseWidget::class.java)
    if (ids.isEmpty()) return
    if (BuildConfig.DEBUG) Log.d("My debug writeWidgetStateForAll", ctx.toString())
    ids.forEach { id ->
        updateAppWidgetState(ctx, PreferencesGlanceStateDefinition, id) { prefs ->
            prefs.toMutablePreferences().apply {
                this[WidgetKeys.SUM] = sumCents
                this[WidgetKeys.CCY] = currency
                this[WidgetKeys.SUBTITLE] = subtitle
                this[WidgetKeys.SHOW_REMAINDER] = showRemainder
            }
        }
    }
}


