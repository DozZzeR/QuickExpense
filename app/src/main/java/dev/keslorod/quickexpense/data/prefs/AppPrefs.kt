package dev.keslorod.quickexpense.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private const val STORE_NAME = "quickexpense_prefs"
val Context.dataStore by preferencesDataStore(name = STORE_NAME)

object PrefKeys {
    val currency = stringPreferencesKey("currency")           // "RSD" / "EUR" ...
    val widgetPeriod = stringPreferencesKey("widget_period")  // "none/day/week/month/all"
    val monthAnchorDay = intPreferencesKey("month_anchor")    // 1..31
    val widgetLimitCents = longPreferencesKey("widget_limit_cents")  // лимит трат в центах, или 0 если не установлен
    val showRemainderInsteadOfExpense = booleanPreferencesKey("show_remainder")  // показывать остаток вместо расхода
    val seeded = booleanPreferencesKey("seeded")
}

class AppPrefs(private val context: Context) {
    val data = context.dataStore.data

    val currencyFlow = data.map { it[PrefKeys.currency] ?: "RSD" }
    val widgetPeriodFlow = data.map { it[PrefKeys.widgetPeriod] ?: "day" }
    val monthAnchorDayFlow = data.map { it[PrefKeys.monthAnchorDay] ?: 1 }
    val widgetLimitCentsFlow = data.map { it[PrefKeys.widgetLimitCents] ?: 0L }
    val showRemainderInsteadOfExpenseFlow = data.map { it[PrefKeys.showRemainderInsteadOfExpense] ?: false }
    val seededFlow = data.map { it[PrefKeys.seeded] ?: false }

    suspend fun setCurrency(code: String) = context.dataStore.edit { it[PrefKeys.currency] = code }
    suspend fun setWidgetPeriod(mode: String) = context.dataStore.edit { it[PrefKeys.widgetPeriod] = mode }
    suspend fun setMonthAnchorDay(day: Int) = context.dataStore.edit { it[PrefKeys.monthAnchorDay] = day.coerceIn(1,31) }
    suspend fun setWidgetLimitCents(cents: Long) = context.dataStore.edit { it[PrefKeys.widgetLimitCents] = cents.coerceAtLeast(0) }
    suspend fun setShowRemainderInsteadOfExpense(v: Boolean) = context.dataStore.edit { it[PrefKeys.showRemainderInsteadOfExpense] = v }
    suspend fun setSeeded(v: Boolean) = context.dataStore.edit { it[PrefKeys.seeded] = v }
}
